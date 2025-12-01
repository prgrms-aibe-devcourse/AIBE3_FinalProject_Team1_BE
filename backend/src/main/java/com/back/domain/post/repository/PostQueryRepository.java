package com.back.domain.post.repository;

import static com.back.domain.post.entity.QPost.*;
import static com.back.domain.post.entity.QPostRegion.*;
import static com.back.domain.region.entity.QRegion.*;
import static com.back.domain.reservation.entity.QReservation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.back.domain.post.common.EmbeddingStatus;
import com.back.domain.post.dto.req.PostEmbeddingDto;
import com.querydsl.core.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.back.domain.post.entity.Post;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;

@Repository
public class PostQueryRepository extends CustomQuerydslRepositorySupport {

	public PostQueryRepository() {
		super(Post.class);
	}

	public Page<Post> findFilteredPosts(
		String keyword,
		List<Long> categoryId,
		List<Long> regionIds,
		Pageable pageable) {
		return applyPagination(
			pageable,
			contentQuery -> contentQuery
				.selectFrom(post).leftJoin(post.postRegions, postRegion).fetchJoin()
				.leftJoin(postRegion.region, region).fetchJoin()
				.where(
					containsKeyword(keyword),
					inCategoryIds(categoryId),
					inRegionIds(regionIds),
					post.isBanned.isFalse() // 제재 처리 된 게시물 제외
				)
				.distinct(),
			countQuery -> countQuery
				.select(post.count())
				.from(post)
				.leftJoin(post.postRegions, postRegion)
				.where(
					containsKeyword(keyword),
					inCategoryIds(categoryId),
					inRegionIds(regionIds
					)

				)
		);
	}

	private BooleanExpression containsKeyword(String keyword) {
		return keyword != null ? post.title.containsIgnoreCase(keyword) : null;
	}

	private BooleanExpression inCategoryIds(List<Long> categoryIds) {
		return (categoryIds == null || categoryIds.isEmpty())
			? null
			: post.category.id.in(categoryIds);
	}

	private BooleanExpression inRegionIds(List<Long> regionIds) {
		return (regionIds == null || regionIds.isEmpty())
			? null
			: postRegion.region.id.in(regionIds);
	}

	public Page<Post> findMyPost(Long memberId, Pageable pageable) {

		return applyPagination(
			pageable,
			contentQuery -> contentQuery
				.selectFrom(post)
				.where(post.author.id.eq(memberId)),
			countQuery -> countQuery
				.select(post.count())
				.from(post)
				.where(post.author.id.eq(memberId))
		);
	}

	public List<LocalDateTime> findReservedDatesFromToday(Long postId) {
		LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

		List<ReservationStatus> excludedStatuses = List.of(
			ReservationStatus.PENDING_APPROVAL,
			ReservationStatus.CANCELLED,
			ReservationStatus.REJECTED
		);

		// 시작일과 종료일을 함께 조회
		List<Reservation> reservations = select(reservation)
			.from(reservation)
			.where(
				reservation.post.id.eq(postId),
				reservation.reservationEndAt.goe(today), // 종료일이 오늘 이후
				reservation.status.notIn(excludedStatuses)
			)
			.fetch();

		// 각 예약의 시작일~종료일 사이 모든 날짜를 Set에 담기 (중복 제거)
		Set<LocalDateTime> allReservedDates = new HashSet<>();

		for (Reservation r : reservations) {
			LocalDateTime start = r.getReservationStartAt().withHour(0).withMinute(0).withSecond(0).withNano(0);
			LocalDateTime end = r.getReservationEndAt().withHour(0).withMinute(0).withSecond(0).withNano(0);

			// start부터 end까지 하루씩 증가하며 모든 날짜 추가
			LocalDateTime current = start;
			while (!current.isAfter(end)) {
				if (!current.isBefore(today)) { // 오늘 이후만
					allReservedDates.add(current);
				}
				current = current.plusDays(1);
			}
		}

		// 정렬해서 반환
		return allReservedDates.stream()
			.sorted()
			.collect(Collectors.toList());
	}

	/**
	 * ID 목록을 받아 해당 게시글들을 벌크 UPDATE를 통해 '제재(banned)' 상태로 변경합니다.
	 * @return 실제로 변경된 레코드(row) 수
	 */
	public long bulkBanPosts(List<Long> postIds) {
		long updatedCount = getQueryFactory()
				.update(post) // UPDATE Post p
				.set(post.isBanned, true) // SET p.isBanned = true
				.where(post.id.in(postIds)) // WHERE p.id IN (:postIds)
				.execute(); // 쿼리 실행 및 변경된 행 개수 반환

		// 필요에 따라 영속성 컨텍스트(JPA 1차 캐시) 초기화
		// 벌크 연산은 캐시를 우회하므로, 이후 트랜잭션 내에서 최신 데이터를
		// 조회해야 한다면 반드시 초기화해야 합니다.
		getEntityManager().clear();

		return updatedCount;
	}

	public List<Post> findPostsToEmbedWithDetails(int limit) {
		return selectFrom(post)
				.join(post.category).fetchJoin()
				.join(post.author).fetchJoin()
				.leftJoin(post.postRegions).fetchJoin()
				.where(post.embeddingStatus.eq(EmbeddingStatus.WAIT))
				.orderBy(post.createdAt.asc())  // 오래된 것부터
				.limit(limit)  // 🔥 제한 추가
				.fetch();
	}

	/**
	 * WAIT -> PENDING으로 벌크 업데이트 + 버전 증가
	 */
	public long bulkUpdateStatusToPendingWithVersion(List<Long> postIds) {
		if (postIds == null || postIds.isEmpty()) {
			return 0;
		}

		long updatedCount = getQueryFactory()
				.update(post)
				.set(post.embeddingStatus, EmbeddingStatus.PENDING)
				.set(post.embeddingVersion, post.embeddingVersion.add(1))  // 🔥 버전 증가
				.where(
						post.id.in(postIds),
						post.embeddingStatus.eq(EmbeddingStatus.WAIT)
				)
				.execute();

		getEntityManager().clear();
		return updatedCount;
	}

	/**
	 * 실제로 선점한 게시글만 필터링 (버전 검증)
	 */
	public List<PostEmbeddingDto> verifyAcquiredPosts(List<PostEmbeddingDto> postDtos) {
		if (postDtos == null || postDtos.isEmpty()) {
			return List.of();
		}

		// 예상 버전 맵 생성 (원래 버전 + 1)
		Map<Long, Long> expectedVersions = postDtos.stream()
				.collect(Collectors.toMap(
						PostEmbeddingDto::id,
						dto -> dto.embeddingVersion() + 1
				));

		List<Long> postIds = new ArrayList<>(expectedVersions.keySet());

		// PENDING 상태인 게시글의 현재 버전 조회
		List<Tuple> results = getQueryFactory()
				.select(post.id, post.embeddingVersion)
				.from(post)
				.where(
						post.id.in(postIds),
						post.embeddingStatus.eq(EmbeddingStatus.PENDING)
				)
				.fetch();

		// 버전이 일치하는 ID만 추출
		Set<Long> acquiredIds = results.stream()
				.filter(tuple -> {
					Long id = tuple.get(post.id);
					Long currentVersion = tuple.get(post.embeddingVersion);
					return currentVersion.equals(expectedVersions.get(id));
				})
				.map(tuple -> tuple.get(post.id))
				.collect(Collectors.toSet());

		// 선점 성공한 DTO만 반환
		return postDtos.stream()
				.filter(dto -> acquiredIds.contains(dto.id()))
				.toList();
	}

	public long bulkUpdateStatus(List<Long> postIds, EmbeddingStatus toStatus, EmbeddingStatus fromStatus) {
		if (postIds == null || postIds.isEmpty()) {
			return 0;
		}

		long updatedCount = getQueryFactory() // CustomQuerydslRepositorySupport의 메서드 사용
				.update(post) // UPDATE Post p
				.set(post.embeddingStatus, toStatus) // SET p.embeddingStatus = :toStatus
				.where(
						post.id.in(postIds), // WHERE p.id IN (:postIds)
						post.embeddingStatus.eq(fromStatus) // AND p.embeddingStatus = :fromStatus
				)
				.execute(); // 쿼리 실행

		// 벌크 연산 후, 영속성 컨텍스트(1차 캐시)의 데이터가 DB와 불일치하므로 반드시 초기화
		getEntityManager().clear(); // CustomQuerydslRepositorySupport의 메서드 사용

		return updatedCount;
	}
}



