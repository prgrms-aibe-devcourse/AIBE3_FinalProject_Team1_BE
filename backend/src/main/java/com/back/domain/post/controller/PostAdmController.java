package com.back.domain.post.controller;

import com.back.domain.post.dto.res.PostBannedResBody;
import com.back.domain.post.service.PostService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/posts")
public class PostAdmController implements PostAdmApi {
    private final PostService postService;

    @PatchMapping("/{id}/ban")
    public ResponseEntity<RsData<PostBannedResBody>> banPost(
            @PathVariable Long id
    ) {
        PostBannedResBody postBannedResBody = postService.banPost(id);
        RsData<PostBannedResBody> response = new RsData<>(200, "게시물이 제재되었습니다.", postBannedResBody);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<RsData<PostBannedResBody>> unbanPost(
            @PathVariable Long id
    ) {
        PostBannedResBody postBannedResBody = postService.unbanPost(id);
        RsData<PostBannedResBody> response = new RsData<>(200, "게시물 제재가 해제되었습니다.", postBannedResBody);
        return ResponseEntity.ok(response);
    }
}
