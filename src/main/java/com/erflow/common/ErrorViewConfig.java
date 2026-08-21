package com.erflow.common;

import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * 오류 응답에 레거시 화면을 붙인다.
 *
 * <p>레거시는 {@code web.xml} 에서 상태 코드를 화면에 이었다.
 *
 * <pre>
 * 404 -&gt; /notFoundError.jsp
 * 500 -&gt; /internalServerError.html   &lt;- 없는 파일이다. 실제 파일은 .jsp 다
 * </pre>
 *
 * <p>500 쪽은 한 글자가 어긋나 한 번도 뜬 적이 없다. 그래도 잇는다 — 근거는
 * {@code design/00-decisions.md} D-042 에 적었다.
 *
 * <p>404 와 5xx 밖의 상태는 잇지 않는다. 레거시 {@code web.xml} 도 그 둘만 적었고,
 * 없던 화면을 새로 띄우는 것은 발명이다.
 */
@Configuration
public class ErrorViewConfig {

    /**
     * 상태 코드를 레거시 오류 화면으로 잇는다.
     *
     * @return 오류 화면 결정자. 맡지 않는 상태에는 {@code null} 을 돌려준다
     */
    @Bean
    public ErrorViewResolver legacyErrorViews() {
        return (request, status, model) -> {
            if (status == HttpStatus.NOT_FOUND) {
                return new ModelAndView("error/not-found-error");
            }
            if (status.is5xxServerError()) {
                return new ModelAndView("error/internal-server-error");
            }
            return null;
        };
    }
}
