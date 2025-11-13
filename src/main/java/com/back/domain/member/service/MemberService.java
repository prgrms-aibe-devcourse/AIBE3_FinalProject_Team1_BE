package com.back.domain.member.service;

import com.back.domain.member.common.MemberRole;
import com.back.domain.member.dto.MemberJoinReqBody;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public long count() {
        return memberRepository.count();
    }

    public Member getById(long userId) {
        return memberRepository.findById(userId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Member join(MemberJoinReqBody reqBody) {
        return join(reqBody, MemberRole.USER);
    }

    public Member join(MemberJoinReqBody reqBody, MemberRole role) {
        String password = passwordEncoder.encode(reqBody.password());
        Member member = new Member(reqBody.email(), password, reqBody.name(),
                reqBody.address1(), reqBody.address2(), reqBody.nickname(),
                reqBody.phoneNumber(), role);
        return memberRepository.save(member);
    }

    public void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "비밀번호가 올바르지 않습니다.");
        }
    }
}
