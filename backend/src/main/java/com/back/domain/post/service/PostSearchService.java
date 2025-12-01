package com.back.domain.post.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.entity.Post;
import com.back.domain.post.entity.PostImage;
import com.back.domain.post.repository.PostFavoriteRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.global.s3.S3Uploader;

@Service
public class PostSearchService {

	private final PostVectorService postVectorService;
	private final PostRepository postRepository;
	private final PostFavoriteRepository postfavoriteRepository;
	private final S3Uploader s3;

	private final ChatClient rerankerClient;
	private final ChatClient answerClient;

	@Value("${custom.ai.rag-llm-answer-prompt}")
	private String answerPrompt;

	@Value("${custom.ai.rag-llm-rerank-prompt}")
	private String rerankPrompt;

	public PostSearchService(PostVectorService postVectorService, PostRepository postRepository,
		PostFavoriteRepository postfavoriteRepository, S3Uploader s3,
		@Qualifier("gpt41MiniChatClient") ChatClient rerankerClient,
		@Qualifier("gpt51ChatClient") ChatClient answerClient) {
		this.postVectorService = postVectorService;
		this.postRepository = postRepository;
		this.postfavoriteRepository = postfavoriteRepository;
		this.s3 = s3;
		this.rerankerClient = rerankerClient;
		this.answerClient = answerClient;
	}

	@Transactional(readOnly = true)
	public List<PostListResBody> searchPosts(String query, Long memberId) {

		List<Long> candidatePostIds = postVectorService.searchPostIds(query, 5);

		if (candidatePostIds.isEmpty())
			return List.of();

		List<Post> candidates = candidatePostIds.stream()
			.map(id -> postRepository.findById(id).orElse(null))
			.filter(Objects::nonNull)
			.toList();

		List<Long> recommendedIds = selectRecommendedIdsWithLLM(query, candidates);

		if (recommendedIds.isEmpty())
			return List.of();

		List<Post> recommendPosts = recommendedIds.stream()
			.map(id -> postRepository.findById(id).orElse(null))
			.filter(Objects::nonNull)
			.limit(3)
			.toList();

		return recommendPosts.stream().map(post -> {

			boolean isFavorite =
				(memberId != null) && postfavoriteRepository.existsByMemberIdAndPostId(memberId, post.getId());

			String thumbnail = post.getImages()
				.stream()
				.filter(PostImage::getIsPrimary)
				.findFirst()
				.map(img -> s3.generatePresignedUrl(img.getImageUrl()))
				.orElse(null);

			return PostListResBody.of(post, isFavorite, thumbnail);
		}).toList();
	}

	private List<Long> selectRecommendedIdsWithLLM(String query, List<Post> candidates) {
		String context = candidates.stream().map(p -> """
			ID: %d
			제목: %s
			카테고리ID: %d
			대여료: %d원
			보증금: %d원
			거래 방식: 수령=%s / 반납=%s
			지역ID들: %s
			""".formatted(p.getId(), p.getTitle(), p.getCategory().getId(), p.getFee(), p.getDeposit(),
			p.getReceiveMethod(), p.getReturnMethod(),
			p.getPostRegions().stream().map(r -> r.getRegion().getId()).toList())).collect(Collectors.joining("\n\n"));

		String prompt = rerankPrompt.formatted(context, query);

		String raw = rerankerClient.prompt(prompt).call().content();

		return parseJsonIdList(raw);

	}

	private List<Long> parseJsonIdList(String json) {
		try {
			json = json.replaceAll("[^0-9,]", "");
			if (json.isBlank())
				return List.of();

			return Arrays.stream(json.split(","))
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.map(Long::valueOf)
				.toList();
		} catch (Exception e) {
			return List.of();
		}
	}

	public String searchWithLLM(String query, List<PostListResBody> recommendedPosts) {

		String context = recommendedPosts.stream().map(p -> """
			제목: %s
			카테고리ID: %d
			대여료: %d원
			보증금: %d원
			수령 방식: %s
			반납 방식: %s
			지역ID들: %s
			""".formatted(p.title(), p.categoryId(), p.fee(), p.deposit(), p.receiveMethod(), p.returnMethod(),
			p.regionIds())).collect(Collectors.joining("\n\n"));

		String prompt = answerPrompt.formatted(query, context);

		return answerClient.prompt(prompt).call().content();
	}
}
