package com.back.domain.post.service;

import com.back.domain.post.dto.res.PostListResBody;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostFavoriteRepository;
import com.back.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final PostVectorService postVectorService;
    private final PostRepository postRepository;
    private final PostFavoriteRepository postfavoriteRepository;
    private final ChatClient chatClient;

    @Value("${custom.ai.rag-llm-answer-prompt}")
    private String ragPrompt;

    public List<PostListResBody> searchPosts(String query, Long memberId) {

        List<Long> candidatePostIds = postVectorService.searchPostIds(query, 5);

        if (candidatePostIds.isEmpty()) return List.of();

        List<Post> candidates = candidatePostIds.stream()
                .map(id -> postRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        List<Long> recommendedIds = selectRecommendedIdsWithLLM(query, candidates);

        if (recommendedIds.isEmpty()) return List.of();

        List<Post> recommendPosts = recommendedIds.stream()
                .map(id -> postRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        return recommendPosts.stream()
                .map(post -> {

                    boolean isFavorite = (memberId != null)
                            && postfavoriteRepository.existsByMemberIdAndPostId(memberId, post.getId());

                    String thumbnail = post.getImages().isEmpty()
                            ? null
                            : post.getImages().get(0).getImageUrl();

                    return PostListResBody.of(post, isFavorite, thumbnail);
                })
                .toList();
    }

    private List<Long> selectRecommendedIdsWithLLM(String query, List<Post> candidates) {
        String context = candidates.stream()
                .map(p -> """
                        ID: %d
                        제목: %s
                        카테고리ID: %d
                        대여료: %d원
                        보증금: %d원
                        거래 방식: 수령=%s / 반납=%s
                        지역ID들: %s
                        """.formatted(
                        p.getId(),
                        p.getTitle(),
                        p.getCategory().getId(),
                        p.getFee(),
                        p.getDeposit(),
                        p.getReceiveMethod(),
                        p.getReturnMethod(),
                        p.getPostRegions().stream()
                                .map(r -> r.getRegion().getId())
                                .toList()
                ))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                다음은 벡터 검색으로 찾은 후보 게시글들이야.
                
                %s
                
                사용자 질문: "%s"
                
                위 후보군 중 질문과 가장 관련 있는 게시글을 0~3개 골라줘.
                관련도가 낮은 건 선택 안해도돼.
                반드시 JSON 배열 형태로만 반환해줘.
                예시: [21, 5] 또는 [] 또는 [20]
                
                설명은 절대 하지 마.
                """.formatted(context, query);

        String raw = chatClient.prompt(prompt)
                .options(ChatOptions.builder()
                        .temperature(1.0)
                        .build())
                .call()
                .content();

        return parseJsonIdList(raw);

    }

    private List<Long> parseJsonIdList(String json) {
        try {
            json = json.replaceAll("[^0-9,]", "");
            if (json.isBlank()) return List.of();

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

        String context = recommendedPosts.stream()
                .map(p -> """
                        제목: %s
                        카테고리ID: %d
                        대여료: %d원
                        보증금: %d원
                        수령 방식: %s
                        반납 방식: %s
                        지역ID들: %s
                        """.formatted(
                        p.title(),
                        p.categoryId(),
                        p.fee(),
                        p.deposit(),
                        p.receiveMethod(),
                        p.returnMethod(),
                        p.regionIds()
                ))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                %s
                
                ---------------------
                [사용자 질문]
                %s
                
                [추천된 게시글 정보]
                %s
                """.formatted(ragPrompt, query, context);

        return chatClient.prompt(prompt)
                .options(ChatOptions.builder()
                        .temperature(1.0)
                        .build())
                .call()
                .content();
    }

}
