package com.erflow.task;

import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수·발주 업무.
 *
 * <p>레거시는 페이징 계산과 파라미터 정리를 {@code purchaseTask.jsp}/{@code sellTask.jsp}
 * 스크립틀릿에, DB 접근을 {@code TaskServiceImpl} 에 두었다. 화면에서 로직을 걷어내
 * 여기로 모은다.
 */
@Service
public class TaskService {

    /** 수주. 레거시 {@code type} 0. */
    public static final int SELL = 0;

    /** 발주. 레거시 {@code type} 1. */
    public static final int PURCHASE = 1;

    private final TaskMapper taskMapper;

    /**
     * @param taskMapper 수·발주 매퍼
     */
    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /**
     * 목록 한 페이지와 페이징 정보를 함께 돌려준다.
     *
     * @param type 0 수주 / 1 발주
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public TaskPage list(int type, TaskSearch search, int requestedPage) {
        // 레거시는 status 검색어가 정수가 아니면 예외를 삼켜 빈 목록을 냈다. 매퍼까지
        // 가기 전에 걸러 같은 결과(빈 목록)를 낸다 — TaskSearch.isBrokenStatus 참조.
        if (search != null && search.isBrokenStatus()) {
            return new TaskPage(List.of(), Pagination.of(0, requestedPage));
        }
        int total = taskMapper.countBy(type, search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<TaskRow> rows =
                taskMapper.findPage(type, search, pagination.start(), pagination.numPerPage());
        return new TaskPage(rows, pagination);
    }

    /**
     * 수·발주를 이력과 함께 등록한다.
     *
     * <p>레거시 {@code createTask} 를 옮겼다 — 본체를 넣어 생성된 id 를 받고, 그 id 로
     * 수량이 <b>0 보다 큰</b> 이력만 잇는다. 수량 0 은 넣지 않는다.
     *
     * <h2>레거시와 달라지는 것 — 원자성</h2>
     *
     * <p>레거시는 트랜잭션이 없었다. 본체가 들어가고 이력 하나가 실패해도 본체는 남았고,
     * 예외를 삼켜 «성공»으로 알렸다(D-043). 여기서는 한 트랜잭션으로 묶어 이력이 실패하면
     * 본체까지 되돌린다. 정상 데이터에서는 결과가 같다.
     *
     * @param task 등록할 수·발주 본체
     * @param histories 제품·수량 목록
     * @return 본체가 들어갔으면 {@code true}
     */
    @Transactional
    public boolean create(Task task, List<TaskHistory> histories) {
        boolean created = taskMapper.insertTask(task) == 1;
        if (created && histories != null) {
            int taskId = taskMapper.lastInsertId();
            for (TaskHistory history : histories) {
                if (history.count() > 0) {
                    taskMapper.insertHistory(
                            new TaskHistory(taskId, history.productId(), history.count()));
                }
            }
        }
        return created;
    }

    /**
     * 한 수·발주의 제품 이력. 내역 모달과 수정 화면이 쓴다.
     *
     * @param taskId 수·발주 번호
     * @return 제품·수량 목록
     */
    @Transactional(readOnly = true)
    public List<TaskHistoryRow> histories(int taskId) {
        return taskMapper.findHistories(taskId);
    }

    /**
     * 수·발주 한 건을 읽는다. 수정 화면이 쓴다.
     *
     * @param id 의뢰 번호
     * @return 수·발주. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public TaskDetail get(int id) {
        return taskMapper.findById(id);
    }

    /**
     * 수·발주를 수정하고 이력을 갈아 끼운다.
     *
     * <p>레거시 {@code taskUpdateProc} 를 옮겼다 — 본체를 고치고, 이력을 <b>전부 지운
     * 뒤 제출된 제품을 다시 넣는다.</b> 등록과 달리 수량 0 도 넣는다(레거시가 여기선
     * {@code if count > 0} 을 걸지 않는다).
     *
     * <p>레거시는 본체 수정 결과와 무관하게 이력을 갈아 끼워, 발주 수정이 «실패» 라고
     * 뜨는데도 이력은 바뀌어 있었다(D-044). 2단계에서 정리했다(D-106) — 본체가
     * 수정됐을 때만 이력을 간다. 실패한 수정은 아무것도 바꾸지 않는다.
     *
     * @param task 수정 값
     * @param histories 제품·수량 목록(전량 교체)
     * @return 본체가 수정됐으면 {@code true}
     */
    @Transactional
    public boolean update(TaskUpdate task, List<TaskHistory> histories) {
        boolean updated = taskMapper.updateTask(task) == 1;
        if (!updated) {
            return false;
        }
        taskMapper.deleteHistories(task.id());
        if (histories != null) {
            for (TaskHistory history : histories) {
                taskMapper.insertHistory(
                        new TaskHistory(task.id(), history.productId(), history.count()));
            }
        }
        return true;
    }

    /**
     * 선택된 수·발주를 지운다.
     *
     * <p>레거시는 한 건씩 지우며 결과를 {@code &=} 로 모았다. type 이 맞아야 지워지므로
     * 발주 목록에서 고른 것은 발주만 지워진다. 이력은 본체보다 먼저 지운다.
     *
     * @param ids 지울 의뢰 번호 목록
     * @param type 0 수주 / 1 발주
     * @return 전부 지워졌으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Integer> ids, int type) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean all = true;
        for (int id : ids) {
            taskMapper.deleteHistories(id);
            all &= taskMapper.deleteTask(id, type) == 1;
        }
        return all;
    }

    /**
     * 목록 한 페이지.
     *
     * @param rows 이 페이지의 수·발주 목록
     * @param pagination 페이징 정보
     */
    public record TaskPage(List<TaskRow> rows, Pagination pagination) {
    }
}
