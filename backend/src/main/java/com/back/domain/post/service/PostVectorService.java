package com.back.domain.post.service;

import com.back.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostVectorService {

    private final VectorStore vectorStore;

    public void indexPost(Post post) {

        String embeddingText = """
                제목: %s
                설명: %s
                카테고리: %s
                대여료: %d원
                보증금: %d원
                수령 방식: %s
                반납 방식: %s
                지역ID들: %s
                """.formatted(
                post.getTitle(),
                post.getContent(),
                post.getCategory().getName(),
                post.getFee(),
                post.getDeposit(),
                post.getReceiveMethod(),
                post.getReturnMethod(),
                post.getPostRegions().stream()
                        .map(r -> r.getRegion().getId())
                        .toList()
        );

        String docId = UUID.randomUUID().toString();

        vectorStore.add(List.of(
                new Document(
                        docId,
                        embeddingText,
                        Map.of("postId", post.getId())
                )
        ));
    }

    public List<Long> searchPostIds(String query, int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);

        return docs.stream()
                .map(doc -> (Number) doc.getMetadata().get("postId"))
                .map(Number::longValue)
                .toList();
    }
}
