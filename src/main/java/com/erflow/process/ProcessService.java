package com.erflow.process;

import com.erflow.common.Pagination;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * 공정들을 지운다 — 고리를 다시 잇고 자리 번호를 당긴다 (D-133, O-013 해소).
     *
     * <p>레거시는 저장 프로시저 {@code DeleteProcess} 를 불렀는데 그 정의를 볼 수
     * 없었고(D-073), 이 스키마에는 프로시저가 없어 삭제가 영영 실패했다. 이제 우리가
     * 만든다 — 등록({@link #createChain})의 대칭이다.
     *
     * <ul>
     *   <li>이전 공정의 다음 고리가 지워지는 공정의 다음을 가리키게 잇는다. 반대쪽도
     *       같다. 사슬의 끝을 지우면 그 자리는 {@code null} 이 된다</li>
     *   <li>지워지는 공정 <b>뒤</b>의 공정들은 자리 번호를 한 칸씩 당긴다 — 사슬을
     *       따라가며 당기므로 다른 사슬은 건드리지 않는다</li>
     *   <li>관리·생산 기록이 참조하는 공정은 지우지 않는다 — 지우면 그 기록들이 없는
     *       공정을 가리키게 된다(첨부 파일의 O-009 와 같은 종류의 고아)</li>
     * </ul>
     *
     * <p><b>전부 지우거나 하나도 지우지 않는다.</b> 먼저 전부 검사하고 나서 지운다 —
     * 화면은 «지웠다/못 지웠다» 만 말할 수 있으므로, 절반만 지워 놓고 «못 지웠다» 고
     * 말하는 상태를 만들지 않는다.
     *
     * @param ids 지울 공정ID 들
     * @return 전부 지웠으면 {@code true}. 하나라도 못 지우면 아무것도 지우지 않고
     *     {@code false}
     */
    @Transactional
    public boolean delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (String id : ids) {
            if (processMapper.findById(id) == null || processMapper.countReferences(id) > 0) {
                return false;
            }
        }
        boolean result = true;
        for (String id : ids) {
            result &= unlinkAndDelete(id);
        }
        return result;
    }

    private boolean unlinkAndDelete(String id) {
        ProcessRow row = processMapper.findById(id);
        if (row == null) {
            // 같은 ID 를 두 번 보냈을 때 — 앞 순회에서 이미 지워졌다.
            return false;
        }
        if (row.prevId() != null) {
            processMapper.updateNextOf(row.prevId(), row.nextId());
        }
        if (row.nextId() != null) {
            processMapper.updatePrevOf(row.nextId(), row.prevId());
        }
        pullForward(row.nextId());
        return processMapper.deleteById(id) == 1;
    }

    /**
     * 지워진 자리 뒤의 공정들을 사슬을 따라가며 한 칸씩 당긴다.
     *
     * <p>{@code priority > n 인 전부} 로 당기면 안 된다 — 자리 번호는 사슬마다 1부터
     * 매겨지므로 다른 사슬까지 밀린다. 끊긴 고리나 고리 순환을 만나면 멈춘다.
     *
     * @param startId 지워진 공정의 다음 공정ID. {@code null} 이면 할 일이 없다
     */
    private void pullForward(String startId) {
        Set<String> visited = new HashSet<>();
        for (String id = startId; id != null && visited.add(id); ) {
            ProcessRow row = processMapper.findById(id);
            if (row == null) {
                return;
            }
            processMapper.decrementPriority(id);
            id = row.nextId();
        }
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
