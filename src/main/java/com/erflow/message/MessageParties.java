package com.erflow.message;

/**
 * 쪽지 한 건의 보낸/받는 사람 사번. 소프트 삭제가 «어느 쪽을 숨길지» 정하는 데 쓴다.
 *
 * @param senderId 보낸 사람 사번({@code user_tbl_sender_id})
 * @param receiverId 받는 사람 사번({@code user_tbl_receiver_id})
 */
public record MessageParties(String senderId, String receiverId) {
}
