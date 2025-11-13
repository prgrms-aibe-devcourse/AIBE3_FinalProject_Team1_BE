package com.back.domain.post.service;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.entity.*;
import com.back.domain.post.repository.PostFavoriteRepository;
import com.back.domain.post.repository.PostOptionRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.region.entity.Region;
import com.back.domain.region.repository.RegionRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

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

        Post post = Post.of(
                reqBody.title(),
                reqBody.content(),
                reqBody.receiveMethod(),
                reqBody.returnMethod(),
                reqBody.returnAddress1(),
                reqBody.returnAddress2(),
                reqBody.deposit(),
                reqBody.fee(),
                author,
                category
        );

        if (reqBody.options() != null && !reqBody.options().isEmpty()) {
            List<PostOption> postOptions = reqBody.options().stream()
                    .map(option -> new PostOption(
                            post,
                            option.name(),
                            option.deposit(),
                            option.fee()
                    ))
                    .toList();

            post.getOptions().addAll(postOptions);
        }

        if (reqBody.images() != null && !reqBody.images().isEmpty()) {
            List<PostImage> images = reqBody.images().stream()
                    .map(img -> new PostImage(
                            post,
                            "example.com/image.jpg", // TODO: 실제 업로드 로직으로 변경
                            img.isPrimary()
                    ))
                    .toList();

            post.resetPostImages(images);
        }

        List<PostRegion> postRegions = regions.stream()
                .map(region -> new PostRegion(
                        post,
                        region
                ))
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

        if (regionIds != null && regionIds.isEmpty()) {
            regionIds = null;
        }

        Page<Post> postPage = hasFilter
                ? postRepository.findFilteredPosts(keyword, categoryId, regionIds, pageable)
                : postRepository.findAll(pageable);

        Page<PostListResBody> mappedPage = postPage.map(post -> {

            boolean isFavorite =
                    memberId != null &&
                            !post.getAuthor().getId().equals(memberId) &&
                            postFavoriteRepository.findByMemberIdAndPostId(memberId, post.getId()).isPresent();

            return PostListResBody.of(post, isFavorite);
        });

        return PageUt.of(mappedPage);
    }


    public PostDetailResBody getPostById(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ServiceException(HttpStatus.NOT_FOUND, "%d번 글은 존재하지 않는 게시글입니다.".formatted(postId))
                );

        boolean isFavorite = postFavoriteRepository.findByMemberIdAndPostId(memberId, postId).isPresent();

        return PostDetailResBody.of(post, isFavorite);
    }
    public PagePayload<PostListResBody> getMyPosts(Long memberId, Pageable pageable) {
        Page<PostListResBody> result = postRepository.findAllByAuthorId(memberId, pageable)
                .map(p -> PostListResBody.of(p, false));

        return PageUt.of(result);
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
                .map(fav -> {
                    postFavoriteRepository.delete(fav);
                    return false;
                })
                .orElseGet(() -> {
                    postFavoriteRepository.save(new PostFavorite(post, member));
                    return true;
                });
    }

    public PagePayload<PostListResBody> getFavoritePosts(long memberId, Pageable pageable) {

        Page<PostFavorite> favorites =
                postFavoriteRepository.findAllByMemberId(memberId, pageable);

        Page<PostListResBody> result = favorites
                .map(f -> PostListResBody.of(f.getPost(), true));

        return PageUt.of(result);

    }

    public void updatePost(Long postId, PostUpdateReqBody reqBody, long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));

        if (post.getAuthor().getId().equals(memberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "본인의 게시글만 수정할 수 있습니다.");
        }

        Category category = categoryRepository.findById(reqBody.categoryId())
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
                .map(option -> new PostOption(
                        post,
                        option.name(),
                        option.deposit(),
                        option.fee()
                ))
                .toList();
        post.resetPostOptions(newOptions);

        List<PostImage> newImages = reqBody.images().stream()
                .map(img -> new PostImage(
                        post,
                        "example.com/image.jpg", // TODO: 실제 업로드 로직으로 변경
                        img.isPrimary()
                ))
                .toList();
        post.resetPostImages(newImages);

        List<PostRegion> newPostRegions = regionRepository.findAllById(reqBody.regionIds()).stream()
                .map(region -> new PostRegion(
                        post,
                        region
                ))
                .toList();
        post.resetPostRegions(newPostRegions);
    }

}
