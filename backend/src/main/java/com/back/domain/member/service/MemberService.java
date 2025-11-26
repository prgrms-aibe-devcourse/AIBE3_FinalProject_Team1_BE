package com.back.domain.member.service;

import com.back.domain.member.common.MemberRole;
import com.back.domain.member.dto.MemberBannedResBody;
import com.back.domain.member.dto.MemberDto;
import com.back.domain.member.dto.MemberJoinReqBody;
import com.back.domain.member.dto.MemberUpdateReqBody;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3;

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
        Member member = new Member(reqBody.email(), password, reqBody.nickname(), role);
        return memberRepository.save(member);
    }

    public void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "비밀번호가 올바르지 않습니다.");
        }
    }

    public Member updateMember(Long memberId, MemberUpdateReqBody reqBody, MultipartFile profileImage) {
        Member member = getById(memberId);
        member.updateMember(reqBody);

        // 프로필 이미지 등록
        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지가 DB에 기록되어 있다면 S3에서 파일 삭제
            if (member.getProfileImgUrl() != null) {
                s3.delete(member.getProfileImgUrl());
            }
            String profileImgUrl = s3.upload(profileImage);
            member.updateProfileImage(profileImgUrl);
        }
        // removeProfileImage가 true 면 프로필 이미지 삭제
        else if (reqBody.removeProfileImage()) {
            // 기존 이미지가 DB에 기록되어 있다면 S3에서 파일 삭제
            if (member.getProfileImgUrl() != null) {
                s3.delete(member.getProfileImgUrl());
            }
            member.updateProfileImage(null);
        }

        return memberRepository.save(member);
    }

    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional
    public MemberBannedResBody banMember(Long memberId) {
        Member member = getById(memberId);
        if (member.isBanned()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미 차단된 회원입니다.");
        }
        member.ban();
        return MemberBannedResBody.of(member);
    }

    @Transactional
    public MemberBannedResBody unbanMember(Long id) {
        Member member = getById(id);
        if (!member.isBanned()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "차단되지 않은 회원입니다.");
        }
        member.unban();
        return MemberBannedResBody.of(member);
    }
}
