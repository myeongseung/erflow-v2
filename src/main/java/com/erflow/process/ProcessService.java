package com.erflow.process;

import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공정 업무.
 *
 * <p>공정은 앞뒤로 이어진 사슬이다. 등록 화면이 표에 쌓은 순서가 곧 그 순서이며,
 * 서버가 그 순서대로 이전·다음 고리를 채운다.
 */
@Service
public class ProcessService {

    private final ProcessMapper processMapper;

    /**
     * @param processMapper 공정 매퍼
     */
    public ProcessService(ProcessMapper processMapper) {
        this.processMapper = processMapper;
    }

    /**
     * 공정 목록 한 페이지.
     *
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public ProcessPage list(ProcessSearch search, int requestedPage) {
        Pagination pagination = Pagination.of(processMapper.countBy(search), requestedPage);
        return new ProcessPage(
                processMapper.findPage(search, pagination.start(), pagination.numPerPage()),
                pagination);
    }

    /**
     * 공정들을 한 사슬로 만든다.
     *
     * <p>받은 순서가 곧 우선순위다. 첫 공정은 다음만, 마지막 공정은 이전만, 가운데는
     * 둘 다 갖는다.
     *
     * <p><b>공정이 하나면 만들지 못한다.</b> 레거시가 «첫 공정» 이면 무조건 다음 공정을
     * 찾는데, 하나뿐이면 다음이 없어 그 자리에서 죽는다(D-072). 죽음은 옮기지 않고
     * 실패로 돌려준다.
     *
     * <p>레거시는 한 건씩 따로 커밋하고 <b>마지막 결과만</b> 돌려줬다. 중간에 끊기면
     * 고리가 끊어진 공정이 남으므로 한 트랜잭션으로 묶는다(D-043 과 같은 판단).
     *
     * @param steps 공정ID·공정명 짝. 화면 표의 순서 그대로다
     * @return 전부 만들었으면 {@code true}
     */
    @Transactional
    public boolean createChain(List<ProcessStep> steps) {
        if (steps == null || steps.size() < 2) {
            return false;
        }
        boolean result = true;
        for (int index = 0; index < steps.size(); ++index) {
            ProcessStep step = steps.get(index);
            String prevId = index == 0 ? null : steps.get(index - 1).processId();
            String nextId = index == steps.size() - 1 ? null : steps.get(index + 1).processId();
            // 우선순위는 1부터다 — 레거시가 머리글 행을 건너뛴 자리 번호를 그대로 쓴다.
            result &= processMapper.insertProcess(new ProcessRow(
                    step.processId(), prevId, nextId, step.processName(), index + 1)) == 1;
        }
        return result;
    }

    /**
     * 공정명을 고친다.
     *
     * @param id 공정ID
     * @param name 새 이름
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean rename(String id, String name) {
        if (processMapper.findById(id) == null) {
            return false;
        }
        return processMapper.updateName(id, name) == 1;
    }

    /**
     * 공정들을 지운다.
     *
     * <p>저장 프로시저 {@code DeleteProcess} 를 부른다. 그 프로시저가 없으면 예외가 나고
     * 레거시와 같이 실패로 돌아간다 — 레거시도 예외를 삼키고 false 를 돌려줬다(D-073).
     *
     * @param ids 지울 공정ID 들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean delete(List<String> ids) {
        boolean result = true;
        for (String id : ids) {
            try {
                result &= processMapper.callDeleteProcess(id) == 1;
            } catch (DataAccessException expected) {
                result = false;
            }
        }
        return result;
    }

    /**
     * 등록 화면이 보내는 공정 한 줄.
     *
     * @param processId 공정ID
     * @param processName 공정명
     */
    public record ProcessStep(String processId, String processName) {
    }

    /**
     * 공정 목록 한 페이지.
     *
     * @param rows 공정 줄
     * @param pagination 페이징 정보
     */
    public record ProcessPage(List<ProcessRow> rows, Pagination pagination) {
    }
}
