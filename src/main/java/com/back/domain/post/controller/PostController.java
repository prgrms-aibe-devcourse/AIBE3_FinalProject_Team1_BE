package com.back.domain.post.controller;

import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.service.PostService;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<String> createPost(
            @Valid @RequestBody PostCreateReqBody reqBody,
            @AuthenticationPrincipal SecurityUser user
    ) {
        postService.createPost(reqBody, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body("게시글이 생성되었습니다");
    }

    @GetMapping
    public ResponseEntity<PagePayload<PostListResBody>> getPostList(
            @AuthenticationPrincipal SecurityUser user,
            @ParameterObject
            @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> regionIds
    ) {
        Long memberId = (user != null) ? user.getId() : null;
        PagePayload<PostListResBody> body = postService.getPostList(pageable, keyword, categoryId, regionIds, memberId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResBody> getPostById(
            @PathVariable Long postId,
            @AuthenticationPrincipal SecurityUser user) {
        PostDetailResBody body = postService.getPostById(postId, user.getId());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/my")
    public ResponseEntity<PagePayload<PostListResBody>> getMyPostList(
            @AuthenticationPrincipal SecurityUser user,
            @ParameterObject
            @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PagePayload<PostListResBody> body = postService.getMyPosts(user.getId(), pageable);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/favorites/{postId}")
    public ResponseEntity<String> toggleFavorite(
            @PathVariable Long postId,
            @AuthenticationPrincipal SecurityUser user) {
        boolean isFavorite = postService.toggleFavorite(postId, user.getId());
        return ResponseEntity.ok(isFavorite ? "즐겨찾기에 추가되었습니다." : "즐겨찾기가 해제되었습니다.");
    }

    @GetMapping("/favorites")
    public ResponseEntity<PagePayload<PostListResBody>> getFavoritePosts(
            @AuthenticationPrincipal SecurityUser user,
            @ParameterObject
            @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PagePayload<PostListResBody> body = postService.getFavoritePosts(user.getId(), pageable);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<String> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateReqBody reqBody,
            @AuthenticationPrincipal SecurityUser user
    ) {
        postService.updatePost(postId, reqBody, user.getId());

        return ResponseEntity.ok("게시글이 수정되었습니다.");
    }

}
