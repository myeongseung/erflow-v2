package com.erflow.document;

/**
 * 문서 한 건. 작성 화면이 고칠 때 채워 넣는 값이다.
 *
 * @param id 문서번호
 * @param userId 작성자 사번
 * @param subject 제목
 * @param content 본문. HTML 이다
 */
public record DocumentDetail(long id, String userId, String subject, String content) {
}
