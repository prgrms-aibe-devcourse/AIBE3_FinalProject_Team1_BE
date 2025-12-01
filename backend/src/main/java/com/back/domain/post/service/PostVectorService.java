package com.back.domain.post.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.post.dto.req.PostEmbeddingDto;
import com.back.domain.post.entity.Post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostVectorService {

	private final VectorStore vectorStore;
	private final JdbcTemplate jdbcTemplate;

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
			.map(doc -> (Number)doc.getMetadata().get("postId"))
			.map(Number::longValue)
			.toList();
	}

	public void deletePost(long postId) {
		String sql = """
			    SELECT id 
			    FROM vector_store 
			    WHERE JSON_EXTRACT(metadata, '$.postId') = ?
			""";

		List<String> docsIds = jdbcTemplate.queryForList(sql, String.class, postId);

		if (docsIds.isEmpty()) {
			return;
		}

		vectorStore.delete(docsIds);
	}

	@Transactional
	public void indexPost(PostEmbeddingDto dto) {
		try {
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
				dto.title(),
				dto.content(),
				dto.categoryName(),
				dto.fee(),
				dto.deposit(),
				dto.receiveMethod(),
				dto.returnMethod(),
				dto.regionIds()
			);

			log.info("임베딩 텍스트 생성 완료 - 길이: {} bytes", embeddingText.getBytes().length);

			String docId = UUID.randomUUID().toString();

			Document doc = new Document(
				docId,
				embeddingText,
				Map.of("postId", dto.id())
			);

			log.info("Document 객체 생성 완료 - docId: {}", docId);

			log.info("VectorStore.add() 호출 시작...");
			vectorStore.add(List.of(doc));
			log.info("✅ VectorStore.add() 성공! Post ID: {}", dto.id());

		} catch (Exception e) {
			log.error("❌ indexPost 실패 - Post ID: {}", dto.id(), e);
			throw new RuntimeException("벡터 저장 실패: Post ID " + dto.id(), e);
		}
	}
}
