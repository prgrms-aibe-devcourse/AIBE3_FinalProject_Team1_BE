package com.back.domain.member.service;

import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final String REDIS_KEY_PREFIX = "email:verify:signup:";
    private static final long TTL_SECONDS = 5 * 60L;

    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;

    public LocalDateTime sendVerificationCode(String email) {
        String code = generateCode();
        String key = buildKey(email);

        stringRedisTemplate.opsForValue()
                .set(key, code, TTL_SECONDS, TimeUnit.SECONDS);

        // 만료 시간 계산 (서버 기준 현재 시간 + TTL)
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(TTL_SECONDS);

        String subject = "[Chwi-Meet] 이메일 인증코드 안내";
        String content = """
                안녕하세요. Chwi-Meet 입니다.

                이메일 인증을 위해 아래 인증코드를 입력해주세요.

                인증코드: %s

                유효시간: 5분

                감사합니다.
                """.formatted(code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);

        // 클라이언트에게 만료 시간 내려주기
        return expiresAt;
    }

    public void verifyCode(String email, String code) {
        String key = buildKey(email);
        String savedCode = stringRedisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new ServiceException(HttpStatus.GONE, "인증코드가 만료되었거나 존재하지 않습니다.");
        }

        if (!savedCode.equals(code)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다.");
        }

        stringRedisTemplate.delete(key);
    }

    private String buildKey(String email) {
        return REDIS_KEY_PREFIX + email;
    }

    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
}
