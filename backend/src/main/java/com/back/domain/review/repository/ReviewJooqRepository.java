package com.back.domain.review.repository;

import com.back.domain.review.dto.ReviewSummaryDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static com.back.jooq.tables.Post.POST;
import static com.back.jooq.tables.Reservation.RESERVATION;
import static com.back.jooq.tables.Review.REVIEW;
import static org.jooq.impl.DSL.avg;
import static org.jooq.impl.DSL.count;

@Repository
@RequiredArgsConstructor
public class ReviewJooqRepository {
    private final DSLContext dsl;

    private static float roundValue(double value) {
        return (float) (Math.round(value * 10) / 10.0);
    }

    public ReviewSummaryDto findPostReceivedReviewSummary(Long postId) {
        // ✅ 집계 필드 정의
        Field<BigDecimal> equipmentAvg = avg(REVIEW.EQUIPMENT_SCORE);
        Field<BigDecimal> kindnessAvg  = avg(REVIEW.KINDNESS_SCORE);
        Field<BigDecimal> responseAvg  = avg(REVIEW.RESPONSE_TIME_SCORE);
        Field<Integer> reviewCount     = count(REVIEW.ID);

        // ✅ Record4 타입으로 결과 받기
        Record4<BigDecimal, BigDecimal, BigDecimal, Integer> record = dsl
                .select(equipmentAvg, kindnessAvg, responseAvg, reviewCount)
                .from(REVIEW)
                .join(RESERVATION).on(REVIEW.RESERVATION_ID.eq(RESERVATION.ID))
                .join(POST).on(RESERVATION.POST_ID.eq(POST.ID))
                .where(
                        POST.ID.eq(postId)
                                .and(REVIEW.IS_BANNED.eq(false))
                )
                .fetchOne();

        return toSummary(record, equipmentAvg, kindnessAvg, responseAvg, reviewCount);
    }

    public ReviewSummaryDto findMemberReceivedReviewSummary(Long memberId) {
        Field<BigDecimal> equipmentAvg = avg(REVIEW.EQUIPMENT_SCORE);
        Field<BigDecimal> kindnessAvg  = avg(REVIEW.KINDNESS_SCORE);
        Field<BigDecimal> responseAvg  = avg(REVIEW.RESPONSE_TIME_SCORE);
        Field<Integer> reviewCount     = count(REVIEW.ID);

        Record4<BigDecimal, BigDecimal, BigDecimal, Integer> record = dsl
                .select(equipmentAvg, kindnessAvg, responseAvg, reviewCount)
                .from(REVIEW)
                .join(RESERVATION).on(REVIEW.RESERVATION_ID.eq(RESERVATION.ID))
                .join(POST).on(RESERVATION.POST_ID.eq(POST.ID))
                .where(
                        POST.AUTHOR_ID.eq(memberId)
                                .and(REVIEW.IS_BANNED.eq(false))
                )
                .fetchOne();

        return toSummary(record, equipmentAvg, kindnessAvg, responseAvg, reviewCount);
    }

    private ReviewSummaryDto toSummary(
            Record4<BigDecimal, BigDecimal, BigDecimal, Integer> record,
            Field<BigDecimal> equipmentAvg,
            Field<BigDecimal> kindnessAvg,
            Field<BigDecimal> responseAvg,
            Field<Integer> reviewCount
    ) {
        if (record == null) {
            return ReviewSummaryDto.empty();
        }

        // ✅ count는 Integer
        Integer count = record.get(reviewCount);
        if (count == null || count == 0) {
            return ReviewSummaryDto.empty();
        }

        BigDecimal eqAvg = record.get(equipmentAvg);
        BigDecimal kdAvg = record.get(kindnessAvg);
        BigDecimal rtAvg = record.get(responseAvg);

        double eq = eqAvg != null ? eqAvg.doubleValue() : 0.0;
        double kd = kdAvg != null ? kdAvg.doubleValue() : 0.0;
        double rt = rtAvg != null ? rtAvg.doubleValue() : 0.0;

        return new ReviewSummaryDto(
                roundValue(eq),
                roundValue(kd),
                roundValue(rt),
                roundValue((eq + kd + rt) / 3.0),
                count.longValue()  // ReviewSummaryDto가 long count 쓰니까 형변환
        );
    }
}
