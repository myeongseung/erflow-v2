package com.erflow.message;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 쪽지 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/message/MessageMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 *
 * <p>{@code className} 은 receiver 또는 sender 다 — 화면에서 그 둘로만 검증된다. SQL 이
 * 이 값으로 {@code <class>_id}/{@code <class>_visible} 컬럼을 고르는데, 문자열로 이어
 * 붙이지 않고 분기로 고정해 주입을 막는다.
 */
@Mapper
public interface MessageMapper {

    /**
     * 쪽지함 한 페이지.
     *
     * @param className receiver 또는 sender
     * @param classId 현재 사용자 사번
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 쪽지 목록
     */
    List<MessageRow> findPage(
            @Param("className") String className,
            @Param("classId") String classId,
            @Param("search") MessageSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 검색 조건에 걸리는 전체 건수.
     *
     * @param className receiver 또는 sender
     * @param classId 현재 사용자 사번
     * @param search 검색 조건
     * @return 전체 건수
     */
    int countBy(
            @Param("className") String className,
            @Param("classId") String classId,
            @Param("search") MessageSearch search);

    /**
     * 쪽지 한 건을 읽기 화면용으로 조회한다.
     *
     * @param id 쪽지 번호
     * @return 쪽지. 없으면 {@code null}
     */
    MessageDetail findView(@Param("id") int id);

    /**
     * 쪽지를 «읽음» 으로 표시한다.
     *
     * <p>레거시는 읽지 않은 것만 갱신했다({@code read_status == 0} 일 때만). 여기서는
     * {@code WHERE ... AND read_status = 0} 으로 같은 결과를 낸다 — 이미 읽은 것은
     * {@code read_at} 이 바뀌지 않는다.
     *
     * @param id 쪽지 번호
     * @return 갱신된 행 수(이미 읽었으면 0)
     */
    int markRead(@Param("id") int id);

    /**
     * 쪽지 한 건을 넣는다.
     *
     * @param senderId 보낸 사람 사번
     * @param receiverId 받는 사람 사번
     * @param content 내용
     * @return 반영된 행 수
     */
    int insertMessage(
            @Param("senderId") String senderId,
            @Param("receiverId") String receiverId,
            @Param("content") String content);

    /**
     * 쪽지의 보낸/받는 사람. 소프트 삭제가 어느 쪽을 숨길지 정하는 데 쓴다.
     *
     * @param id 쪽지 번호
     * @return 보낸/받는 사람. 없으면 {@code null}
     */
    MessageParties findParties(@Param("id") int id);

    /**
     * 쪽지를 한 쪽(또는 양쪽)에서 안 보이게 한다(소프트 삭제).
     *
     * <p>레거시는 사용자가 보낸 사람이면 {@code sender_visible = 0}, 받는 사람이면
     * {@code receiver_visible = 0}, 둘 다면 둘 다 내렸다. 지우지 않고 숨긴다.
     *
     * @param id 쪽지 번호
     * @param hideSender 보낸 쪽에서 숨길지
     * @param hideReceiver 받는 쪽에서 숨길지
     * @return 반영된 행 수
     */
    int hide(
            @Param("id") int id,
            @Param("hideSender") boolean hideSender,
            @Param("hideReceiver") boolean hideReceiver);
}
