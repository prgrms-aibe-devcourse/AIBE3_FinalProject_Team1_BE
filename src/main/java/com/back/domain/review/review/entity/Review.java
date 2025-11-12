package com.back.domain.review.review.entity;

import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Review extends BaseEntity {
    @Column(nullable = false)
    private int equipmentScore;
    @Column(nullable = false)
    private int kindnessScore;
    @Column(nullable = false)
    private int responseTimeScore;
    @Column(nullable = false)
    private String comment;
    @Builder.Default
    @Column(nullable = false)
    private boolean isBanned = false;

    @OneToOne
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;
}
