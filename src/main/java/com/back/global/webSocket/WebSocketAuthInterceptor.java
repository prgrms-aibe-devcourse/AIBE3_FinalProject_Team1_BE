package com.back.global.webSocket;

import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.global.exception.ServiceException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        log.info("[Handshake 요청]");

        if (request instanceof ServletServerHttpRequest servletRequest) {

            HttpServletRequest httpReq = servletRequest.getServletRequest();
            log.info("HttpServletRequest: {} {}", httpReq.getMethod(), httpReq.getRequestURI());

            Cookie[] cookies = httpReq.getCookies();

            if (cookies == null) {
                throw new ServiceException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }

            String accessToken = null;
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                }
            }

            if (accessToken == null || accessToken.isBlank()) {
                throw new ServiceException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }

            var claims = authTokenService.payload(accessToken);
            if (claims == null) {
                throw new ServiceException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            }

            long userId = ((Number) claims.get("id")).longValue();
            long tokenVer = ((Number) claims.get("authVersion")).longValue();
            long serverVer = refreshTokenStore.getAuthVersion(userId);

            if (tokenVer != serverVer) {
                throw new ServiceException(HttpStatus.UNAUTHORIZED, "권한이 변경되었습니다. 다시 로그인해주세요.");
            }

            log.info("[Handshake 인증 통과] userId={}", userId);

            attributes.put("accessToken", accessToken);

            return true;
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
