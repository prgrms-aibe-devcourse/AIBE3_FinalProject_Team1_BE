package com.back.domain.review.entity;

import com.back.domain.reservation.entity.Reservation;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {
    @Column(nullable = false)
    private int equipmentScore;
    @Column(nullable = false)
    private int kindnessScore;
    @Column(nullable = false)
    private int responseTimeScore;
    @Column(nullable = false)
    private String comment;
    @Column(nullable = false)
    private boolean isBanned;

    @OneToOne
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    private Review(Reservation reservation, int equipmentScore, int kindnessScore, int responseTimeScore, String comment) {
        this.reservation = reservation;
        this.equipmentScore = equipmentScore;
        this.kindnessScore = kindnessScore;
        this.responseTimeScore = responseTimeScore;
        this.comment = comment;
        this.isBanned = false;
    }

    public static Review create(Reservation reservation, ReviewWriteReqBody reqBody) {
        return new Review(
                reservation,
                reqBody.equipmentScore(),
                reqBody.kindnessScore(),
                reqBody.responseTimeScore(),
                reqBody.comment()
        );
    }

}
