package com.back.domain.post.repository;

import com.back.domain.post.entity.Post;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.back.domain.post.entity.QPost.post;
import static com.back.domain.post.entity.QPostRegion.postRegion;
import static com.back.domain.region.entity.QRegion.region;
import static com.back.domain.reservation.entity.QReservation.reservation;

@Repository
public class PostQueryRepository extends CustomQuerydslRepositorySupport {

    public PostQueryRepository(){
        super(Post.class);
    }

    public Page<Post> findFilteredPosts(
            String keyword,
            Long categoryId,
            List<Long> regionIds,
            Pageable pageable) {
        return applyPagination(
                pageable,
                contentQuery -> contentQuery
                        .selectFrom(post).leftJoin(post.postRegions, postRegion).fetchJoin()
                        .leftJoin(postRegion.region, region).fetchJoin()
                        .where(
                                containsKeyword(keyword),
                                equalsCategoryId(categoryId),
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
                                equalsCategoryId(categoryId),
                                inRegionIds(regionIds
                                )

                        )
        );
    }

    private BooleanExpression containsKeyword(String keyword) {
        return keyword != null ? post.title.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression equalsCategoryId(Long categoryId) {
        return categoryId != null ? post.category.id.eq(categoryId) : null;
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
}



