package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.standard.util.json.JsonUt;
import com.back.standard.util.jwt.JwtUt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final RefreshTokenStore rtStore;
    private final MemberService memberService; // 권한 변경 시 authVersion 올릴 때 사용
    private final Clock clock = Clock.systemUTC();

    @Value("${custom.jwt.secretKey}")
    private String jwtSecretKey;
    @Value("${custom.accessToken.expireSeconds}")
    private int accessTokenExpireSeconds;
    @Value("${custom.refreshToken.expireSeconds}")
    private int refreshTokenExpireSeconds;

    /* =================== 액세스 토큰 (JWT) =================== */
    public String genAccessToken(Member member) {
        long authVersion = rtStore.getAuthVersion(member.getId());
        Map<String, Object> claims = Map.of(
                "id", member.getId(),
                "email", member.getEmail(),
                "nickname", member.getNickname(),
                "authVersion", authVersion,
                "role", member.getRole()
        );

        return JwtUt.toString(jwtSecretKey, accessTokenExpireSeconds, claims);
    }

    /** AT 파싱 */
    public Map<String, Object> payload(String accessToken) {
        return JwtUt.payload(jwtSecretKey, accessToken); // {id, username, nickname, authVersion, roles}
    }

    /* ===== Refresh Token (opaque) in Redis ===== */
    public String issueRefresh(Member member) {
        String jti = JwtUt.newOpaqueToken(64); // 원문(쿠키)
        Instant exp = Instant.now(clock).plusSeconds(refreshTokenExpireSeconds);
        String payloadJson = JsonUt.toString(Map.of(
                "userId", member.getId(),
                "exp", exp.getEpochSecond()
        ));
        rtStore.saveRefresh(jti, member.getId(), Duration.ofSeconds(refreshTokenExpireSeconds), payloadJson);
        return jti;
    }

    /** 회전 */
    public String rotateRefresh(String oldJti) {
        String payload = rtStore.findRefreshPayload(oldJti);
        if (payload == null) throw new ServiceException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Token 입니다.");

        long userId = ((Number) JsonUt.parse(payload, Map.class).get("userId")).longValue();
        // old 삭제
        rtStore.deleteRefresh(oldJti, userId);

        // new 발급
        String newJti = JwtUt.newOpaqueToken(64);
        Instant exp = Instant.now(clock).plusSeconds(refreshTokenExpireSeconds);
        String newPayload = JsonUt.toString(Map.of("userId", userId, "exp", exp.getEpochSecond()));
        rtStore.saveRefresh(newJti, userId, Duration.ofSeconds(refreshTokenExpireSeconds), newPayload);
        return newJti;
    }

    public long findRefreshOwner(String jti) {
        String payload = rtStore.findRefreshPayload(jti);
        if (payload == null) throw new ServiceException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Token 입니다.");
        return ((Number) JsonUt.parse(payload, Map.class).get("userId")).longValue();
    }

    public void revokeRefresh(String jti) {
        String payload = rtStore.findRefreshPayload(jti);
        if (payload == null) return;
        long userId = ((Number) JsonUt.parse(payload, Map.class).get("userId")).longValue();
        rtStore.deleteRefresh(jti, userId);
    }

    public void revokeAll(long userId) {
        rtStore.revokeAllForUser(userId);
    }

    /* 권한 변경 시 호출 */
    public void bumpAuthVersion(long userId) {
        rtStore.bumpAuthVersion(userId);
    }
}
