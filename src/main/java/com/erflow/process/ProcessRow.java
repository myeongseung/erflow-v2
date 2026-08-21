package com.erflow.process;

/**
 * 공정 한 줄. {@code process_tbl} 한 행이다.
 *
 * <p>공정은 앞뒤로 이어진 사슬이다 — 저마다 이전·다음 공정을 들고 있고 {@code priority}
 * 가 순서를 매긴다. 등록 화면이 표에 쌓은 순서대로 그 고리를 만든다.
 *
 * @param id 공정ID
 * @param prevId 이전 공정ID. 첫 공정이면 {@code null}
 * @param nextId 다음 공정ID. 마지막 공정이면 {@code null}
 * @param name 공정명
 * @param priority 우선순위
 */
public record ProcessRow(String id, String prevId, String nextId, String name, int priority) {
}
