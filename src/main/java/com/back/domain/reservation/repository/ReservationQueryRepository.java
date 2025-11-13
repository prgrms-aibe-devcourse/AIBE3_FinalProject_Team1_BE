package com.back.domain.reservation.repository;

import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.back.domain.reservation.entity.QReservation.reservation;


@Repository
public class ReservationQueryRepository extends CustomQuerydslRepositorySupport
        implements ReservationRepositoryCustom {

    // 종료된 상태 목록
    private static final List<ReservationStatus> TERMINATED_STATUSES = List.of(
            ReservationStatus.CANCELLED,
            ReservationStatus.REJECTED,
            ReservationStatus.REFUND_COMPLETED,
            ReservationStatus.CLAIM_COMPLETED
    );

    public ReservationQueryRepository() {
        super(Reservation.class);
    }

    @Override
    public boolean existsOverlappingReservation(
            Long postId,
            LocalDate startAt,
            LocalDate endAt,
            Long excludeReservationId) {

        Long result = select(reservation.id)
                .from(reservation)
                .where(
                        postIdEq(postId),
                        excludeReservationIdNe(excludeReservationId),
                        statusNotIn(ReservationStatus.CANCELLED, ReservationStatus.REJECTED),
                        dateOverlap(startAt, endAt)
                )
                .fetchFirst();  // LIMIT 1

        return result != null;
    }

    @Override
    public boolean existsActiveReservation(Long postId, Long authorId) {
        Long result = select(reservation.id)
                .from(reservation)
                .where(
                        postIdEq(postId),
                        authorIdEq(authorId),
                        statusNotIn(TERMINATED_STATUSES.toArray(new ReservationStatus[0]))
                )
                .fetchFirst();  // LIMIT 1

        return result != null;
    }

    // ===== 동적 조건 메서드 (Report 예시 스타일) =====

    private BooleanExpression postIdEq(Long postId) {
        return postId != null ? reservation.post.id.eq(postId) : null;
    }

    private BooleanExpression authorIdEq(Long authorId) {
        return authorId != null ? reservation.author.id.eq(authorId) : null;
    }

    private BooleanExpression excludeReservationIdNe(Long excludeReservationId) {
        return excludeReservationId != null
                ? reservation.id.ne(excludeReservationId)
                : null;
    }

    private BooleanExpression statusNotIn(ReservationStatus... statuses) {
        return statuses != null && statuses.length > 0
                ? reservation.status.notIn(statuses)
                : null;
    }

    private BooleanExpression dateOverlap(LocalDate startAt, LocalDate endAt) {
        if (startAt == null || endAt == null) {
            return null;
        }
        return reservation.reservationStartAt.lt(endAt)
                .and(reservation.reservationEndAt.goe(startAt));
    }
}
