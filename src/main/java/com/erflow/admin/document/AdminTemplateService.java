package com.erflow.admin.document;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 양식 업무.
 *
 * <p>레거시는 요청 파라미터를 읽는 일까지 {@code TemplateServiceImpl} 이 했다. 그 부분은
 * 컨트롤러로 올리고 여기에는 업무만 남긴다.
 */
@Service
public class AdminTemplateService {

    private final AdminTemplateMapper mapper;

    /**
     * @param mapper 문서 양식 매퍼
     */
    public AdminTemplateService(AdminTemplateMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 문서 양식 전체.
     *
     * @return 양식 목록
     */
    @Transactional(readOnly = true)
    public List<TemplateRow> list() {
        return mapper.findAll();
    }

    /**
     * 문서 양식 하나.
     *
     * @param id 문서 번호
     * @return 양식. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public TemplateRow find(int id) {
        return mapper.findById(id);
    }

    /**
     * 문서 양식을 만든다.
     *
     * @param subject 양식명
     * @param content 양식 내용(HTML)
     * @return 만들었으면 {@code true}
     */
    @Transactional
    public boolean create(String subject, String content) {
        return mapper.insertTemplate(subject, content) == 1;
    }

    /**
     * 문서 양식을 고친다.
     *
     * <p>레거시는 고치기 전에 그 양식을 한 번 읽는다. 읽은 값을 쓰지는 않지만, 없는
     * 번호면 그 자리에서 {@code NullPointerException} 으로 죽는다. 여기서는 없으면
     * 고치지 않고 실패로 돌려준다.
     *
     * @param id 문서 번호
     * @param subject 양식명
     * @param content 양식 내용(HTML)
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean update(int id, String subject, String content) {
        if (mapper.findById(id) == null) {
            return false;
        }
        return mapper.updateTemplate(id, subject, content) == 1;
    }

    /**
     * 문서 양식들을 지운다.
     *
     * @param ids 지울 문서 번호들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Integer> ids) {
        boolean result = true;
        for (int id : ids) {
            result &= mapper.deleteTemplate(id) == 1;
        }
        return result;
    }
}
