package com.back.domain.member.dto;

import java.time.LocalDateTime;

public record MemberSendCodeResBody(
        LocalDateTime expiresIn
) {
}
