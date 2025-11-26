package com.back.domain.post.controller;

import com.back.domain.post.dto.res.PostBannedResBody;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Post Admin API", description = "게시글 관리자 API, 관리자 인증 필요")
public interface PostAdmApi {
    @Operation(summary = "게시글 제재 API", description = "id에 해당하는 게시글을 제재합니다.")
    ResponseEntity<RsData<PostBannedResBody>> banPost(@PathVariable Long id);

    @Operation(summary = "게시글 제재 해제 API", description = "id에 해당하는 게시글의 제재를 해제합니다.")
    ResponseEntity<RsData<PostBannedResBody>> unbanPost(@PathVariable Long id);
}
