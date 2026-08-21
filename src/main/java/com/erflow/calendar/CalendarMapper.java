package com.erflow.calendar;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 일정 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/calendar/CalendarMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 */
@Mapper
public interface CalendarMapper {

    /**
     * 그 사람이 볼 수 있는 일정 전부.
     *
     * @param userId 사번
     * @return 일정 목록
     */
    List<CalendarEvent> findVisible(@Param("userId") String userId);

    /**
     * 일정을 넣는다.
     *
     * @param event 새 일정. {@code id} 는 보지 않는다
     * @return 반영된 행 수
     */
    int insertEvent(@Param("event") CalendarEvent event);

    /**
     * 일정을 고친다. 자기 것만 고쳐진다.
     *
     * <p>레거시는 주인을 확인하지 않아 번호만 맞으면 남의 일정도 고쳐졌다(D-083).
     * 2단계에서 지우기와 같은 모양으로 주인 조건을 붙였다(D-101).
     *
     * @param event 고칠 일정. {@code userId} 가 주인과 다르면 아무 일도 없다
     * @return 반영된 행 수
     */
    int updateEvent(@Param("event") CalendarEvent event);

    /**
     * 일정을 지운다. 지우는 쪽은 주인까지 본다.
     *
     * @param id 일정 번호
     * @param userId 사번
     * @return 반영된 행 수
     */
    int deleteEvent(@Param("id") int id, @Param("userId") String userId);

}
