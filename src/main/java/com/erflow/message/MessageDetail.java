package com.erflow.message;

/**
 * 쪽지 읽기 화면이 보여주는 한 건. {@code message_view} 한 행이다.
 *
 * @param id 쪽지 번호
 * @param senderId 보낸 사람 사번. 답장 대상이라 화면에 숨겨 둔다
 * @param senderName 보낸 사람 이름
 * @param senderDeptName 보낸 사람 부서
 * @param senderJobName 보낸 사람 직급
 * @param content 내용
 * @param createdAt 받은 시각
 */
public record MessageDetail(
        int id,
        String senderId,
        String senderName,
        String senderDeptName,
        String senderJobName,
        String content,
        String createdAt) {

    /**
     * 보낸 사람 표기.
     *
     * @return {@code [부서] 이름 직급}
     */
    public String senderLabel() {
        return "[" + senderDeptName + "] " + senderName + " " + senderJobName;
    }
}
