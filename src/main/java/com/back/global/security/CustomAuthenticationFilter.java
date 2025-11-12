package com.back.global.security;

import com.back.domain.member.member.common.MemberRole;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.AuthTokenService;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.member.service.RefreshTokenStore;
import com.back.global.exception.ServiceException;
import com.back.global.web.CookieHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final CookieHelper cookieHelper;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore; // authVersion 조회 등

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // Preflight(OPTIONS)은 그대로 통과
            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1) Access Token 검사 (헤더 → 쿠키)
            String accessToken = resolveAccessToken();
            if (!accessToken.isBlank()) {
                Map<String, Object> claims = authTokenService.payload(accessToken);
                if (claims != null) {
                    // 내부에서 authVersion 체크 & 권한 세팅
                    setAuthenticationFromClaims(claims);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 2) Refresh Token로 회복 (쿠키 전용)
            String refreshPlain = cookieHelper.getCookieValue("refreshToken", "");
            if (!refreshPlain.isBlank()) {
                try {
                    // Redis에서 소유자(userId) 확인
                    long userId = authTokenService.findRefreshOwner(refreshPlain);
                    // 회전 + 새 AT 발급
                    String newRefresh = authTokenService.rotateRefresh(refreshPlain);
                    // 최신 유저(권한/닉네임 등)
                    Member owner = memberService.getById(userId);
                    String newAccess = authTokenService.genAccessToken(owner);

                    // 쿠키 갱신
                    cookieHelper.setCookie("refreshToken", newRefresh);
                    cookieHelper.setCookie("accessToken", newAccess);

                    // 현재 요청 인증 확정
                    setAuthenticationFromUser(owner);

                    filterChain.doFilter(request, response);
                    return;
                } catch (ServiceException e) {
                    // 회복 실패 → 401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            // ✅ 3) 토큰이 없으면 익명 인증 설정 (permitAll 경로를 위해)
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                        "anonymous",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                );
                SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
            }

            filterChain.doFilter(request, response);

        } catch (ServiceException e) {
            // 서비스 레벨 예외는 상태코드만 세팅하고 종료
            response.setStatus(e.getRsData().status());
        }
    }

    /** DB(User) 객체 기반 인증 세팅 */
    private void setAuthenticationFromUser(Member member) {
        var authorities = member.getAuthorities(); // User.getAuthorities()는 ROLE_ 접두사 포함해야 함
        SecurityUser principal = new SecurityUser(
                member.getId(), member.getEmail(), "", member.getNickname(), authorities
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** Authorization: Bearer ... → or cookie(accessToken) */
    private String resolveAccessToken() {
        return cookieHelper.getCookieValue("accessToken", "");
    }

    /** AT claims로부터 인증 세팅 (authVersion 즉시성 체크 포함) */
    private void setAuthenticationFromClaims(Map<String, Object> claims) {
        long id = ((Number) claims.get("id")).longValue();
        String email = (String) claims.get("email");
        String nickname = (String) claims.get("nickname");

        // authVersion 체크
        long tokenVer = toLong(claims.getOrDefault("authVersion", 1));
        long serverVer = refreshTokenStore.getAuthVersion(id);
        if (serverVer != tokenVer) {
            throw new ServiceException("401-9", "권한 정보가 변경되었습니다. 재로그인 해주세요.");
        }

        // role 단순화
        String roleStr = (String) claims.getOrDefault("role", "USER");
        MemberRole role = MemberRole.valueOf(roleStr);
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );

        SecurityUser principal = new SecurityUser(id, email, "", nickname, authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private long toLong(Object n) {
        if (n instanceof Number) return ((Number) n).longValue();
        try {
            return Long.parseLong(String.valueOf(n));
        } catch (Exception e) {
            return 1L;
        }
    }
}
