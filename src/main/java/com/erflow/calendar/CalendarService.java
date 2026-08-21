package com.erflow.calendar;

import com.erflow.auth.AuthMapper;
import com.erflow.auth.ErflowUserDetails;
import com.erflow.auth.Permissions;
import com.erflow.auth.ScreenAccess;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일정 업무. 레거시 {@code controller/CalendarController} 가 하던 일이다.
 *
 * <h2>공개 범위마다 물어보는 것이 다르다</h2>
 *
 * <pre>
 * 0 개인일정   누구나
 * 1 부서일정   프로그램 권한 «부서 일정 등록»
 * 2 전체일정   관리자
 * </pre>
 *
 * <p>이 판정은 <b>만들 때와 고칠 때만</b> 한다. 보는 것은 조회 조건이 대신하고,
 * 지우는 것은 주인인지만 본다.
 */
@Service
public class CalendarService {

    /**
     * «부서 일정 등록» 프로그램.
     *
     * <p>레거시가 {@code CalendarController} 안에 상수로 박아 둔 값이다. 화면이 아니라
     * 코드에 있어 {@code screen} 표로는 닿지 않는다. 미결 O-001 이 «어느 화면에서도
     * 참조되지 않는다» 고 적어 둔 그 프로그램이며, 쓰는 자리가 여기다.
     */
    static final String DEPT_CALENDAR_PROGRAM =
            "822D0CAB0FFF374AAE6D095F0DF8612A3D2C14AB72B4AC1774D5DFD5FB5D29D0";

    private final CalendarMapper calendarMapper;
    private final AuthMapper authMapper;

    /**
     * @param calendarMapper 일정 조회·수정
     * @param authMapper 프로그램 권한 조회
     */
    public CalendarService(CalendarMapper calendarMapper, AuthMapper authMapper) {
        this.calendarMapper = calendarMapper;
        this.authMapper = authMapper;
    }

    /**
     * 그 사람이 볼 수 있는 일정.
     *
     * @param userId 사번
     * @return 일정 목록
     */
    @Transactional(readOnly = true)
    public List<CalendarEvent> visible(String userId) {
        return calendarMapper.findVisible(userId);
    }

    /**
     * 일정을 만든다.
     *
     * @param user 로그인 사용자
     * @param event 새 일정
     * @return 만들어졌으면 {@code true}
     */
    @Transactional
    public boolean create(ErflowUserDetails user, CalendarEvent event) {
        return allowed(user, event.type()) && calendarMapper.insertEvent(event) == 1;
    }

    /**
     * 일정을 고친다. 자기 것만 고쳐진다(D-101).
     *
     * @param user 로그인 사용자
     * @param event 고칠 일정
     * @return 고쳐졌으면 {@code true}
     */
    @Transactional
    public boolean update(ErflowUserDetails user, CalendarEvent event) {
        return allowed(user, event.type()) && calendarMapper.updateEvent(event) == 1;
    }

    /**
     * 일정을 지운다. 자기 것만 지워진다.
     *
     * @param userId 사번
     * @param id 일정 번호
     * @return 지워졌으면 {@code true}
     */
    @Transactional
    public boolean delete(String userId, int id) {
        return calendarMapper.deleteEvent(id, userId) == 1;
    }

    /**
     * 그 공개 범위로 일정을 다룰 수 있는지.
     *
     * @param user 로그인 사용자
     * @param type 공개 범위
     * @return 다룰 수 있으면 {@code true}
     */
    boolean allowed(ErflowUserDetails user, int type) {
        return switch (type) {
            case 0 -> true;
            case 1 -> hasDeptCalendarPermission(user);
            case 2 -> user.admin();
            // 레거시 switch 에 없는 값이다. 아무 갈래에도 걸리지 않아 거부된다.
            default -> false;
        };
    }

    private boolean hasDeptCalendarPermission(ErflowUserDetails user) {
        ScreenAccess access = authMapper.findProgramAccess(DEPT_CALENDAR_PROGRAM);
        return access != null && Permissions.hasProgramPermission(
                user.deptPermission(), user.jobPermission(),
                access.deptLevel(), access.jobLevel());
    }
}
