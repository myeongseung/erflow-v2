package com.erflow.calendar;

/**
 * 일정 한 건. {@code calendar_view} 한 행.
 *
 * <p>이 이름들이 그대로 JSON 이 되어 화면으로 나간다 — {@code js/main/calendar.js} 의
 * {@code mapEvent} 가 {@code subject}·{@code start}·{@code end}·{@code type}·
 * {@code content}·{@code id}·{@code userId} 를 읽는다. 바꾸면 달력이 빈다.
 *
 * @param id 일정 번호
 * @param userId 만든 사람 사번
 * @param deptId 부서 번호
 * @param deptName 부서명
 * @param subject 제목
 * @param content 내용
 * @param start 시작 시각
 * @param end 종료 시각
 * @param type 공개 범위. 0 개인 / 1 부서 / 2 전체
 */
public record CalendarEvent(
        int id,
        String userId,
        int deptId,
        String deptName,
        String subject,
        String content,
        String start,
        String end,
        int type) {
}
