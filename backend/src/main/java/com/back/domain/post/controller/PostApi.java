package com.back.domain.post.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.PostCreateResBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Post API", description = "게시글 기능 관련 API")
public interface PostApi {

	@Operation(summary = "게시글 생성 API", description = "새로운 게시글을 생성합니다.")
	ResponseEntity<RsData<PostCreateResBody>> createPost(@Valid @RequestPart("request") PostCreateReqBody reqBody,
		@RequestPart(value = "file", required = false) List<MultipartFile> files,
		@AuthenticationPrincipal SecurityUser user);

	@Operation(summary = "게시글 목록 조회 API", description = "게시글 목록을 조회합니다.")
	ResponseEntity<RsData<PagePayload<PostListResBody>>> getPostList(
		@AuthenticationPrincipal SecurityUser user,
		@ParameterObject
		@PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC)
		Pageable pageable,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) List<Long> categoryIds,
		@RequestParam(required = false) List<Long> regionIds
	);

	@Operation(summary = "게시글 상세 조회 API", description = "특정 게시글의 상세 정보를 조회합니다.")
	ResponseEntity<RsData<PostDetailResBody>> getPostById(
		@PathVariable Long postId,
		@AuthenticationPrincipal SecurityUser user
	);

	@Operation(summary = "예약된 날짜 조회 API", description = "특정 게시글의 예약된 날짜 목록을 조회합니다.")
	ResponseEntity<RsData<List<LocalDateTime>>> getReservedDates(
		@PathVariable Long postId
	);

	@Operation(summary = "내 게시글 목록 조회 API", description = "로그인한 사용자의 게시글 목록을 조회합니다.")
	ResponseEntity<RsData<PagePayload<PostListResBody>>> getMyPostList(
		@AuthenticationPrincipal SecurityUser user,
		@ParameterObject
		@PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC)
		Pageable pageable
	);

	@Operation(summary = "즐겨찾기 토글 API", description = "특정 게시글에 대해 즐겨찾기를 설정 및 해제합니다.")
	ResponseEntity<RsData<Boolean>> toggleFavorite(
		@PathVariable Long postId,
		@AuthenticationPrincipal SecurityUser user
	);

	@Operation(summary = "즐겨찾기 게시글 목록 조회 API", description = "로그인한 사용자의 즐겨찾기한 게시글 목록을 조회합니다.")
	ResponseEntity<RsData<PagePayload<PostListResBody>>> getFavoritePosts(
		@AuthenticationPrincipal SecurityUser user,
		@ParameterObject
		@PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC)
		Pageable pageable
	);

	@Operation(summary = "게시글 수정 API", description = "특정 게시글의 정보를 수정합니다.")
	ResponseEntity<RsData<Void>> updatePost(
		@PathVariable Long id,
		@Valid @RequestPart("request") PostUpdateReqBody reqBody,
		@RequestPart(value = "file", required = false) List<MultipartFile> files,
		@AuthenticationPrincipal SecurityUser user
	);

	@Operation(summary = "게시글 삭제 API", description = "특정 게시글을 삭제합니다.")
	ResponseEntity<RsData<Void>> deletePost(
		@PathVariable Long postId,
		@AuthenticationPrincipal SecurityUser user
	);

	@Operation(summary = "게시글 후기 요약 API", description = "AI가 특정 게시글의 후기 목록을 요약합니다.")
	ResponseEntity<RsData<String>> summarizeReviews(@PathVariable Long postId);

	@Operation(summary = "AI 검색 API", description = "유사 게시글을 AI로 검색하고 설명을 생성합니다.")
	ResponseEntity<RsData<?>> searchPostsWithAi(
		@RequestParam String query,
		@AuthenticationPrincipal SecurityUser user
	);
}
