package com.erflow.post;

import java.util.Arrays;
import java.util.List;

/**
 * 조회수를 올릴지 정한다.
 *
 * <h2>레거시의 판단이 뒤집혀 있다</h2>
 *
 * <p>{@code postView.jsp} 는 {@code postId} 쿠키에 이미 본 글 번호를 쌓아 중복
 * 집계를 막으려 했다. 그런데 조건이 반대다.
 *
 * <pre>
 * if (cookie == null) {
 *     result = activityCon.updateCount(postId);          // 쿠키가 없으면 올린다
 * } else {
 *     if (cookiePostList.indexOf(postId + "") != -1) {   // 목록에 &lt;b&gt;있으면&lt;/b&gt;
 *         cookie.setValue(cookie.getValue() + ";" + postId);
 *         result = activityCon.updateCount(postId);      // 또 올린다
 *     }
 * }
 * </pre>
 *
 * <p>결과는 이렇다.
 *
 * <table border="1">
 * <caption>레거시 조회수 집계</caption>
 * <tr><th>상황</th><th>동작</th></tr>
 * <tr><td>쿠키가 아예 없다 (첫 조회)</td><td>올린다</td></tr>
 * <tr><td>쿠키는 있는데 이 글은 처음</td><td><b>안 올린다</b></td></tr>
 * <tr><td>이미 본 글을 또 연다</td><td><b>볼 때마다 올린다</b></td></tr>
 * </table>
 *
 * <p>중복을 막으려던 장치가 정확히 중복만 세고 있다. 새 글은 세지 않는다.
 *
 * <p>그대로 옮긴다. 조회수는 화면에 뜨는 값이라 규칙을 고치면 레거시와 다른
 * 숫자가 된다. D-029 참조.
 *
 * <p>글쓴이 본인이 열면 이 판단 자체를 하지 않는다 — 레거시가
 * {@code isCreatedUser} 로 먼저 걸러낸다.
 *
 * <h2>구분자만 바꾼다</h2>
 *
 * <p>레거시는 번호를 {@code ;} 로 이어 붙이는데 그 글자는 쿠키 값에 들어갈 수 없다.
 * Tomcat 이 거부해서 <b>이미 본 글을 다시 열면 500 이 난다.</b> 실화면 대조로
 * 찾았다. 세는 규칙은 그대로 두고 구분자만 바꾼다 — D-033 참조.
 */
public final class PostViewCounter {

    /** 레거시가 쓰던 쿠키 이름. */
    public static final String COOKIE_NAME = "postId";

    /**
     * 번호를 잇는 글자.
     *
     * <p>레거시는 {@code ;} 를 썼다. 쿠키 값에 넣을 수 없는 글자라 붙이는 순간
     * 500 이 난다. 밑줄은 허용된다. 값의 뜻은 달라지지 않는다.
     */
    static final String SEPARATOR = "_";

    private PostViewCounter() {
    }

    /**
     * 조회수를 올려야 하는지 판정한다.
     *
     * @param cookieValue {@code postId} 쿠키 값. 쿠키가 없으면 {@code null}
     * @param postId 지금 여는 글번호
     * @return 올려야 하면 {@code true}
     */
    public static boolean shouldIncrement(String cookieValue, int postId) {
        if (cookieValue == null) {
            return true;
        }
        return seen(cookieValue).contains(String.valueOf(postId));
    }

    /**
     * 쿠키에 담을 새 값.
     *
     * <p>레거시는 이미 있는 번호를 또 이어 붙인다. 값이 계속 길어진다.
     *
     * @param cookieValue 지금 쿠키 값. 없으면 {@code null}
     * @param postId 지금 여는 글번호
     * @return 새 쿠키 값
     */
    public static String nextValue(String cookieValue, int postId) {
        if (cookieValue == null) {
            return String.valueOf(postId);
        }
        return seen(cookieValue).contains(String.valueOf(postId))
                ? cookieValue + SEPARATOR + postId
                : cookieValue;
    }

    private static List<String> seen(String cookieValue) {
        // 레거시가 남긴 `;` 짜리 값도 읽을 수 있어야 한다 — 실제로는 만들어지지
        // 못하고 500 이 났지만, 다른 Tomcat 설정에서는 남아 있을 수 있다.
        return Arrays.asList(cookieValue.split("[;_]"));
    }
}
