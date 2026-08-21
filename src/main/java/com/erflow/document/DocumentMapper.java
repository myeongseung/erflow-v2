package com.erflow.document;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 문서 매퍼. 지금은 찾기 팝업이 쓰는 것만 있다. */
@Mapper
public interface DocumentMapper {

    /**
     * 사용자가 만든 문서 목록.
     *
     * @param userId 사번
     * @return 문서 목록. 번호 내림차순
     */
    List<DocumentRow> findByUser(@Param("userId") String userId);

    /**
     * 문서에 걸린 결재 건수.
     *
     * @param documentId 문서번호
     * @return 건수
     */
    int countProposals(@Param("documentId") long documentId);

    /**
     * 내 문서 목록 한 페이지.
     *
     * @param search 검색 조건
     * @param userId 작성자 사번
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 문서 목록. 문서번호 내림차순
     */
    List<DocumentListRow> findPage(
            @Param("search") DocumentSearch search,
            @Param("userId") String userId,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 내 문서 수.
     *
     * @param search 검색 조건
     * @param userId 작성자 사번
     * @return 건수
     */
    int countBy(@Param("search") DocumentSearch search, @Param("userId") String userId);

    /**
     * 문서 한 건.
     *
     * @param id 문서번호
     * @return 문서. 없으면 {@code null}
     */
    DocumentDetail findDetail(@Param("id") long id);

    /**
     * 문서를 넣는다.
     *
     * @param userId 작성자 사번
     * @param templateId 양식 번호. 빈 문서면 0
     * @param subject 제목
     * @param content 본문(HTML)
     * @return 반영된 행 수
     */
    int insertDocument(
            @Param("userId") String userId,
            @Param("templateId") int templateId,
            @Param("subject") String subject,
            @Param("content") String content);

    /**
     * 문서를 고친다.
     *
     * @param id 문서번호
     * @param templateId 양식 번호
     * @param subject 제목
     * @param content 본문(HTML)
     * @return 반영된 행 수
     */
    int updateDocument(
            @Param("id") long id,
            @Param("templateId") int templateId,
            @Param("subject") String subject,
            @Param("content") String content);

    /**
     * 문서를 지운다.
     *
     * @param id 문서번호
     * @return 반영된 행 수
     */
    int deleteDocument(@Param("id") long id);
}
