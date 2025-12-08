package com.back.domain.reservation.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.QMember;
import com.back.domain.post.entity.Post;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.global.app.mcp.dto.CategoryStatsDto;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.back.domain.category.entity.QCategory.category;
import static com.back.domain.member.entity.QMember.member;
import static com.back.domain.post.entity.QPost.post;
import static com.back.domain.post.entity.QPostOption.postOption;
import static com.back.domain.reservation.common.ReservationStatus.*;
import static com.back.domain.reservation.entity.QReservation.reservation;
import static com.back.domain.reservation.entity.QReservationOption.reservationOption;

@Repository
public class ReservationQueryRepository extends CustomQuerydslRepositorySupport{

    private static final List<ReservationStatus> TERMINATED_STATUSES = List.of(
            ReservationStatus.CANCELLED,
            ReservationStatus.REJECTED,
            ReservationStatus.REFUND_COMPLETED,
            ReservationStatus.CLAIM_COMPLETED
    );

    public ReservationQueryRepository() {
        super(Reservation.class);
    }

    public boolean existsOverlappingReservation(
            Long postId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long excludeReservationId) {

        Long result = select(reservation.id)
                .from(reservation)
                .where(
                        postIdEq(postId),
                        excludeReservationIdNe(excludeReservationId),
                        statusNotIn(ReservationStatus.CANCELLED, ReservationStatus.REJECTED),
                        dateOverlap(startAt, endAt)
                )
                .fetchFirst();

        return result != null;
    }

    public boolean existsActiveReservation(Long postId, Long authorId) {
        Long result = select(reservation.id)
                .from(reservation)
                .where(
                        postIdEq(postId),
                        authorIdEq(authorId),
                        statusNotIn(TERMINATED_STATUSES.toArray(new ReservationStatus[0]))
                )
                .fetchFirst();

        return result != null;
    }

    public Optional<Reservation> findByIdWithAll(Long id) {
        Reservation result = selectFrom(reservation)
                .leftJoin(reservation.post, post).fetchJoin()
                .leftJoin(reservation.author, member).fetchJoin()
                .leftJoin(reservation.reservationOptions, reservationOption).fetchJoin()
                .leftJoin(reservationOption.postOption, postOption).fetchJoin()
                .where(reservation.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    public Optional<Reservation> findByIdWithPostAndAuthor(Long id) {
        QMember guest = new QMember("guest");
        QMember host = new QMember("host");

        Reservation result = selectFrom(reservation)
                .leftJoin(reservation.post, post).fetchJoin()
                .leftJoin(reservation.author, guest).fetchJoin()
                .leftJoin(post.author, host).fetchJoin()
                .where(reservation.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    public Page<Reservation> findByAuthorWithFetch(
            Member author,
            List<ReservationStatus> status,
            String keyword,
            Pageable pageable) {

        return applyPagination(pageable,
                queryFactory -> queryFactory
                        .selectFrom(reservation)
                        .leftJoin(reservation.post, post).fetchJoin()
                        .leftJoin(post.author, member).fetchJoin()
                        .where(
                                reservation.author.eq(author),
                                statusIn(status),
                                postTitleContains(keyword)
                        ),
                queryFactory -> queryFactory
                        .select(reservation.count())
                        .from(reservation)
                        .where(
                                reservation.author.eq(author),
                                statusIn(status),
                                postTitleContains(keyword)
                        )
        );
    }

    public Page<Reservation> findByPostWithFetch(
            Post post,
            ReservationStatus status,
            Pageable pageable) {

        return applyPagination(pageable,
                queryFactory -> queryFactory
                        .selectFrom(reservation)
                        .where(
                                reservation.post.eq(post),
                                statusEq(status)
                        ),
                queryFactory -> queryFactory
                        .select(reservation.count())
                        .from(reservation)
                        .where(
                                reservation.post.eq(post),
                                statusEq(status)
                        )
        );
    }

    public List<CategoryStatsDto> getCategoryStats(LocalDateTime from, LocalDateTime to) {
        return select(Projections.constructor(
                CategoryStatsDto.class,
                category.id,
                category.name,
                reservation.count(),
                post.fee.sum()))
                .from(reservation)
                .join(reservation.post, post)
                .join(post.category, category)
                .where(
                        reservation.status.in(RETURN_COMPLETED, REFUND_COMPLETED, CLAIM_COMPLETED),
                        reservation.createdAt.between(from, to)
                )
                .groupBy(category.id, category.name)
                .orderBy(reservation.count().desc(), post.fee.sum().desc())
                .fetch();
    }
    
    public List<Reservation> findWithPostAndAuthorByStatus(ReservationStatus status) {
        return select(reservation)
                .from(reservation)
                .leftJoin(reservation.author, new QMember("reservationAuthor"))
                .fetchJoin()
                .leftJoin(reservation.post, post)
                .fetchJoin()
                .leftJoin(post.author, new QMember("postAuthor"))
                .fetchJoin()
                .where(reservation.status.eq(status))
                .fetch();
    }

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

    private Predicate statusIn(List<ReservationStatus> status) {
        return status != null && !status.isEmpty()
                ? reservation.status.in(status)
                : null;
    }

    private BooleanExpression statusNotIn(ReservationStatus... statuses) {
        return statuses != null && statuses.length > 0
                ? reservation.status.notIn(statuses)
                : null;
    }

    private BooleanExpression dateOverlap(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            return null;
        }
        return reservation.reservationStartAt.lt(endAt)
                .and(reservation.reservationEndAt.goe(startAt));
    }

    private BooleanExpression statusEq(ReservationStatus status) {
        return status != null ? reservation.status.eq(status) : null;
    }

    public List<Reservation> findWithPostAndAuthorByIds(List<Long> ids) {
        return select(reservation)
                .from(reservation)
                .leftJoin(reservation.author, new QMember("reservationAuthor"))
                .fetchJoin()
                .leftJoin(reservation.post, post)
                .fetchJoin()
                .leftJoin(post.author, new QMember("postAuthor"))
                .fetchJoin()
                .where(reservation.id.in(ids))
                .fetch();
    }

    private BooleanExpression postTitleContains(String keyword) {
        return keyword != null && !keyword.isBlank()
                ? reservation.post.title.containsIgnoreCase(keyword)
                : null;
    }

    public Map<ReservationStatus, Integer> countStatusesByAuthor(Member author) {
        List<Tuple> results = getQueryFactory()
                .select(reservation.status, reservation.count())
                .from(reservation)
                .where(reservation.author.eq(author))
                .groupBy(reservation.status)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(reservation.status),
                        tuple -> tuple.get(reservation.count()).intValue()
                ));
    }

    public List<Reservation> findAllByEndAtAndStatus(LocalDate tomorrow, ReservationStatus reservationStatus) {
        LocalDateTime start = tomorrow.atStartOfDay();
        LocalDateTime end = tomorrow.plusDays(1).atStartOfDay();

        return selectFrom(reservation)
                .leftJoin(reservation.post, post).fetchJoin()
                .leftJoin(reservation.author, member).fetchJoin()
                .where(
                        reservation.status.eq(reservationStatus),
                        reservation.reservationEndAt.goe(start),
                        reservation.reservationEndAt.lt(end)
                )
                .fetch();
    }
}
