package com.back.domain.post.service;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.res.*;
import com.back.domain.post.entity.*;
import com.back.domain.post.repository.PostFavoriteRepository;
import com.back.domain.post.repository.PostOptionRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.region.region.entity.Region;
import com.back.domain.region.region.repository.RegionRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostOptionRepository postOptionRepository;
    private final PostFavoriteRepository postFavoriteRepository;

    private final RegionRepository regionRepository;
    private final CategoryRepository categoryRepository;

    public void createPost(PostCreateReqBody reqBody, Long memberId) {

        Member author = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

        Category category = categoryRepository.findById(reqBody.categoryId())
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."));

        List<Region> regions = regionRepository.findAllById(reqBody.regionIds());
        if (regions.isEmpty()) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다.");
        }

        Post post = Post.builder()
                .title(reqBody.title())
                .content(reqBody.content())
                .receiveMethod(reqBody.receiveMethod())
                .returnMethod(reqBody.returnMethod())
                .returnAddress1(reqBody.returnAddress1())
                .returnAddress2(reqBody.returnAddress2())
                .deposit(reqBody.deposit())
                .fee(reqBody.fee())
                .author(author)
                .category(category)
                .isBanned(false)
                .build();

        if (reqBody.options() != null && !reqBody.options().isEmpty()) {
            List<PostOption> postOptions = reqBody.options().stream()
                    .map(option -> PostOption.builder()
                            .post(post)
                            .name(option.name())
                            .deposit(option.deposit())
                            .fee(option.fee())
                            .build())
                    .toList();
            post.getOptions().addAll(postOptions);
        }

        if (reqBody.images() != null && !reqBody.images().isEmpty()) {
            List<PostImage> postImages = reqBody.images().stream()
                    .map(image -> PostImage.builder()
                            .post(post)
                            .imageUrl("example.com/image.jpg") // TODO: 이미지 업로드 로직 구현 후 수정
                            .isPrimary(image.isPrimary())
                            .build())
                    .toList();
            post.getImages().addAll(postImages);
        }

        List<PostRegion> postRegions = regions.stream()
                .map(region -> PostRegion.builder()
                        .post(post)
                        .region(region)
                        .build())
                .toList();
        post.getPostRegions().addAll(postRegions);

        postRepository.save(post);
    }

    public PagePayload<PostListResBody> getPostList(
            Pageable pageable,
            String keyword,
            Long categoryId,
            List<Long> regionIds,
            Long memberId
    ) {
        boolean hasFilter =
                (keyword != null && !keyword.isBlank()) ||
                        categoryId != null ||
                        (regionIds != null && !regionIds.isEmpty());

        Page<Post> postPage;

        if (regionIds != null && regionIds.isEmpty()) {
            regionIds = null;
        }

        if (hasFilter) {
            postPage = postRepository.findFilteredPosts(keyword, categoryId, regionIds, pageable);
        } else {
            postPage = postRepository.findAll(pageable);
        }

        Page<PostListResBody> mappedPage = postPage.map(post -> {
            boolean isFavorite = false;

            if (memberId != null && !post.getAuthor().getId().equals(memberId)) {
                isFavorite = postFavoriteRepository
                        .findByMemberIdAndPostId(memberId, post.getId())
                        .isPresent();
            }

            return PostListResBody.builder()
                    .postId(post.getId())
                    .title(post.getTitle())
                    .thumbnailImageUrl(
                            post.getImages().stream()
                                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                    .findFirst()
                                    .map(PostImage::getImageUrl)
                                    .orElse(null)
                    )
                    .categoryId(post.getCategory().getId())
                    .regionIds(
                            post.getPostRegions().stream()
                                    .map(postRegion -> postRegion.getRegion().getId())
                                    .collect(Collectors.toList())
                    )
                    .receiveMethod(post.getReceiveMethod())
                    .returnMethod(post.getReturnMethod())
                    .createdAt(post.getCreatedAt())
                    .authorNickname(post.getAuthor().getNickname())
                    .fee(post.getFee())
                    .deposit(post.getDeposit())
                    .isFavorite(isFavorite)
                    .isBanned(post.getIsBanned())
                    .build();
        });

        return PageUt.of(mappedPage);
    }


    public PostDetailResBody getPostById(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ServiceException(HttpStatus.NOT_FOUND, "%d번 글은 존재하지 않는 게시글입니다.".formatted(postId))
                );

        boolean isFavorite = postFavoriteRepository.findByMemberIdAndPostId(memberId, postId).isPresent();

        return PostDetailResBody.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categoryId(post.getCategory().getId())
                .regionIds(post.getPostRegions().stream()
                        .map(postRegion -> postRegion.getRegion().getId())
                        .collect(Collectors.toList()))
                .receiveMethod(post.getReceiveMethod())
                .returnMethod(post.getReturnMethod())
                .returnAddress1(post.getReturnAddress1())
                .returnAddress2(post.getReturnAddress2())
                .deposit(post.getDeposit())
                .fee(post.getFee())
                .options(post.getOptions().stream()
                        .map(option -> PostOptionResBody.builder()
                                .name(option.getName())
                                .deposit(option.getDeposit())
                                .fee(option.getFee())
                                .build())
                        .collect(Collectors.toList()))
                .images(post.getImages().stream()
                        .map(image -> PostImageResBody.builder()
                                .file(image.getImageUrl())
                                .isPrimary(image.getIsPrimary())
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                .createdAt(post.getCreatedAt())
                .modifiedAt(post.getModifiedAt())
                .author(PostAuthorDto.from(post.getAuthor()))
                .isFavorite(isFavorite)
                .isBanned(post.getIsBanned())
                .build();
    }
    public PagePayload<PostListResBody> getMyPosts(Long memberId, Pageable pageable) {
        Page<PostListResBody> postPage = postRepository.findAllByAuthorId(memberId, pageable)
                .map(post -> PostListResBody.builder()
                        .postId(post.getId())
                        .title(post.getTitle())
                        .thumbnailImageUrl(
                                post.getImages().stream()
                                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                        .findFirst()
                                        .map(img -> img.getImageUrl())
                                        .orElse(null)
                        )
                        .categoryId(post.getCategory().getId())
                        .regionIds(post.getPostRegions().stream()
                                .map(postRegion -> postRegion.getRegion().getId())
                                .collect(Collectors.toList()))
                        .receiveMethod(post.getReceiveMethod())
                        .returnMethod(post.getReturnMethod())
                        .createdAt(post.getCreatedAt())
                        .authorNickname(post.getAuthor().getNickname())
                        .fee(post.getFee())
                        .deposit(post.getDeposit())
                        .isFavorite(false)
                        .isBanned(post.getIsBanned())
                        .build()
                );

        return PageUt.of(postPage);
    }

    public Post getById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
    }

    public List<PostOption> getAllOptionsById(List<Long> optionIds) {
        return postOptionRepository.findAllById(optionIds);
    }

    public boolean toggleFavorite(Long postId, long memberId) {
        Post post = getById(postId);
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

        if (post.getAuthor().getId().equals(member.getId())) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "본인의 게시글은 즐겨찾기 할 수 없습니다.");
        }

        return postFavoriteRepository.findByMemberIdAndPostId(memberId, postId)
                .map(postFavorite -> {
                    postFavoriteRepository.delete(postFavorite);
                    return false;
                })
                .orElseGet(() -> {
                    PostFavorite postFavorite = PostFavorite.builder()
                            .member(member)
                            .post(post)
                            .build();
                    postFavoriteRepository.save(postFavorite);
                    return true;
                });
    }

    public PagePayload<PostListResBody> getFavoritePosts(long memberId, Pageable pageable) {
        Page<PostFavorite> favoritePosts = postFavoriteRepository.findAllByMemberId(memberId, pageable);

        Page<PostListResBody> mappedPage = favoritePosts.map(favorite -> {
            Post post = favorite.getPost();
            return PostListResBody.builder()
                    .postId(post.getId())
                    .title(post.getTitle())
                    .thumbnailImageUrl(
                            post.getImages().stream()
                                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                    .findFirst()
                                    .map(img -> img.getImageUrl())
                                    .orElse(null)
                    )
                    .categoryId(post.getCategory().getId())
                    .regionIds(post.getPostRegions().stream()
                            .map(postRegion -> postRegion.getRegion().getId())
                            .collect(Collectors.toList()))
                    .receiveMethod(post.getReceiveMethod())
                    .returnMethod(post.getReturnMethod())
                    .createdAt(post.getCreatedAt())
                    .authorNickname(post.getAuthor().getNickname())
                    .fee(post.getFee())
                    .deposit(post.getDeposit())
                    .isFavorite(true)
                    .isBanned(post.getIsBanned())
                    .build();
        });
        return PageUt.of(mappedPage);
    }

    public void updatePost(Long postId, PostCreateReqBody reqBody, long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));


        if (post.getAuthor().getId().equals(memberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "본인의 게시글만 수정할 수 있습니다.");
        }
    }

}
