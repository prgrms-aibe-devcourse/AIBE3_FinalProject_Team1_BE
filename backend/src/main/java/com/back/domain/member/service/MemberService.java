package com.back.domain.member.service;

import com.back.domain.member.dto.*;
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

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3;
    private final EmailService emailService;

    public long count() {
        return memberRepository.count();
    }

    public Member getById(long userId) {
        return memberRepository.findById(userId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public MemberDto toMemberDto(Member member) {
        String presignedUrl = s3.getProfileThumbnailUrl(member.getProfileImgUrl());
        return new MemberDto(member, presignedUrl);
    }
    public SimpleMemberDto toSimpleMemberDto(Member member) {
        String presignedUrl = s3.getProfileThumbnailUrl(member.getProfileImgUrl());
        return new SimpleMemberDto(member, presignedUrl);
    }

    public Member authenticateAndGetMember(String email, String password) {
        Member member = findByEmail(email)
                .orElseThrow(() -> new ServiceException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        checkPassword(member, password);
        return member;
    }

    public Member join(MemberJoinReqBody reqBody) {
        if (memberRepository.existsByEmail(reqBody.email())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다.");
        }

        String password = passwordEncoder.encode(reqBody.password());
        Member member = Member.createForJoin(reqBody.email(), password, reqBody.nickname());
        return memberRepository.save(member);
    }
    public Member joinForAdmin(MemberJoinReqBody reqBody) {
        if (memberRepository.existsByEmail(reqBody.email())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다.");
        }

        String password = passwordEncoder.encode(reqBody.password());
        Member member = Member.createForAdmin(reqBody.email(), password, reqBody.nickname());
        return memberRepository.save(member);
    }

    public void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");
        }
    }

    public Member updateMember(Long memberId, MemberUpdateReqBody reqBody, MultipartFile profileImage) {
        Member member = getById(memberId);
        member.updateMember(reqBody);

        // 프로필 이미지 등록
        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지가 DB에 기록되어 있다면 S3에서 파일 삭제
            if (member.getProfileImgUrl() != null) {
                s3.deleteProfileSafely(member.getProfileImgUrl());
            }
            String originalUrl = s3.uploadProfileOriginal(profileImage);
            member.updateProfileImage(originalUrl);
        }
        // removeProfileImage가 true 면 프로필 이미지 삭제
        else if (reqBody.removeProfileImage()) {
            // 기존 이미지가 DB에 기록되어 있다면 S3에서 파일 삭제
            if (member.getProfileImgUrl() != null) {
                s3.deleteProfileSafely(member.getProfileImgUrl());
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

    public LocalDateTime sendEmailVerificationCode(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다.");
        }

        return emailService.sendVerificationCode(email);
    }
}
