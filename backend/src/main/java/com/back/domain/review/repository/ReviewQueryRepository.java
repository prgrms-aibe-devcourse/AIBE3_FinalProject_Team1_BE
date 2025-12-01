package com.back.domain.review.repository;

import com.back.domain.member.entity.QMember;
import com.back.domain.review.dto.ReviewAuthorDto;
import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.entity.Review;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.Projections;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.back.domain.post.entity.QPost.post;
import static com.back.domain.reservation.entity.QReservation.reservation;
import static com.back.domain.review.entity.QReview.review;

@Repository
public class ReviewQueryRepository extends CustomQuerydslRepositorySupport {
    public ReviewQueryRepository() {
        super(Review.class);
    }

    public Page<ReviewDto> findPostReceivedReviews(Pageable pageable, Long postId){
        return applyPagination(pageable,
                contentQuery -> contentQuery
                        .select(Projections.constructor(ReviewDto.class,
                                review.id,
                                review.equipmentScore,
                                review.kindnessScore,
                                review.responseTimeScore,
                                review.comment,
                                review.createdAt,
                                Projections.constructor(ReviewAuthorDto.class,
                                        reservation.author.id,
                                        reservation.author.nickname,
                                        reservation.author.profileImgUrl
                                )
                        ))
                        .from(review)
                        .join(review.reservation, reservation)
                        .join(reservation.post, post)
                        .where(
                                post.id.eq(postId),
                                review.isBanned.isFalse()
                        ),
                countQuery -> countQuery
                        .select(review.count())
                        .from(review)
                        .join(review.reservation, reservation)
                        .join(reservation.post, post)
                        .where(
                                post.id.eq(postId),
                                review.isBanned.isFalse()
                        )
        );
    }

    public Page<ReviewDto> findMemberReceivedReviews(Pageable pageable, Long memberId){
        return applyPagination(pageable,
                contentQuery -> contentQuery
                        .select(Projections.constructor(ReviewDto.class,
                                review.id,
                                review.equipmentScore,
                                review.kindnessScore,
                                review.responseTimeScore,
                                review.comment,
                                review.createdAt,
                                Projections.constructor(ReviewAuthorDto.class,
                                        reservation.author.id,
                                        reservation.author.nickname,
                                        reservation.author.profileImgUrl
                                )
                        ))
                        .from(review)
                        .join(review.reservation, reservation)
                        .join(reservation.post, post)
                        .where(
                                post.author.id.eq(memberId),
                                review.isBanned.isFalse()
                        ),
                countQuery -> countQuery
                        .select(review.count())
                        .from(review)
                        .join(review.reservation, reservation)
                        .join(reservation.post, post)
                        .where(
                                post.author.id.eq(memberId),
                                review.isBanned.isFalse()
                        )
        );
    }

    public List<Review> findTop30ByPostId(Long postId) {
        return selectFrom(review)
                .join(review.reservation, reservation)
                .join(reservation.post, post)
                .where(
                        post.id.eq(postId),
                        review.isBanned.isFalse()
                )
                .orderBy(review.id.desc())
                .limit(30)
                .fetch();
    }

    public List<Review> findTop30ByMemberId(Long memberId) {
        return selectFrom(review)
                .join(review.reservation, reservation)
                .join(reservation.post, post)
                .where(
                        post.author.id.eq(memberId),
                        review.isBanned.isFalse()
                )
                .orderBy(review.id.desc())
                .limit(30)
                .fetch();
    }

    public Set<Long> findReviewedReservationIds(List<Long> reservationIds, Long authorId) {
        if (reservationIds.isEmpty()) {
            return Collections.emptySet();
        }

        return new HashSet<>(
                select(review.reservation.id)
                        .from(review)
                        .where(
                                review.reservation.id.in(reservationIds),
                                review.reservation.author.id.eq(authorId)
                        )
                        .fetch()
        );
    }

    public List<Review> findWithReservationAndPostAndAuthorsByIds(List<Long> reviewIds) {
        return selectFrom(review)
                .leftJoin(review.reservation, reservation).fetchJoin()
                .leftJoin(reservation.author, new QMember("reservationAuthor")).fetchJoin()
                .leftJoin(reservation.post, post).fetchJoin()
                .leftJoin(post.author, new QMember("postAuthor")).fetchJoin()
                .where(review.id.in(reviewIds))
                .fetch();
    }

    public long bulkBanReview(List<Long> reviewIds) {
        long updatedCount = getQueryFactory()
                .update(review)
                .set(review.isBanned, true)
                .where(review.id.in(reviewIds))
                .execute();

        getEntityManager().clear();

        return updatedCount;
    }
}
