package com.back.domain.post.controller;

import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.PostCreateResBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.service.PostService;
import com.back.domain.review.service.ReviewSummaryService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController implements PostApi {

    private final PostService postService;
    private final ReviewSummaryService reviewSummaryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RsData<PostCreateResBody>> createPost(
            @Valid @RequestPart("request") PostCreateReqBody reqBody,
            @RequestPart(value = "file", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal SecurityUser user
    ) {
        
        PostCreateResBody body = this.postService.createPost(reqBody, files, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RsData<>(HttpStatus.CREATED, "게시글이 생성되었습니다.", body));
    }

    @GetMapping
    public ResponseEntity<RsData<PagePayload<PostListResBody>>> getPostList(
            @AuthenticationPrincipal SecurityUser user,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> regionIds) {

        Long memberId = (user != null) ? user.getId() : null;
        PagePayload<PostListResBody> body = this.postService.getPostList(pageable, keyword, categoryId, regionIds, memberId);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "성공", body));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RsData<PostDetailResBody>> getPostById(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {
        PostDetailResBody body = this.postService.getPostById(id, user.getId());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "성공", body));
    }

    @GetMapping("/my")
    public ResponseEntity<RsData<PagePayload<PostListResBody>>> getMyPostList(
            @AuthenticationPrincipal SecurityUser user,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PagePayload<PostListResBody> body = this.postService.getMyPosts(user.getId(), pageable);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "성공", body));
    }

    @PostMapping("/favorites/{id}")
    public ResponseEntity<RsData<Boolean>> toggleFavorite(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal SecurityUser user) {

        boolean isFavorite = postService.toggleFavorite(postId, user.getId());

        String msg = isFavorite ? "즐겨찾기에 추가되었습니다." : "즐겨찾기가 해제되었습니다.";

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, msg, isFavorite));
    }

    @GetMapping("/favorites")
    public ResponseEntity<RsData<PagePayload<PostListResBody>>> getFavoritePosts(
            @AuthenticationPrincipal SecurityUser user,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PagePayload<PostListResBody> body = this.postService.getFavoritePosts(user.getId(), pageable);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "성공", body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RsData<Void>> updatePost(
            @PathVariable Long id,
            @Valid @RequestPart("request") PostUpdateReqBody reqBody,
            @RequestPart(value = "file", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal SecurityUser user) {

        postService.updatePost(id, reqBody, files, user.getId());

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "게시글이 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RsData<Void>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {
        this.postService.deletePost(id, user.getId());

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "게시글이 삭제되었습니다."));
    }

    @GetMapping("/{id}/reviews/summary")
    public ResponseEntity<RsData<String>> summarizeReviews(@PathVariable Long id) {
        String body = reviewSummaryService.summarizeReviews(id);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, HttpStatus.OK.name(), body));
    }
}
