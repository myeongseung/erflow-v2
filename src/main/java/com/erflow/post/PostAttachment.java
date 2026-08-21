package com.erflow.post;

/**
 * 게시글 첨부파일 한 건. {@code post_file_tbl} 에 대응한다.
 *
 * <p>저장 이름({@code name})은 UUID 이고 확장자가 붙지 않는다. 원본 이름과 확장자는
 * 따로 보관한다 — 레거시가 그렇게 넣는다.
 *
 * @param id 첨부 번호
 * @param postId 글번호
 * @param originalName 원본 파일명 (확장자 제외)
 * @param name 저장 파일명 (UUID)
 * @param extension 확장자
 * @param size 바이트 크기
 */
public record PostAttachment(
        int id,
        int postId,
        String originalName,
        String name,
        String extension,
        long size) {

    /**
     * 화면에 뿌리는 이름.
     *
     * @return 레거시 {@code filename + "." + extension} 과 같은 문자열
     */
    public String displayName() {
        return originalName + "." + extension;
    }
}
