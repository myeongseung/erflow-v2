package com.erflow.auth;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * 보안 설정.
 *
 * <h2>레거시와 달라지는 것</h2>
 *
 * <p>레거시에는 CSRF 방어가 없었다. 이관하면서 켠다. 그 결과 <b>모든 form 에
 * {@code _csrf} hidden 입력이 생긴다</b> — 정합성 게이트가 이것을 "레거시에 없던 요소"
 * 로 잡으며, 그게 맞다. 사유를 적어 allowlist 에 등록해 통과시킨다.
 *
 * <p>세션 고정 공격 방어도 프레임워크 기본값을 쓴다. 레거시는 로그인 전후로 세션을
 * 바꾸지 않았다.
 *
 * <h2>레거시를 따르는 것</h2>
 *
 * <p>부서·직급 비트마스크({@link Permissions})와 화면별 권한
 * ({@link ScreenAuthorizationManager})은 레거시 규칙 그대로다. 비밀번호 저장 형식은
 * bcrypt 로 승격했다 — 아래 {@link #passwordEncoder()} 참조(D-128).
 *
 * <p>로그아웃도 레거시대로 <b>링크(GET)</b> 다. CSRF 를 켜면 프레임워크 기본 로그아웃이
 * POST 만 받는데, 레거시 헤더 메뉴는 링크라 그대로 두면 404 가 된다(D-055).
 *
 * <p>관리자 화면({@code /admin/**})은 {@code screen} 표에 없어 프로그램 권한으로 막을 수
 * 없다. 레거시가 화면마다 {@code isAdmin} 을 물었던 자리이므로 경로 규칙으로 막는다(D-053).
 */
@Configuration
public class SecurityConfig {

    /**
     * 비밀번호 인코더 — 저장 형식을 bcrypt 로 승격했다 (D-128, O-006 해소).
     *
     * <p>새로 저장하는 비밀번호는 전부 {@code {bcrypt}} 다. 평문을 모르는 채 승격한
     * 계정은 {@code {erflow-bcrypt}}(레거시 해시를 bcrypt 로 겹씌운 것)로 저장돼 있고,
     * 로그인이 성공하면 {@link ErflowUserDetailsService} 가 순수 bcrypt 로 다시 쓴다.
     *
     * <p>접두사가 없는 값은 레거시 형식으로 본다 — {@link PasswordStorageMigration} 이
     * 기동 때 전부 승격하지만, 승격 전에 로그인이 와도 막히지 않아야 한다.
     *
     * @return 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put(WrappedLegacyPasswordEncoder.ID, new WrappedLegacyPasswordEncoder());
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        encoder.setDefaultPasswordEncoderForMatches(new ErflowPasswordEncoder());
        return encoder;
    }

    /**
     * 필터 체인.
     *
     * @param http 보안 설정 빌더
     * @param screenAuthorization 화면 권한 판정
     * @return 필터 체인
     * @throws Exception 설정 실패 시
     */
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, ScreenAuthorizationManager screenAuthorization) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // /res 는 글꼴이다. 결재 도장 글꼴을 CSS 가 불러온다
                        // /fonts 는 화면 글꼴(Pretendard). 자체 호스팅으로 바꾸면서
                        // 생겼다 — 막혀 있으면 로그인 화면조차 글꼴 없이 뜬다(D-119)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/res/**",
                                "/fonts/**", "/favicon.ico")
                            .permitAll()
                        .requestMatchers("/login", "/login/password-error", "/login/find-password")
                            .permitAll()
                        // 비밀번호 변경은 '변경 필요' 상태에서만 들어간다
                        .requestMatchers("/login/change-password")
                            .hasAuthority(ErflowUserDetails.ROLE_PASSWORD_CHANGE)
                        .requestMatchers("/login/password-ok", "/permission-error", "/access-error")
                            .permitAll()
                        // 오류 화면은 누구에게나 보여야 한다. 로그인하지 않은 사람이
                        // 없는 주소를 열면 레거시도 404 화면을 보여줬다
                        .requestMatchers("/not-found-error", "/internal-server-error", "/error")
                            .permitAll()
                        // 관리자 화면은 screen 테이블에 없다. 프로그램 권한이 아니라
                        // isAdmin 으로 지키던 자리라 경로로 막는다(D-053)
                        .requestMatchers("/admin", "/admin/**")
                            .hasAuthority(ErflowUserDetails.ROLE_ADMIN)
                        .anyRequest().access(screenAuthorization))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("id")
                        .passwordParameter("password")
                        .successHandler(new LoginSuccessHandler())
                        // 레거시는 실패 시 passwordError.html 로 보냈다
                        .failureUrl("/login/password-error")
                        .permitAll())
                .logout(logout -> logout
                        // 레거시 로그아웃은 헤더 메뉴의 «링크»다. CSRF 를 켜면 기본
                        // 로그아웃이 POST 만 받아 그 링크가 404 가 된다(D-055)
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.GET, "/login/logout-proc"))
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true))
                // 미인가는 레거시와 같은 안내 화면으로 보낸다
                .exceptionHandling(ex -> ex.accessDeniedPage("/permission-error"));

        return http.build();
    }
}
