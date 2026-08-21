package com.erflow.admin.document;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 문서 양식 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/admin/AdminTemplateMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 */
@Mapper
public interface AdminTemplateMapper {

    /**
     * 문서 양식 전체.
     *
     * @return 양식 목록. DB 가 주는 순서 그대로다
     */
    List<TemplateRow> findAll();

    /**
     * 문서 양식 하나.
     *
     * @param id 문서 번호
     * @return 양식. 없으면 {@code null}
     */
    TemplateRow findById(@Param("id") int id);

    /**
     * 문서 양식을 넣는다.
     *
     * @param subject 양식명
     * @param content 양식 내용(HTML)
     * @return 반영된 행 수
     */
    int insertTemplate(@Param("subject") String subject, @Param("content") String content);

    /**
     * 문서 양식을 고친다.
     *
     * @param id 문서 번호
     * @param subject 양식명
     * @param content 양식 내용(HTML)
     * @return 반영된 행 수
     */
    int updateTemplate(
            @Param("id") int id,
            @Param("subject") String subject,
            @Param("content") String content);

    /**
     * 문서 양식을 지운다.
     *
     * @param id 문서 번호
     * @return 반영된 행 수
     */
    int deleteTemplate(@Param("id") int id);
}
