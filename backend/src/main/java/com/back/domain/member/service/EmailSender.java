package com.back.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Async("emailExecutor")
    public void sendMailAsync(String email, String code) {
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
    }
}
