package com.erflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Map;

/**
 * 상태 코드를 오류 화면으로 잇는 규칙.
 *
 * <p>레거시 {@code web.xml} 의 {@code error-page} 두 줄에 해당한다. 실제 오류가 났을 때
 * 화면이 뜨는지는 여기서 보지 못한다 — 그것은 서블릿 컨테이너가 하는 일이라 실행 중인
 * 앱에서 확인한다.
 */
class ErrorViewConfigTest {

    private final ErrorViewResolver resolver = new ErrorViewConfig().legacyErrorViews();

    private String viewFor(HttpStatus status) {
        var view = resolver.resolveErrorView(new MockHttpServletRequest(), status, Map.of());
        return view == null ? null : view.getViewName();
    }

    @Test
    @DisplayName("404 는 페이지 없음 화면이다")
    void notFoundGoesToItsScreen() {
        assertThat(viewFor(HttpStatus.NOT_FOUND)).isEqualTo("error/not-found-error");
    }

    @Test
    @DisplayName("5xx 는 내부 서버 오류 화면이다")
    void serverErrorGoesToItsScreen() {
        // 레거시 web.xml 은 500 만 적었다. 502·503 도 사용자에게는 같은 사고다.
        assertThat(viewFor(HttpStatus.INTERNAL_SERVER_ERROR))
                .isEqualTo("error/internal-server-error");
        assertThat(viewFor(HttpStatus.SERVICE_UNAVAILABLE))
                .isEqualTo("error/internal-server-error");
    }

    @Test
    @DisplayName("그 밖의 상태에는 화면을 붙이지 않는다")
    void otherStatusesAreLeftAlone() {
        // 403 은 Spring Security 가 권한 오류 화면으로 보낸다. 405 는 레거시에
        // 화면이 없었다 — 없던 화면을 띄우는 것은 발명이다.
        assertThat(viewFor(HttpStatus.FORBIDDEN)).isNull();
        assertThat(viewFor(HttpStatus.METHOD_NOT_ALLOWED)).isNull();
    }
}
