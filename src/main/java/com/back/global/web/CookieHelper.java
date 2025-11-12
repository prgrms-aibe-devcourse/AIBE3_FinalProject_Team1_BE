package com.back.global.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CookieHelper {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public String getCookieValue(String name, String defaultValue) {
        if(req.getCookies() == null) return defaultValue;

        return Arrays.stream(req.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(defaultValue);
    }

    public void setCookie(String name, String value) {
        boolean delete = (value == null) || value.isBlank();
        String host = req.getServerName();
        boolean isLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);

        String domain = System.getenv("COOKIE_DOMAIN");
        boolean secure = Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_SECURE",
                isLocal ? "false" : "true"));
        boolean crossSite = Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_CROSS_SITE",
                isLocal ? "false" : "true"));

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(name, delete ? "" : value)
                .path("/")
                .maxAge(delete ? 0 : 60 * 60 * 24 * 365)
                .httpOnly(true)
                .secure(secure)
                .sameSite(crossSite ? "None" : "Lax");

        if (!isLocal && domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        ResponseCookie cookie = builder.build();
        resp.addHeader("Set-Cookie", cookie.toString());
    }

    public void deleteCookie(String name) {
        setCookie(name, null);
    }
}
