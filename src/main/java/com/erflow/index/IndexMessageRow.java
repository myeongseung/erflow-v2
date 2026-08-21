package com.erflow.index;

/**
 * 메인 화면의 «받은 쪽지함» 위젯 한 줄.
 *
 * @param number 왼쪽 끝 번호
 * @param id 쪽지 번호
 * @param sender 보낸이 표기. {@code [부서] 이름 직급}
 * @param content 화면에 찍히는 내용. 열 글자에서 잘려 있다
 * @param createdAt 수신 시각
 */
public record IndexMessageRow(
        int number, int id, String sender, String content, String createdAt) {
}
