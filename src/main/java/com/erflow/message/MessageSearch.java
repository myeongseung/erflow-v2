package com.erflow.message;

import java.util.Map;

/**
 * 쪽지함 목록 검색 조건.
 *
 * <p>레거시 {@code getMessageViews} 를 옮겼다. 받은쪽지함은 «보낸사람»(sender)으로,
 * 보낸쪽지함은 «받는사람»(receiver)으로, 둘 다 «내용»(content)으로 찾는다. 모두
 * {@code like '%값%'} 이다.
 *
 * <p>레거시는 keyfield 와 keyword 가 <b>둘 다</b> 비어 있지 않을 때만 조건을 건다.
 * 검색어가 비면 keyfield 가 있어도 전체를 보여준다.
 *
 * @param keyfield 검색 대상(receiver/sender/content)
 * @param keyword 검색어
 */
public record MessageSearch(String keyfield, String keyword) {

    /** 검색 대상 -> {@code message_view} 컬럼. */
    private static final Map<String, String> FIELDS = Map.of(
            "receiver", "receiver_name",
            "sender", "sender_name",
            "content", "content");

    /**
     * @return 조건을 걸어야 하면 {@code true}
     */
    public boolean isActive() {
        return keyfield != null && FIELDS.containsKey(keyfield)
                && keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * @return 검색 대상 컬럼명. 조건이 없으면 {@code null}
     */
    public String column() {
        return isActive() ? FIELDS.get(keyfield) : null;
    }

    /**
     * @return 앞뒤에 {@code %} 를 붙인 검색어
     */
    public String pattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }
}
