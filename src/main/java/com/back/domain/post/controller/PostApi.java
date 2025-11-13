package com.back.domain.post.controller;

import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Post API", description = "게시글 기능 관련 API")
public interface PostApi {

    @Operation(summary = "게시글 생성 API", description = "새로운 게시글을 생성합니다.")
    ResponseEntity<String> createPost(@Valid @RequestBody PostCreateReqBody postCreateReqBody, @AuthenticationPrincipal SecurityUser securityUser);

}
