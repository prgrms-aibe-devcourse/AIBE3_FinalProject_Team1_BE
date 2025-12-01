package com.back.domain.post.service;

import com.back.domain.post.common.EmbeddingStatus;
import com.back.domain.post.dto.req.PostEmbeddingDto;
import com.back.domain.post.repository.PostQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostTransactionService {
    private final PostQueryRepository postQueryRepository;

    /**
     * WAIT -> PENDING으로 벌크 업데이트 (버전 증가 포함)
     */
    @Transactional
    public long updateStatusToPending(List<Long> postIds) {
        return postQueryRepository.bulkUpdateStatusToPendingWithVersion(postIds);
    }

    /**
     * 실제로 선점한 게시글만 검증
     */
    @Transactional(readOnly = true)
    public List<PostEmbeddingDto> verifyAcquiredPosts(List<PostEmbeddingDto> postDtos) {
        return postQueryRepository.verifyAcquiredPosts(postDtos);
    }

    /**
     * PENDING -> DONE으로 업데이트 (개별 처리, 버전 변경 없음)
     */
    @Transactional
    public void updateStatusToDone(Long postId) {
        postQueryRepository.bulkUpdateStatus(
                List.of(postId),
                EmbeddingStatus.DONE,
                EmbeddingStatus.PENDING
        );
    }

    /**
     * PENDING -> WAIT으로 복구 (개별 처리, 버전 변경 없음)
     */
    @Transactional
    public void updateStatusToWait(Long postId) {
        postQueryRepository.bulkUpdateStatus(
                List.of(postId),
                EmbeddingStatus.WAIT,
                EmbeddingStatus.PENDING
        );
    }
}
