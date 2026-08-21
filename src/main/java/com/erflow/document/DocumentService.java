package com.erflow.document;

import com.erflow.admin.document.AdminTemplateMapper;
import com.erflow.admin.document.TemplateRow;
import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 업무.
 *
 * <p>문서는 결재에 올릴 실제 서식이다. 관리자가 만든 <b>문서 양식</b>({@code template_tbl})
 * 을 골라 그 내용을 편집기에 부어 놓고 쓴다 — 그래서 양식 목록을 관리자 쪽 매퍼에서
 * 그대로 읽는다. 레거시도 같은 조회({@code getTemplates})를 두 화면이 함께 썼다.
 */
@Service
public class DocumentService {

    /** 빈 문서를 뜻하는 양식 번호. 화면의 첫 항목이 이 값이다. */
    public static final int BLANK_TEMPLATE = 0;

    private final DocumentMapper documentMapper;

    private final AdminTemplateMapper templateMapper;

    /**
     * @param documentMapper 문서 매퍼
     * @param templateMapper 문서 양식 매퍼. 양식 목록만 읽는다
     */
    public DocumentService(DocumentMapper documentMapper, AdminTemplateMapper templateMapper) {
        this.documentMapper = documentMapper;
        this.templateMapper = templateMapper;
    }

    /**
     * 내 문서 목록 한 페이지.
     *
     * @param search 검색 조건
     * @param userId 작성자 사번
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public DocumentPage list(DocumentSearch search, String userId, int requestedPage) {
        Pagination pagination =
                Pagination.of(documentMapper.countBy(search, userId), requestedPage);
        return new DocumentPage(
                documentMapper.findPage(
                        search, userId, pagination.start(), pagination.numPerPage()),
                pagination);
    }

    /**
     * 문서 한 건.
     *
     * @param id 문서번호
     * @return 문서. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public DocumentDetail get(long id) {
        return documentMapper.findDetail(id);
    }

    /**
     * 고를 수 있는 문서 양식.
     *
     * @return 양식 목록
     */
    @Transactional(readOnly = true)
    public List<TemplateRow> templates() {
        return templateMapper.findAll();
    }

    /**
     * 양식 하나의 내용. 양식을 고르면 그 내용이 편집기에 부어진다.
     *
     * @param templateId 양식 번호
     * @return 양식 내용. 빈 문서이거나 없는 양식이면 빈 문자열
     */
    @Transactional(readOnly = true)
    public String templateContent(int templateId) {
        if (templateId == BLANK_TEMPLATE) {
            return "";
        }
        TemplateRow template = templateMapper.findById(templateId);
        return template == null ? "" : template.content();
    }

    /**
     * 문서를 만든다.
     *
     * @param userId 작성자 사번
     * @param templateId 양식 번호
     * @param subject 제목
     * @param content 본문(HTML)
     * @return 만들었으면 {@code true}
     */
    @Transactional
    public boolean create(String userId, int templateId, String subject, String content) {
        return documentMapper.insertDocument(userId, templateId, subject, content) == 1;
    }

    /**
     * 문서를 고친다.
     *
     * <p><b>이미 결재에 올라간 문서는 고치지 않고 새로 만든다.</b> 결재가 걸린 문서를
     * 고치면 결재선이 본 내용과 달라지기 때문이다 — 레거시가 그렇게 갈랐고 화면에도
     * «이미 결재 진행된 문서이므로, 문서를 새로 등록합니다» 라고 알린다.
     *
     * @param id 문서번호
     * @param userId 작성자 사번. 새로 만들 때 쓴다
     * @param templateId 양식 번호
     * @param subject 제목
     * @param content 본문(HTML)
     * @return 무엇을 했는지
     */
    @Transactional
    public UpdateResult update(
            long id, String userId, int templateId, String subject, String content) {

        if (documentMapper.countProposals(id) > 0) {
            return create(userId, templateId, subject, content)
                    ? UpdateResult.RENEWED : UpdateResult.FAILED;
        }
        return documentMapper.updateDocument(id, templateId, subject, content) == 1
                ? UpdateResult.UPDATED : UpdateResult.FAILED;
    }

    /**
     * 문서들을 지운다.
     *
     * <p><b>결재에 올라간 문서는 지우지 않는다.</b> 하나라도 걸리면 화면이 «결재 진행 중인
     * 항목 외의 문서를 삭제하였습니다» 라고 알린다 — 나머지는 지워진 뒤다.
     *
     * @param ids 지울 문서번호들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Long> ids) {
        boolean result = true;
        for (long id : ids) {
            result &= documentMapper.countProposals(id) == 0
                    && documentMapper.deleteDocument(id) == 1;
        }
        return result;
    }

    /** 수정 요청이 무엇으로 끝났는지. */
    public enum UpdateResult {

        /** 그 문서를 고쳤다. */
        UPDATED,

        /** 결재에 올라간 문서라 새로 만들었다. */
        RENEWED,

        /** 아무것도 하지 못했다. */
        FAILED
    }

    /**
     * 문서 목록 한 페이지.
     *
     * @param rows 문서 줄
     * @param pagination 페이징 정보
     */
    public record DocumentPage(List<DocumentListRow> rows, Pagination pagination) {
    }
}
