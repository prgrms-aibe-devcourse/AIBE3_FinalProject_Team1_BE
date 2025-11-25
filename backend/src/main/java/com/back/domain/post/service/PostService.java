package com.back.domain.post.service;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.dto.req.PostAiContentReqBody;
import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostOptionReqBody;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.PostCreateResBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.entity.*;
import com.back.domain.post.repository.*;
import com.back.domain.region.entity.Region;
import com.back.domain.region.repository.RegionRepository;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3Uploader;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostOptionRepository postOptionRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostFavoriteQueryRepository postFavoriteQueryRepository;
    private final S3Uploader s3;
    private final ChatClient chatClient;

    private final RegionRepository regionRepository;
    private final CategoryRepository categoryRepository;

    @Value("${custom.ai.post-content-prompt}")
    private String postContentPrompt;

    @Transactional
    public PostCreateResBody createPost(PostCreateReqBody reqBody, List<MultipartFile> files, Long memberId) {

        Member author = this.memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

        Category category = this.categoryRepository.findById(reqBody.categoryId()).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));

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
        if (regions.isEmpty()) throw new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다.");

        Post post = Post.of(reqBody.title(), reqBody.content(), reqBody.receiveMethod(), reqBody.returnMethod(), reqBody.returnAddress1(), reqBody.returnAddress2(), reqBody.deposit(), reqBody.fee(), author, category);

        if (reqBody.options() != null && !reqBody.options().isEmpty()) {
            List<PostOption> postOptions = reqBody.options().stream().map(option -> new PostOption(post, option.name(), option.deposit(), option.fee())).toList();

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

        return PostCreateResBody.of(post);
    }

    @Transactional(readOnly = true)
    public PagePayload<PostListResBody> getPostList(Pageable pageable, String keyword, Long categoryId, List<Long> regionIds, Long memberId) {
        boolean hasFilter = (keyword != null && !keyword.isBlank()) || categoryId != null || (regionIds != null && !regionIds.isEmpty());

        if (regionIds != null && regionIds.isEmpty()) regionIds = null;

        Page<Post> postPage = hasFilter ? this.postQueryRepository.findFilteredPosts(keyword, categoryId, regionIds, pageable) : this.postRepository.findAll(pageable);

        Page<PostListResBody> mappedPage = postPage.map(post -> {

            boolean isFavorite = memberId != null && !post.getAuthor().getId().equals(memberId) && this.postFavoriteRepository.findByMemberIdAndPostId(memberId, post.getId()).isPresent();

            String thumbnail = post.getImages().stream()
                    .filter(img -> img.getIsPrimary())
                    .findFirst()
                    .map(img -> s3.generatePresignedUrl(img.getImageUrl()))
                    .orElse(null);

            return PostListResBody.of(post, isFavorite, thumbnail);
        });

        return PageUt.of(mappedPage);
    }

    @Transactional(readOnly = true)
    public PostDetailResBody getPostById(Long postId, Long memberId) {
        Post post = this.postRepository.findById(postId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 글은 존재하지 않는 게시글입니다.".formatted(postId)));

        boolean isFavorite = false;

        if (memberId != null) {
            isFavorite = this.postFavoriteRepository
                    .findByMemberIdAndPostId(memberId, postId)
                    .isPresent();
        }

        List<LocalDateTime> reservedDates = postQueryRepository.findReservedDatesFromToday(postId);

        return PostDetailResBody.of(post, isFavorite, reservedDates);
    }

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
        return this.postRepository.findById(postId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
    }

    public List<PostOption> getAllOptionsById(List<Long> optionIds) {
        return this.postOptionRepository.findAllById(optionIds);
    }

    public boolean toggleFavorite(Long postId, long memberId) {
        Post post = this.getById(postId);
        Member member = this.memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

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
    }

    public List<LocalDateTime> getReservedDates(Long id) {
        return postQueryRepository.findReservedDatesFromToday(id);
    }
    public String generateContent(PostAiContentReqBody reqBody, List<MultipartFile> files) {
        String userPrompt = createUserPrompt(reqBody);

        return chatClient.prompt()
                         .system(postContentPrompt)
                         .user(user -> {
                             user.text(userPrompt);
                             for (MultipartFile file : files) {
                                 user.media(MimeTypeUtils.parseMimeType(file.getContentType()), file.getResource());
                             }
                         })
                         .call()
                         .content();
    }

    private String createUserPrompt(PostAiContentReqBody reqBody) {
        Category category = categoryRepository.findById(reqBody.categoryId())
                                              .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));

        String optionInfo = getOptionInfo(reqBody);

        return String.format("""
                        다음 정보를 바탕으로 장비 대여 게시글의 상세 내용을 작성해주세요:
                        
                        제목: %s
                        카테고리: %s
                        대여료: %,d원
                        보증금: %,d원
                        
                        옵션 정보:
                        %s
                        
                        첨부된 이미지들을 참고하여 장비의 특징과 상태를 설명해주세요.
                        """,
                reqBody.title(),
                category.getName(),
                reqBody.fee(),
                reqBody.deposit(),
                optionInfo
        );
    }

    private String getOptionInfo(PostAiContentReqBody reqBody) {
        StringBuilder optionInfo = new StringBuilder();

        if (reqBody.options() != null && !reqBody.options().isEmpty()) {
            optionInfo.append("아래와 같은 추가 옵션들이 있습니다.\n");

            for (PostOptionReqBody option : reqBody.options()) {
                optionInfo.append(String.format(
                        "- 옵션 이름: %s (추가 보증금: %,d원, 추가 대여료: %,d원)\n",
                        option.name(),
                        option.deposit(),
                        option.fee()
                ));
            }
        } else {
            optionInfo.append("추가 옵션은 없습니다.\n");
        }

        return optionInfo.toString();
    }
}
