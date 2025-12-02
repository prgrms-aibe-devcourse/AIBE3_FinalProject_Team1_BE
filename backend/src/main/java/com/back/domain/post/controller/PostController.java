package com.back.domain.post.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.back.domain.post.dto.req.PostCreateReqBody;
import com.back.domain.post.dto.req.PostUpdateReqBody;
import com.back.domain.post.dto.res.GenPostDetailResBody;
import com.back.domain.post.dto.res.PostCreateResBody;
import com.back.domain.post.dto.res.PostDetailResBody;
import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.service.PostContentGenerateService;
import com.back.domain.post.service.PostSearchService;
import com.back.domain.post.service.PostService;
import com.back.domain.review.service.ReviewSummaryService;
import com.back.global.annotations.ValidateImages;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController implements PostApi {

	private final PostService postService;
	private final ReviewSummaryService reviewSummaryService;
	private final PostSearchService postSearchService;
	private final PostContentGenerateService postContentGenerateService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RsData<PostCreateResBody>> createPost(
		@Valid @RequestPart("request") PostCreateReqBody reqBody,
		@RequestPart(value = "images", required = false) List<MultipartFile> images,
		@AuthenticationPrincipal SecurityUser user
	) {

		PostCreateResBody body = this.postService.createPost(reqBody, images, user.getId());

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new RsData<>(HttpStatus.CREATED, "게시글이 생성되었습니다.", body));
	}

	@GetMapping
	public ResponseEntity<RsData<PagePayload<PostListResBody>>> getPostList(
		@AuthenticationPrincipal SecurityUser user,
		@ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) List<Long> categoryIds,
		@RequestParam(required = false) List<Long> regionIds) {

		Long memberId = (user != null) ? user.getId() : null;
		PagePayload<PostListResBody> body = this.postService.getPostList(pageable, keyword, categoryIds, regionIds,
			memberId);
		return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "성공", body));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RsData<PostDetailResBody>> getPostById(
		@PathVariable Long id,
		@AuthenticationPrincipal SecurityUser user
	) {
		Long memberId = (user != null) ? user.getId() : null;

		PostDetailResBody body = this.postService.getPostById(id, memberId);

		return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "성공", body));
	}

	@GetMapping("/{id}/reserved-dates")
	public ResponseEntity<RsData<List<LocalDateTime>>> getReservedDates(
		@PathVariable Long id
	) {
		List<LocalDateTime> body = this.postService.getReservedDates(id);

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
		@RequestPart(value = "images", required = false) List<MultipartFile> images,
		@AuthenticationPrincipal SecurityUser user) {

		postService.updatePost(id, reqBody, images, user.getId());

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
		String body = reviewSummaryService.summarizePostReviews(id);

		return ResponseEntity.ok(new RsData<>(HttpStatus.OK, HttpStatus.OK.name(), body));
	}

	@PostMapping("/gen-detail")
	public ResponseEntity<RsData<GenPostDetailResBody>> genDetail(
		@ValidateImages @RequestPart("images") List<MultipartFile> imageFiles,
		@RequestPart(name = "additionalInfo", required = false) String additionalInfo) {
		GenPostDetailResBody result = postContentGenerateService.generatePostDetail(imageFiles, additionalInfo);
		RsData<GenPostDetailResBody> response = new RsData<>(HttpStatus.OK, "응답 생성 성공", result);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/search-ai")
	public ResponseEntity<RsData<?>> searchPostsWithAi(
		@RequestParam String query,
		@AuthenticationPrincipal SecurityUser user
	) {

		Long memberId = (user != null ? user.getId() : null);

		List<PostListResBody> recommendedPosts = postSearchService.searchPosts(query, memberId);

		String answer = postSearchService.searchWithLLM(query, recommendedPosts);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("query", query);
		result.put("answer", answer);
		result.put("posts", recommendedPosts);

		return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "AI 검색 결과입니다.", result));
	}

}
