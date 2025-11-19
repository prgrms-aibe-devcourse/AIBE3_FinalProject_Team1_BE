package com.back.global.initData;

import com.back.domain.member.common.MemberRole;
import com.back.domain.member.dto.MemberJoinReqBody;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Profile("prod")
@RequiredArgsConstructor
@Configuration
public class ProdInitData {
    private final MemberService memberService;
    @Autowired
    @Lazy
    private ProdInitData self;

    @Bean
    ApplicationRunner notProdInitDataApplicationRunner() {
        return args -> {
            self.work1();
        };
    }
    
    @Transactional
    public void work1() {
        if(memberService.count() > 0) return;

        MemberJoinReqBody reqBody1 = new MemberJoinReqBody(
                "admin@0.0",
                "root123414",
                "관리자",
                "관리동",
                "101동 101호",
                "관리닉네임",
                "010-0000-7777"
                );
        Member admin = memberService.join(reqBody1, MemberRole.ADMIN);

        MemberJoinReqBody reqBody2 = new MemberJoinReqBody(
                "0@0.0",
                "root123414",
                "일반회원",
                "일반동",
                "101동 101호",
                "일반닉네임",
                "010-0000-0000"
        );
        Member member = memberService.join(reqBody2, MemberRole.USER);
    }
}
