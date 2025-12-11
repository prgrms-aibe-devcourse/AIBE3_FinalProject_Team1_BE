package com.back.domain.review.service;

import com.back.domain.review.entity.Review;
import com.back.domain.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ChatClient chatClient;
    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;
    private final ReviewQueryRepository reviewQueryRepository;

    @Value("${custom.ai.review-summary-prompt}")
    private String reviewSummaryPrompt;

    @Value("${custom.ai.author-review-summary-prompt}")
    private String authorReviewSummaryPrompt;

    public String summarizePostReviews(Long postId) {
        String lockKey = "lock:postReviewSummary:" + postId;
        RLock lock = redissonClient.getLock(lockKey);

        String cachedSummary = getCachedSummary(postId);
        if (cachedSummary != null) {
            log.info("캐시 히트 (락 전): postId={}", postId);
            return cachedSummary;
        }

        try {
            if (!lock.tryLock(15, 20, TimeUnit.SECONDS)) {
                log.error("락 획득 실패: postId={}", postId);
                throw new RuntimeException("락 획득 실패");
            }

            log.info("{} 락 획득!", lockKey);

            cachedSummary = getCachedSummary(postId);
            if (cachedSummary != null) {
                log.info("캐시 히트 (락 후): postId={} - 다른 스레드가 생성함", postId);
                return cachedSummary;
            }

            log.info("캐시 미스 - LLM 호출: postId={}", postId);
            List<Review> reviews = reviewQueryRepository.findTop30ByPostId(postId);

            if (reviews.isEmpty()) {
                cachedSummary = "후기가 없습니다.";
            } else {
                String reviewsText = reviews.stream()
                                            .map(Review::getComment)
                                            .collect(Collectors.joining("\n"));

                cachedSummary = chatClient.prompt()
                                          .system(reviewSummaryPrompt)
                                          .user("후기:\n" + reviewsText)
                                          .call()
                                          .content();
            }

            Objects.requireNonNull(cacheManager.getCache("postReviewSummary")).put(postId, cachedSummary);
            log.info("캐싱 완료: postId={}", postId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("{} 락 해제", lockKey);
            }
        }

        return cachedSummary;
    }

    private String getCachedSummary(Long postId) {
        Cache cache = cacheManager.getCache("postReviewSummary");
        if (cache != null) {
            return cache.get(postId, String.class);
        }
        return null;
    }

    @Cacheable(value = "memberReviewSummary", key = "#memberId")
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
