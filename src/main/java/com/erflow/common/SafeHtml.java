package com.erflow.common;

import java.util.List;
import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * 저장된 본문 HTML 을 화면에 내보내기 전에 거른다.
 *
 * <h2>왜 이스케이프가 아닌가</h2>
 *
 * <p>레거시는 본문을 {@code out.print(content)} 로 그대로 내보냈다 — 저장된 HTML 이
 * 그대로 실행된다는 뜻이다(O-010). 1단계는 {@code th:utext} 로 같은 동작을 옮겼다.
 *
 * <p>단순히 {@code th:text} 로 바꿔 이스케이프할 수는 없다. <b>결재 문서와 양식은
 * 본문 전체가 표 HTML</b> 이다 — 실제 데이터에서 {@code td} 186개, {@code table}
 * 10개, {@code input} 32개를 셌다. 이스케이프하면 결재 문서가 태그 글자 무더기가
 * 된다. 이 자리는 «HTML 이 곧 화면» 이므로 지우는 대신 <b>거른다</b>(D-118).
 *
 * <h2>왜 style 을 허용하는가</h2>
 *
 * <p>거르는 정화기의 기본 설정은 {@code style} 을 지운다. 그런데 결재 문서의 표는
 * <b>테두리·배경색·정렬·높이가 전부 인라인 {@code style}</b> 이다. 지우면 문서가
 * 테두리 없는 맨 표가 되어 알아볼 수 없다.
 *
 * <p>그래서 {@code style} 을 허용하되 <b>CSS 속성 이름과 값까지 검사</b>한다.
 * 허용 목록은 아래에 못 박고, 값 검사는 라이브러리가 한다 — {@code url(...)} 이나
 * {@code expression(...)} 이 든 값은 통과하지 못한다. 목록을 손으로 만들지 않은
 * 이유는 틀릴 방법이 너무 많기 때문이다.
 *
 * <h2>왜 input 에 name·value 를 주지 않는가</h2>
 *
 * <p>양식의 {@code input} 은 <b>«빈칸 모양» 일 뿐</b>이다 — 실제 데이터의
 * {@code input} 32개에 {@code name} 도 {@code value} 도 없고 {@code type}·
 * {@code style}·{@code checked} 만 있다. 모양만 살리면 충분하다. {@code name} 을
 * 허용하면 저장된 본문이 <b>화면의 진짜 폼에 칸을 끼워 넣을</b> 수 있다.
 *
 * <h2>어디서 거르는가</h2>
 *
 * <p>저장할 때가 아니라 <b>화면에 찍을 때</b>다. DB 는 그대로 두므로 이미 저장된
 * 본문도 즉시 보호되고, 정책이 지나쳤다는 것이 나중에 드러나도 되돌릴 수 있다 —
 * 저장할 때 걸렀다면 사람이 쓴 글이 영구히 깎인다.
 */
public final class SafeHtml {

    /**
     * 남기는 태그.
     *
     * <p>앞쪽은 실제 데이터에서 관측한 것들이고({@code p a img div span h1 table
     * tbody tr td th input}), 뒤는 편집기가 흔히 내는 서식 태그다. 목록에 없는
     * 태그는 <b>내용만 남고 껍데기가 벗겨진다</b> — 글이 사라지지는 않는다.
     */
    private static final List<String> ELEMENTS = List.of(
            "p", "br", "div", "span", "hr",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "strong", "b", "em", "i", "u", "s", "sub", "sup",
            "ul", "ol", "li", "dl", "dt", "dd",
            "blockquote", "pre", "code",
            "table", "thead", "tbody", "tfoot", "caption", "colgroup", "col",
            "tr", "td", "th",
            "a", "img", "input");

    /**
     * 남기는 CSS 속성.
     *
     * <p>결재 문서가 실제로 쓰는 것은 {@code border*}·{@code background-color}·
     * {@code text-align}·{@code width}·{@code height}·{@code margin*} 이다. 여기에
     * 같은 성격의 글자·여백 속성을 더했다. <b>{@code position}·{@code z-index} 는
     * 없다</b> — 그 둘이 있으면 저장된 본문이 화면 위에 덮개를 씌울 수 있다.
     */
    private static final List<String> CSS = List.of(
            "background-color", "color",
            "border", "border-bottom", "border-collapse", "border-color", "border-left",
            "border-right", "border-spacing", "border-style", "border-top", "border-width",
            "font-family", "font-size", "font-style", "font-weight",
            "height", "line-height", "width",
            "margin", "margin-bottom", "margin-left", "margin-right", "margin-top",
            "padding", "padding-bottom", "padding-left", "padding-right", "padding-top",
            "text-align", "text-decoration", "vertical-align", "white-space");

    /** 거르는 정책. 만드는 값이 불변이라 한 번 만들어 돌려 쓴다. */
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements(ELEMENTS.toArray(String[]::new))
            .allowStyling(CssSchema.withProperties(CSS))
            .allowAttributes("class").globally()
            // 표의 칸 합치기·정렬. 레거시 편집기가 style 과 섞어 쓴다.
            .allowAttributes("colspan", "rowspan", "align", "valign", "width", "height",
                    "nowrap").onElements("td", "th")
            .allowAttributes("border", "cellspacing", "cellpadding", "align", "width")
            .onElements("table")
            .allowAttributes("span").onElements("col", "colgroup")
            // 양식의 빈칸. 모양만 남긴다 — name·value 는 주지 않는다.
            .allowAttributes("type", "checked", "readonly", "disabled").onElements("input")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            // 실제 데이터의 링크·그림은 https 와 상대경로뿐이다.
            // javascript: 와 data: 를 막아도 잃는 것이 없다.
            .allowUrlProtocols("http", "https", "mailto")
            .requireRelNofollowOnLinks()
            .toFactory();

    private SafeHtml() {
    }

    /**
     * 화면에 내보낼 수 있는 HTML 로 거른다.
     *
     * <p>{@code <script>}·{@code on...} 이벤트 속성·{@code javascript:} 주소는
     * 사라지고, 표와 서식은 남는다.
     *
     * @param html 저장된 본문. {@code null} 이면 {@code null}
     * @return 거른 HTML
     */
    public static String clean(String html) {
        return html == null ? null : POLICY.sanitize(html);
    }
}
