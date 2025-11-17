package com.back.domain.review.repository;

import com.back.domain.review.dto.ReviewAuthorDto;
import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.entity.Review;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.Projections;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.back.domain.post.entity.QPost.post;
import static com.back.domain.reservation.entity.QReservation.reservation;
import static com.back.domain.review.entity.QReview.review;

@Repository
public class ReviewQueryRepository extends CustomQuerydslRepositorySupport {
    public ReviewQueryRepository() {
        super(Review.class);
    }

    public Page<ReviewDto> getPostReceivedReviews(Pageable pageable, Long postId){
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
                        .where(post.id.eq(postId)),
                countQuery -> countQuery
                        .select(review.count())
                        .from(review)
                        .join(review.reservation, reservation)
                        .join(reservation.post, post)
                        .where(post.id.eq(postId))
        );
    }

    public Page<ReviewDto> getMemberReceivedReviews(Pageable pageable, Long memberId){
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
                        .where(post.author.id.eq(memberId)),
                countQuery -> countQuery
                        .select(review.count())
                        .from(review)
                        .join(review.reservation, reservation)
                        .join(reservation.post, post)
                        .where(post.author.id.eq(memberId))
        );
    }

    public List<Review> findTop30ByPostId(Long postId) {
        return selectFrom(review)
                .join(review.reservation, reservation)
                .join(reservation.post, post)
                .where(post.id.eq(postId))
                .orderBy(review.id.desc())
                .limit(30)
                .fetch();
    }
}
