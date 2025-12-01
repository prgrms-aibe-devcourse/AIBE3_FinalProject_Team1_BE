package com.back.domain.post.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostEmbeddingDto;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.PostBannedResBody;
import com.back.domain.post.dto.res.PostCreateResBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostImageResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.entity.Post;
import com.back.domain.post.entity.PostFavorite;
import com.back.domain.post.entity.PostImage;
import com.back.domain.post.entity.PostOption;
import com.back.domain.post.entity.PostRegion;
import com.back.domain.post.repository.PostFavoriteQueryRepository;
import com.back.domain.post.repository.PostFavoriteRepository;
import com.back.domain.post.repository.PostOptionRepository;
import com.back.domain.post.repository.PostQueryRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.region.entity.Region;
import com.back.domain.region.repository.RegionRepository;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3Uploader;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final MemberRepository memberRepository;
	private final PostOptionRepository postOptionRepository;
	private final PostFavoriteRepository postFavoriteRepository;
	private final PostQueryRepository postQueryRepository;
	private final PostFavoriteQueryRepository postFavoriteQueryRepository;
	private final PostVectorService postVectorService;
	private final PostTransactionService postTransactionService;
	private final S3Uploader s3;

	private final RegionRepository regionRepository;
	private final CategoryRepository categoryRepository;

	@Transactional
	public PostCreateResBody createPost(PostCreateReqBody reqBody, List<MultipartFile> files, Long memberId) {

		Member author = this.memberRepository.findById(memberId)
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

		Category category = this.categoryRepository.findById(reqBody.categoryId())
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));

		if (files == null || files.isEmpty()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지는 최소 1개 이상 등록해야 합니다.");
		}

		if (reqBody.images() == null ||
			reqBody.images().isEmpty() ||
			reqBody.images().size() != files.size()) {

			throw new ServiceException(HttpStatus.BAD_REQUEST,
				"이미지 정보(images)와 업로드한 파일 개수가 일치해야 합니다.");
		}

		List<Region> regions = this.regionRepository.findAllById(reqBody.regionIds());
		if (regions.isEmpty())
			throw new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다.");

		Post post = Post.of(reqBody.title(), reqBody.content(), reqBody.receiveMethod(), reqBody.returnMethod(),
			reqBody.returnAddress1(), reqBody.returnAddress2(), reqBody.deposit(), reqBody.fee(), author, category);

		if (reqBody.options() != null && !reqBody.options().isEmpty()) {
			List<PostOption> postOptions = reqBody.options()
				.stream()
				.map(option -> new PostOption(post, option.name(), option.deposit(), option.fee()))
				.toList();

			post.getOptions().addAll(postOptions);
		}

		if (files != null && !files.isEmpty()) {

			List<PostImage> postImages = new ArrayList<>();

			for (int i = 0; i < files.size(); i++) {
				MultipartFile file = files.get(i);

				String url = s3.upload(file);

				boolean isPrimary = false;
				if (reqBody.images() != null && reqBody.images().size() > i) {
					isPrimary = reqBody.images().get(i).isPrimary();
				}

				postImages.add(new PostImage(post, url, isPrimary));
			}

			post.resetPostImages(postImages);
		}

		List<PostRegion> postRegions = regions.stream().map(region -> new PostRegion(post, region)).toList();

		post.getPostRegions().addAll(postRegions);

		this.postRepository.save(post);

		// 임베딩 작업 스케줄 처리
		// postVectorService.indexPost(post);

		return PostCreateResBody.of(post);
	}

	@Transactional(readOnly = true)
	public PagePayload<PostListResBody> getPostList(Pageable pageable, String keyword, List<Long> categoryIds,
		List<Long> regionIds, Long memberId) {
		boolean hasFilter = (keyword != null && !keyword.isBlank()) || categoryIds != null || (regionIds != null
			&& !regionIds.isEmpty());

		if (regionIds != null && regionIds.isEmpty())
			regionIds = null;

		Page<Post> postPage =
			hasFilter ? this.postQueryRepository.findFilteredPosts(keyword, categoryIds, regionIds, pageable) :
				this.postRepository.findByIsBannedFalse(pageable);

		Page<PostListResBody> mappedPage = postPage.map(post -> {

			boolean isFavorite = memberId != null && !post.getAuthor().getId().equals(memberId)
				&& this.postFavoriteRepository.findByMemberIdAndPostId(memberId, post.getId()).isPresent();

			String thumbnail = post.getImages().stream()
				.filter(PostImage::getIsPrimary)
				.findFirst()
				.map(img -> s3.generatePresignedUrl(img.getImageUrl()))
				.orElse(null);

			return PostListResBody.of(post, isFavorite, thumbnail);
		});

		return PageUt.of(mappedPage);
	}

	@Transactional(readOnly = true)
	public PostDetailResBody getPostById(Long postId, Long memberId) {
		Post post = this.postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 글은 존재하지 않는 게시글입니다.".formatted(postId)));

		boolean isFavorite = false;

		if (memberId != null) {
			isFavorite = this.postFavoriteRepository
				.findByMemberIdAndPostId(memberId, postId)
				.isPresent();
		}

		List<PostImageResBody> images = post.getImages().stream()
			.map(img -> PostImageResBody.of(img, s3.generatePresignedUrl(img.getImageUrl())))
			.toList();

		return PostDetailResBody.of(post, isFavorite, images);
	}

	@Transactional(readOnly = true)
	public PagePayload<PostListResBody> getMyPosts(Long memberId, Pageable pageable) {
		Page<PostListResBody> result = this.postQueryRepository.findMyPost(memberId, pageable)
			.map(post -> {

				String thumbnail = post.getImages().stream()
					.filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
					.findFirst()
					.map(img -> s3.generatePresignedUrl(img.getImageUrl()))
					.orElse(null);

				return PostListResBody.of(post, false, thumbnail);
			});

		return PageUt.of(result);
	}

	@Transactional(readOnly = true)
	public Post getById(long postId) {
		return this.postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
	}

	public List<PostOption> getAllOptionsById(List<Long> optionIds) {
		return this.postOptionRepository.findAllById(optionIds);
	}

	public boolean toggleFavorite(Long postId, long memberId) {
		Post post = this.getById(postId);
		Member member = this.memberRepository.findById(memberId)
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

		if (post.getAuthor().getId().equals(member.getId()))
			throw new ServiceException(HttpStatus.FORBIDDEN, "본인의 게시글은 즐겨찾기 할 수 없습니다.");

		return this.postFavoriteRepository.findByMemberIdAndPostId(memberId, postId).map(fav -> {
			this.postFavoriteRepository.delete(fav);
			return false;
		}).orElseGet(() -> {
			this.postFavoriteRepository.save(new PostFavorite(post, member));
			return true;
		});
	}

	@Transactional(readOnly = true)
	public PagePayload<PostListResBody> getFavoritePosts(long memberId, Pageable pageable) {

		Page<PostFavorite> favorites = this.postFavoriteQueryRepository.findFavoritePosts(memberId, pageable);

		Page<PostListResBody> result = favorites.map(fav -> {

			Post post = fav.getPost();

			String thumbnail = post.getImages().stream()
				.filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
				.findFirst()
				.map(img -> s3.generatePresignedUrl(img.getImageUrl()))
				.orElse(null);

			return PostListResBody.of(post, true, thumbnail);
		});

		return PageUt.of(result);

	}

	@Transactional
	public void updatePost(Long postId, PostUpdateReqBody reqBody, List<MultipartFile> files, long memberId) {

		Post post = this.postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));

		if (!post.getAuthor().getId().equals(memberId)) {
			throw new ServiceException(HttpStatus.FORBIDDEN, "본인의 게시글만 수정할 수 있습니다.");
		}

		if (files == null || files.isEmpty()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지는 최소 1개 이상 등록해야 합니다.");
		}

		if (reqBody.images() == null ||
			reqBody.images().isEmpty() ||
			reqBody.images().size() != files.size()) {

			throw new ServiceException(HttpStatus.BAD_REQUEST,
				"이미지 정보(images)와 업로드한 파일 개수가 일치해야 합니다.");
		}

		Category category = this.categoryRepository.findById(reqBody.categoryId())
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));

		post.updatePost(
			reqBody.title(),
			reqBody.content(),
			reqBody.receiveMethod(),
			reqBody.returnMethod(),
			reqBody.returnAddress1(),
			reqBody.returnAddress2(),
			reqBody.deposit(),
			reqBody.fee()
		);

		post.updateCategory(category);

		List<PostOption> newOptions = reqBody.options().stream()
			.map(option -> new PostOption(post, option.name(), option.deposit(), option.fee()))
			.toList();

		post.resetPostOptions(newOptions);

		List<String> oldImageUrls = post.getImages().stream()
			.map(PostImage::getImageUrl)
			.filter(Objects::nonNull)
			.toList();

		for (String url : oldImageUrls) {
			s3.delete(url);
		}

		List<PostImage> newImages = new ArrayList<>();

		for (int i = 0; i < files.size(); i++) {
			MultipartFile file = files.get(i);

			String uploadedUrl = s3.upload(file);

			boolean isPrimary = reqBody.images().get(i).isPrimary();

			newImages.add(new PostImage(post, uploadedUrl, isPrimary));
		}

		post.resetPostImages(newImages);

		List<PostRegion> newPostRegions = this.regionRepository.findAllById(reqBody.regionIds())
			.stream()
			.map(region -> new PostRegion(post, region))
			.toList();

		post.resetPostRegions(newPostRegions);

		postVectorService.indexPost(post);
	}

	@Transactional
	public void deletePost(Long postId, long memberId) {

		Post post = this.postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));

		if (!post.getAuthor().getId().equals(memberId)) {
			throw new ServiceException(HttpStatus.FORBIDDEN, "본인의 게시글만 삭제할 수 있습니다.");
		}

		List<String> imageUrls = post.getImages().stream()
			.map(PostImage::getImageUrl)
			.filter(Objects::nonNull)
			.toList();

		for (String imageUrl : imageUrls) {
			s3.delete(imageUrl);
		}

		this.postRepository.delete(post);

		postVectorService.deletePost(postId);
	}

	public List<LocalDateTime> getReservedDates(Long id) {
		return postQueryRepository.findReservedDatesFromToday(id);
	}

	@Transactional
	public PostBannedResBody banPost(Long postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(
				() -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 글은 존재하지 않는 게시글입니다.".formatted(postId))
			);
		if (post.getIsBanned()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "%d번 글은 이미 차단되었습니다.".formatted(postId));
		}
		post.ban();
		return PostBannedResBody.of(post);
	}

	@Transactional
	public PostBannedResBody unbanPost(Long postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(
				() -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 글은 존재하지 않는 게시글입니다.".formatted(postId))
			);
		if (!post.getIsBanned()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "%d번 글은 제재되지 않았습니다.".formatted(postId));
		}
		post.unban();
		return PostBannedResBody.of(post);
	}

	public void embedPostsBatch() {
		List<Post> postsToEmbed = postQueryRepository.findPostsToEmbedWithDetails(100); // 한 번에 최대 100개 게시글 처리

		if (postsToEmbed.isEmpty()) {
			log.info("임베딩할 WAIT 상태의 게시글이 없습니다.");
			return;
		}

		// 1️⃣ DTO 변환 (버전 정보 포함)
		List<PostEmbeddingDto> postDtos = postsToEmbed.stream()
			.map(PostEmbeddingDto::from)
			.toList();

		List<Long> postIds = postDtos.stream()
			.map(PostEmbeddingDto::id)
			.toList();

		// 2️⃣ 벌크로 선점 시도 (버전 증가)
		long updatedCount = postTransactionService.updateStatusToPending(postIds);

		if (updatedCount == 0) {
			log.warn("선점 시도했으나 업데이트된 게시글이 0건입니다.");
			return;
		}

		log.info("총 {}개의 게시글을 PENDING 상태로 선점 시도했습니다.", updatedCount);

		// 3️⃣ 실제로 선점된 게시글만 필터링 (낙관적 락 검증)
		List<PostEmbeddingDto> acquiredPosts = postTransactionService
				.verifyAcquiredPosts(postDtos);

		log.info("실제 선점 성공: {}건 (다른 워커 선점: {}건)",
				acquiredPosts.size(),
				postDtos.size() - acquiredPosts.size());

		// 4️⃣ 임베딩 처리
		int successCount = 0;
		int failedCount = 0;

		for (PostEmbeddingDto dto : acquiredPosts) {
			try {
				log.info(">>> 임베딩 시작: Post ID {}", dto.id());
				postVectorService.indexPost(dto);
				postTransactionService.updateStatusToDone(dto.id());
				log.info(">>> 임베딩 성공: Post ID {}", dto.id());
				successCount++;
			} catch (Exception e) {
				log.error(">>> 임베딩 실패: Post ID {}", dto.id(), e);
				postTransactionService.updateStatusToWait(dto.id());
				failedCount++;
			}
		}

		log.info("Embedding batch finished. 성공: {}, 실패: {}", successCount, failedCount);
	}
}
