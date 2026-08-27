package com.erflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 본문 HTML 거르기(D-118, O-010).
 *
 * <p>두 가지를 같이 못 박는다 — <b>실행되는 것은 사라지고, 서식은 남는다.</b> 하나만
 * 보면 정책이 한쪽으로 무너진다. 지우기만 하면 결재 문서가 알아볼 수 없게 되고,
 * 남기기만 하면 막은 것이 없다.
 */
class SafeHtmlTest {

    @Test
    @DisplayName("script 는 태그도 내용도 남지 않는다")
    void dropsScript() {
        String safe = SafeHtml.clean("<p>글</p><script>alert('x')</script>");

        assertThat(safe).contains("글").doesNotContain("script").doesNotContain("alert");
    }

    @Test
    @DisplayName("on... 이벤트 속성은 사라지고 요소는 남는다")
    void dropsEventAttributes() {
        String safe = SafeHtml.clean("<img src=\"photo.png\" onerror=\"alert('x')\">");

        assertThat(safe).contains("photo.png").doesNotContain("onerror").doesNotContain("alert");
    }

    @Test
    @DisplayName("javascript: 주소는 사라지고 글자는 남는다")
    void dropsJavascriptUrl() {
        String safe = SafeHtml.clean("<a href=\"javascript:alert('x')\">누르기</a>");

        assertThat(safe).contains("누르기").doesNotContain("javascript");
    }

    @Test
    @DisplayName("https 와 상대경로는 남는다 — 실제 데이터가 그 둘뿐이다")
    void keepsAllowedUrls() {
        assertThat(SafeHtml.clean("<a href=\"https://erflow.test/a\">링크</a>"))
                .contains("https://erflow.test/a");
        assertThat(SafeHtml.clean("<img src=\"/upload/a.png\">")).contains("/upload/a.png");
    }

    @Test
    @DisplayName("iframe·object 같은 것은 통째로 사라진다")
    void dropsEmbeddedObjects() {
        String safe = SafeHtml.clean("<p>앞</p><iframe src=\"https://x.test\"></iframe><p>뒤</p>");

        assertThat(safe).contains("앞").contains("뒤").doesNotContain("iframe");
    }

    @Test
    @DisplayName("결재 문서의 표 서식이 남는다 — 이게 지워지면 문서를 못 읽는다")
    void keepsTableStyling() {
        String safe = SafeHtml.clean("""
                <table style="border-collapse:collapse; border:1px solid black" cellspacing="0">
                  <tbody><tr>
                    <th colspan="2" style="background-color:lightblue; text-align:center">머리</th>
                  </tr><tr>
                    <td rowspan="2" style="border-color:black; border-style:solid; height:37.5px">칸</td>
                  </tr></tbody>
                </table>
                """);

        assertThat(safe).contains("<table").contains("<td").contains("<th")
                .contains("colspan").contains("rowspan")
                .contains("border-collapse").contains("background-color")
                .contains("text-align").contains("머리").contains("칸");
    }

    @Test
    @DisplayName("양식의 빈칸은 모양만 남는다 — name·value 는 주지 않는다")
    void keepsInputShapeButNotItsIdentity() {
        String safe = SafeHtml.clean(
                "<input type=\"checkbox\" checked style=\"width:16px\" name=\"pw\" value=\"x\">");

        assertThat(safe).contains("checkbox").contains("checked").contains("width")
                .doesNotContain("name=").doesNotContain("value=");
    }

    @Test
    @DisplayName("덮개를 씌우는 CSS 는 사라진다")
    void dropsOverlayCss() {
        String safe = SafeHtml.clean(
                "<div style=\"position:fixed; top:0; left:0; z-index:9999\">덮개</div>");

        assertThat(safe).contains("덮개").doesNotContain("position").doesNotContain("z-index");
    }

    @Test
    @DisplayName("CSS 값에 숨은 주소도 사라진다")
    void dropsUrlInsideCss() {
        String safe = SafeHtml.clean(
                "<td style=\"background-color:url(javascript:alert('x'))\">칸</td>");

        assertThat(safe).contains("칸").doesNotContain("javascript").doesNotContain("url(");
    }

    @Test
    @DisplayName("허용하지 않는 태그는 껍데기만 벗겨진다 — 글은 사라지지 않는다")
    void unknownTagLosesOnlyItsShell() {
        String safe = SafeHtml.clean("<marquee>흐르는 글자</marquee>");

        assertThat(safe).contains("흐르는 글자").doesNotContain("marquee");
    }

    @Test
    @DisplayName("빈 값은 그대로")
    void emptyStaysEmpty() {
        assertThat(SafeHtml.clean(null)).isNull();
        assertThat(SafeHtml.clean("")).isEmpty();
    }
}
