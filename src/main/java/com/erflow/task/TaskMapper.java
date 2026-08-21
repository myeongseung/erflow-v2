package com.erflow.task;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 수·발주 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/task/TaskMapper.xml} 에 있고, 각 구문에 레거시
 * 출처를 주석으로 달아 뒀다.
 *
 * <p>{@code type} 으로 발주(1)와 수주(0)가 갈린다. 목록·카운트 모두 같은 {@code type}
 * 을 받는다 — 게시판에서 목록과 카운트가 서로 다른 파라미터를 봐서 생긴 결함(D-025)이
 * 여기엔 없다. 레거시가 인덱스를 맞춰 두었다.
 */
@Mapper
public interface TaskMapper {

    /**
     * 목록을 페이지 단위로 조회한다.
     *
     * @param type 0 수주 / 1 발주
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 수·발주 목록
     */
    List<TaskRow> findPage(
            @Param("type") int type,
            @Param("search") TaskSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 검색 조건에 걸리는 전체 건수를 센다.
     *
     * @param type 0 수주 / 1 발주
     * @param search 검색 조건
     * @return 전체 건수
     */
    int countBy(@Param("type") int type, @Param("search") TaskSearch search);

    /**
     * 수·발주 한 건을 넣는다. id·created_at 은 DB 가 채운다.
     *
     * @param task 등록할 수·발주
     * @return 반영된 행 수
     */
    int insertTask(@Param("task") Task task);

    /**
     * 방금 넣은 행의 자동 생성 id.
     *
     * <p>{@code createTask} 가 생성 키를 읽어 이력을 잇던 것을 대신한다. 같은 트랜잭션
     * (같은 커넥션) 안에서 불러야 방금 넣은 값을 돌려준다.
     *
     * @return {@code LAST_INSERT_ID()}
     */
    int lastInsertId();

    /**
     * 이력 한 줄을 넣는다.
     *
     * @param history 제품·수량
     * @return 반영된 행 수
     */
    int insertHistory(@Param("history") TaskHistory history);

    /**
     * 한 수·발주의 제품 이력.
     *
     * @param taskId 수·발주 번호
     * @return 제품 코드·이름·수량 목록
     */
    List<TaskHistoryRow> findHistories(@Param("taskId") int taskId);

    /**
     * 수·발주 한 건을 읽는다. 수정 화면이 쓴다.
     *
     * @param id 의뢰 번호
     * @return 수·발주. 없으면 {@code null}
     */
    TaskDetail findById(@Param("id") int id);

    /**
     * 본체를 수정한다.
     *
     * <p>{@code WHERE id = ? AND type = ?} 다 — 레거시 그대로. type 이 언제나 0 이라
     * 발주 수정은 걸리는 행이 없다(D-044).
     *
     * @param task 수정 값
     * @return 반영된 행 수
     */
    int updateTask(@Param("task") TaskUpdate task);

    /**
     * 한 수·발주의 이력을 모두 지운다.
     *
     * @param taskId 수·발주 번호
     * @return 지워진 행 수
     */
    int deleteHistories(@Param("taskId") int taskId);

    /**
     * 수·발주 한 건을 지운다. type 이 맞아야 지워진다.
     *
     * @param id 의뢰 번호
     * @param type 0 수주 / 1 발주
     * @return 반영된 행 수
     */
    int deleteTask(@Param("id") int id, @Param("type") int type);
}
