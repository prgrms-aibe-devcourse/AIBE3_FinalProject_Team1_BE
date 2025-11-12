package com.back.global.security;

import com.back.global.rsData.RsData;
import com.back.standard.util.json.JsonUt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAuthenticationFilter customAuthenticationFilter;

    @Value("${custom.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;
    // CORS 허용 메서드
    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    };
    // CORS 허용 헤더
    private static final String[] ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "Accept", "X-Requested-With"
    };

    // 인증 없이 접근 가능한 공개 엔드포인트
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/members/**",           // 회원 인증 (로그인, 회원가입, OAuth2 등)
            "/swagger-ui/**",         // Swagger UI
            "/swagger-ui.html",       // Swagger UI HTML
            "/v3/api-docs", "/v3/api-docs/**", // Swagger OpenApi JSON 문서
            "/h2-console/**",          // H2 콘솔 (개발용)
            "/actuator/health", "/actuator/health/**", "/actuator/info",    // Spring Actuator
            "/api/actuator/health", "/api/actuator/health/**", "/api/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 접근 권한 설정
                .authorizeHttpRequests(
                auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/posts", "/api/v1/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/regions/**").permitAll()
                        .requestMatchers("/api/v1/adm/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // 기본 보안 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // csrf 보호기능 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 폼 비활성
                .logout(AbstractHttpConfigurer::disable) // 로그아웃 기능 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
                .sessionManagement(AbstractHttpConfigurer::disable) // 세션 관리 비활성화
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 인증 실패 시 응답 처리
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(401);
                                            response.getWriter().write(
                                                    JsonUt.toString(
                                                            new RsData<Void>("401-1", "로그인 후 이용해주세요.")
                                                    )
                                            );
                                        }
                                )
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    JsonUt.toString(
                                                            new RsData<Void>("403-1", "권한이 없습니다.")
                                                    )
                                            );
                                        }
                                )
                ).build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 허용할 오리진 설정
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        // 허용할 메소드 설정
        configuration.setAllowedMethods(Arrays.asList(ALLOWED_METHODS));
        // 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList(ALLOWED_HEADERS));
        // 자격 증명 허용 설정
        configuration.setAllowCredentials(true);

        // CORS 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
