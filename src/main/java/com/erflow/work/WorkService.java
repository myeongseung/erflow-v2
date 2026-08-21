package com.erflow.work;

import com.erflow.common.Pagination;
import com.erflow.profile.ProfileMapper;
import com.erflow.profile.WorkDay;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 근태 확인 업무.
 *
 * <p>레거시 {@code work/work.jsp} 가 화면 안에서 하던 조회와 셈이다. 사람 목록을
 * 8명씩 끊고, 사람마다 그 달의 근무 기록을 읽어 두 줄짜리 표를 만든다.
 *
 * <p>근무 기록 조회는 프로필과 같은 것을 쓴다({@link ProfileMapper#findWorks}) —
 * 레거시도 같은 {@code getWorkViews(id, date)} 를 불렀다.
 */
@Service
public class WorkService {

    /** 페이지당 인원. 한 사람이 두 줄을 차지해 레거시가 8명씩 끊는다. */
    static final int USERS_PER_PAGE = 8;

    private final WorkMapper workMapper;
    private final ProfileMapper profileMapper;
    private final Clock clock;

    /**
     * @param workMapper 사람 목록 조회
     * @param profileMapper 근무 기록 조회
     * @param clock 오늘과 «지금» 을 정하는 시계
     */
    public WorkService(WorkMapper workMapper, ProfileMapper profileMapper, Clock clock) {
        this.workMapper = workMapper;
        this.profileMapper = profileMapper;
        this.clock = clock;
    }

    /**
     * 근태 표 한 페이지.
     *
     * @param rows 사람별 두 줄
     * @param pagination 페이징
     */
    public record WorkPage(List<PersonRow> rows, Pagination pagination) {
    }

    /**
     * 표의 한 사람.
     *
     * @param user 성명·부서·직책
     * @param sheet 두 줄짜리 근태
     */
    public record PersonRow(WorkUserRow user, AttendanceSheet sheet) {
    }

    /**
     * 한 페이지를 만든다.
     *
     * @param month 그릴 달
     * @param dept 부서 검색어. 비우면 조건 없음
     * @param name 이름 검색어. 비우면 조건 없음
     * @param nowPage 현재 페이지
     * @return 사람별 두 줄과 페이징
     */
    @Transactional(readOnly = true)
    public WorkPage page(YearMonth month, String dept, String name, int nowPage) {
        Pagination pagination =
                Pagination.of(workMapper.countUsers(dept, name), nowPage, USERS_PER_PAGE);

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<PersonRow> rows = new ArrayList<>();
        for (WorkUserRow user : workMapper.findUsers(
                dept, name, pagination.start(), USERS_PER_PAGE)) {
            rows.add(new PersonRow(user, AttendanceSheet.of(
                    month, today, now, profileMapper.findWorks(user.id(), month.toString()))));
        }
        return new WorkPage(rows, pagination);
    }

    /**
     * 날짜 머리글. 윗줄 1~16일, 아랫줄 17일~말일(빈 칸 포함)이며 주말에 색이 붙는다.
     *
     * <p>프로필 달력과 같은 모양이라 그쪽의 {@link WorkDay} 를 그대로 쓴다.
     *
     * @param month 그릴 달
     * @param from 첫 날
     * @param to 마지막 칸
     * @return 머리글 목록
     */
    public List<WorkDay> headers(YearMonth month, int from, int to) {
        int endOfDay = month.atEndOfMonth().getDayOfMonth();
        List<WorkDay> days = new ArrayList<>();
        for (int day = from; day <= to; ++day) {
            if (day > endOfDay) {
                days.add(WorkDay.outside(day));
                continue;
            }
            DayOfWeek dow = month.atDay(day).getDayOfWeek();
            days.add(WorkDay.inside(
                    day, dow == DayOfWeek.SUNDAY, dow == DayOfWeek.SATURDAY, null));
        }
        return days;
    }

    /**
     * 화면이 처음 여는 달.
     *
     * @return 이번 달
     */
    public YearMonth thisMonth() {
        return YearMonth.now(clock);
    }

    /**
     * 달 고르기의 하한. 올해 1월로 고정이다 — 프로필과 같다.
     *
     * @return 올해 1월
     */
    public YearMonth minMonth() {
        return YearMonth.now(clock).withMonth(1);
    }
}
