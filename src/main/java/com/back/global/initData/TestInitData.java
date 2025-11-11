package com.back.global.initData;

import com.back.domain.member.member.dto.MemberJoinReqBody;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.dto.CreateReservationReqBody;
import com.back.domain.reservation.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Configuration
public class TestInitData {
    private final MemberService memberService;
    private final ReservationService reservationService;
    
    @Autowired
    @Lazy
    private TestInitData self;

    @Profile("test")
    @Bean
    ApplicationRunner testInitDataApplicationRunner() {
        return arg -> {
            self.work1();
//            self.work2();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;
        MemberJoinReqBody joinReqBody = new MemberJoinReqBody(
                "test@example.com",
                "password123",
                "John Doe",
                "123 Main St",
                "Apt 4B",
                "TestUser",
                "123-456-7890"
        );

        Member testUser = memberService.join(joinReqBody);
    }

    @Transactional
    public void work2() {
        if (reservationService.count() > 0) return;
        Member testMember = memberService.findByEmail("test@example.com")
                .orElseThrow(() -> new RuntimeException("테스트 계정을 찾을 수 없습니다."));

        CreateReservationReqBody reqBody = new CreateReservationReqBody(
                ReservationDeliveryMethod.OFFLINE,
                "TestCarrier",
                "TEST123",
                "456 Test St",
                "Unit 1",
                ReservationDeliveryMethod.OFFLINE,
                "ReturnCarrier",
                "RET456",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7)
        );

        reservationService.create(reqBody, testMember);
    }
}