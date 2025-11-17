package com.back.domain.review.service;

import com.back.domain.review.entity.Review;
import com.back.domain.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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

    public String summarizeReviews(Long postId) {
        List<Review> reviews = reviewQueryRepository.findTop30ByPostId(postId);

        if (reviews.isEmpty()) {
            return "후기가 없습니다.";
        }

        String reviewsText = reviews.stream()
                                    .map(Review::getComment)
                                    .collect(Collectors.joining("\n"));

        Prompt prompt = createPrompt(reviewsText);

        return chatClient.prompt(prompt).call().content();
    }

    private Prompt createPrompt(String reviewsText) {
        PromptTemplate promptTemplate = new PromptTemplate(reviewSummaryPrompt);
        promptTemplate.add("reviewsText", reviewsText);

        Message systemMessage = new SystemMessage(promptTemplate.create().getContents());

        return new Prompt(List.of(systemMessage));
    }
}
