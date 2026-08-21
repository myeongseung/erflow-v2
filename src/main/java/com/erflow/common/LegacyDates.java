package com.erflow.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 레거시가 화면에 찍던 날짜 모양.
 *
 * <h2>왜 한 곳에 모으는가</h2>
 *
 * <p>레거시는 날짜를 <b>화면이 아니라 ResultSet 을 읽는 자리에서</b> 바꿨다.
 * {@code ResultSetExtractHelper} 안에 세 군데가 있다.
 *
 * <pre>
 * extractTaskBean       created_at  ->  yyyy년 MM월 dd일
 * extractViewtTaskBean  created_at  ->  yyyy년 MM월 dd일
 * extractUserViewBean   hired_at    ->  yyyy년 MM월 dd일
 * </pre>
 *
 * <p>그래서 JSP 는 {@code <%=createdAt%>} 로 그냥 찍는데 화면에는 한글 날짜가 뜬다.
 * 화면만 읽어서는 어디서 바뀌는지 알 수 없고, 실제로 <b>실화면 대조로만 드러났다</b>
 * (D-088). 게이트는 날짜 자리를 «dyn» 으로 넘기므로 형식을 보지 못한다.
 *
 * <p>같은 앱 안에서 형식이 화면마다 다르다는 것은 이미 적어 두었다(D-026). 그 목록에
 * 이 셋이 더해지는 셈이며, 흩어 두면 또 어긋난다.
 */
public final class LegacyDates {

    /** 레거시 {@code WebHelper.getDate} 가 만드는 모양. */
    private static final DateTimeFormatter KOREAN =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /** 레거시가 DB 에서 글자로 읽어 그대로 찍던 모양. */
    private static final DateTimeFormatter STORED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LegacyDates() {
    }

    /**
     * «2023년 09월 21일».
     *
     * <p>레거시 {@code WebHelper.getDate(String)} 에 대응한다. 그쪽은 읽을 수 없는
     * 글자를 만나면 {@code ParseException} 을 던져 화면이 통째로 죽는다 — 그 죽음은
     * 옮기지 않는다.
     *
     * @param value 저장된 시각. {@code null} 이면 {@code null}
     * @return 한글 날짜
     */
    public static String korean(LocalDateTime value) {
        return value == null ? null : value.format(KOREAN);
    }

    /**
     * 글자로 받은 시각을 «2023년 09월 21일» 로 바꾼다.
     *
     * @param value {@code yyyy-MM-dd HH:mm:ss} 글자
     * @return 한글 날짜. 읽을 수 없으면 받은 글자 그대로
     */
    public static String korean(String value) {
        LocalDateTime parsed = parse(value);
        return parsed == null ? value : parsed.format(KOREAN);
    }

    /**
     * «2023-11-10 03:16:39».
     *
     * <p>{@code LocalDateTime.toString()} 을 그대로 찍으면 가운데에 {@code T} 가
     * 들어간다. 레거시는 DB 에서 글자로 읽었으므로 공백이다.
     *
     * @param value 저장된 시각. {@code null} 이면 {@code null}
     * @return 공백으로 이어진 시각
     */
    public static String stored(LocalDateTime value) {
        return value == null ? null : value.format(STORED);
    }

    private static LocalDateTime parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace('T', ' '), STORED);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
