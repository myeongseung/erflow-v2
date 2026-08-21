package com.erflow;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * ERFlow 애플리케이션 진입점.
 *
 * <p>JSP + Servlet(Model 1) 레거시를 Spring Boot 로 이관한 것이다. 레거시 원본은
 * {@code legacy/ERFlow} 에 있으며 그것이 정합성 판정의 정답이다. 화면 동작을 바꾸는
 * 변경은 {@code migration/design/00-decisions.md} 에 근거를 남긴다.
 */
@SpringBootApplication
public class ErflowApplication {

    /**
     * 애플리케이션을 기동한다.
     *
     * @param args 명령행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(ErflowApplication.class, args);
    }

    /**
     * «지금» 의 출처.
     *
     * <p>레거시는 화면 안에서 {@code LocalDate.now()} 를 불렀다. 그러면 «오늘» 에 기대는
     * 화면을 시험할 수 없다 — 관리자 대시보드가 그날의 근무 기록을 읽는다. 값을 주입해
     * 두면 시험에서 날짜를 고정할 수 있다.
     *
     * @return 시스템 기본 시간대의 시계
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
