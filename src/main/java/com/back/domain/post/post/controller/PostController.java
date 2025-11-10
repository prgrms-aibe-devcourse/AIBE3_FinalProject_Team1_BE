package com.back.domain.post.post.controller;

import com.back.domain.post.post.dto.PostCreateReqBody;
import com.back.domain.post.post.service.PostService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public RsData<Void> createPost(@Valid @RequestBody PostCreateReqBody reqBody) {
        // Post creation logic would go here

        return RsData.success("Post created successfully.");
    }
}
