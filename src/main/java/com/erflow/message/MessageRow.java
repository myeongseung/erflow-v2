package com.erflow.message;

/**
 * 쪽지함 목록 한 줄. {@code message_view} 한 행이다.
 *
 * <p>받은쪽지함이면 보낸 사람을, 보낸쪽지함이면 받는 사람을 «[부서] 이름 직급» 으로
 * 보여준다. 어느 쪽을 쓸지는 화면(class)이 정하므로 양쪽 정보를 다 담는다.
 *
 * @param id 쪽지 번호
 * @param senderId 보낸 사람 사번. 답장 대상이라 행에 숨겨 둔다
 * @param senderName 보낸 사람 이름
 * @param senderDeptName 보낸 사람 부서
 * @param senderJobName 보낸 사람 직급
 * @param receiverName 받는 사람 이름
 * @param receiverDeptName 받는 사람 부서
 * @param receiverJobName 받는 사람 직급
 * @param content 내용
 * @param createdAt 보낸 시각
 */
public record MessageRow(
        int id,
        String senderId,
        String senderName,
        String senderDeptName,
        String senderJobName,
        String receiverName,
        String receiverDeptName,
        String receiverJobName,
        String content,
        String createdAt) {

    /**
     * 받은쪽지함에서 보여줄 상대(보낸 사람) 표기.
     *
     * @return {@code [부서] 이름 직급}
     */
    public String senderLabel() {
        return "[" + senderDeptName + "] " + senderName + " " + senderJobName;
    }

    /**
     * 보낸쪽지함에서 보여줄 상대(받는 사람) 표기.
     *
     * @return {@code [부서] 이름 직급}
     */
    public String receiverLabel() {
        return "[" + receiverDeptName + "] " + receiverName + " " + receiverJobName;
    }
}
