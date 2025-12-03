package com.back.domain.review.service;

import com.back.domain.review.entity.Review;
import com.back.domain.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ChatClient chatClient;
    private final ReviewQueryRepository reviewQueryRepository;

    @Value("${custom.ai.review-summary-prompt}")
    private String reviewSummaryPrompt;

    @Value("${custom.ai.author-review-summary-prompt}")
    private String authorReviewSummaryPrompt;

    public String summarizePostReviews(Long postId) {
        List<Review> reviews = reviewQueryRepository.findTop30ByPostId(postId);

        if (reviews.isEmpty()) {
            return "후기가 없습니다.";
        }

        String reviewsText = reviews.stream()
                                    .map(Review::getComment)
                                    .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                         .system(reviewSummaryPrompt)
                         .user("후기:\n" + reviewsText)
                         .call()
                         .content();
    }

    public String summarizeMemberReviews(Long memberId) {
        List<Review> reviews = reviewQueryRepository.findTop30ByMemberId(memberId);

        if (reviews.isEmpty()) {
            return "후기가 없습니다.";
        }

        String reviewsText = reviews.stream()
                .map(Review::getComment)
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .system(authorReviewSummaryPrompt)
                .user("후기:\n" + reviewsText)
                .call()
                .content();
    }
}
