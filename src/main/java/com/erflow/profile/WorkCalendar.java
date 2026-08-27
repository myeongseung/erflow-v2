package com.erflow.profile;

import com.erflow.common.WorkTally;
import com.erflow.common.WorkStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 프로필 아래쪽 «이번 달 근무 현황» 표.
 *
 * <p>한 달을 두 줄로 나눠 그린다 — 윗줄이 1~16일, 아랫줄이 17일부터 그 달 마지막
 * 날까지다. 칸은 비어 있고 <b>배경색만으로</b> 상태를 알린다.
 *
 * <h2>통계가 줄마다 따로 세어진다</h2>
 *
 * <p>오른쪽 «정상·지각·조퇴·연차» 는 그 줄만의 합계다. 레거시가 두 줄을 그리면서
 * 카운터를 <b>가운데서 0으로 되돌린다.</b> 그래서 한 달 합계는 어디에도 나오지
 * 않는다 — 윗줄 숫자와 아랫줄 숫자를 눈으로 더해야 한다(D-077).
 *
 * <h2>주말은 세지 않는 상태가 있다</h2>
 *
 * <p>결근·지각·반차는 토·일이면 색도 칠하지 않고 세지도 않는다. 그런데 <b>연차는
 * 주말에도 세어진다</b> — 레거시가 그 자리에만 요일 검사를 넣지 않았다.
 *
 * @param firstHalf 1~16일
 * @param secondHalf 17일부터 그 달 마지막 날까지
 */
public record WorkCalendar(WorkHalf firstHalf, WorkHalf secondHalf) {

    /**
     * 한 달 합계.
     *
     * <p>두 줄을 더한다. 레거시는 이 숫자를 어디에도 보여 주지 않았다 —
     * 줄마다의 합계를 위아래로 붙여 놓아 <b>합계처럼 보이게 해 두었다</b>(D-077).
     *
     * @return 정상·지각·조퇴·연차 한 달 합계
     */
    public WorkTally total() {
        return WorkTally.sum(firstHalf.tally(), secondHalf.tally());
    }

    /** 윗줄 마지막 날. */
    private static final int FIRST_HALF_END = 16;

    /** 아랫줄에 자리를 잡아 두는 마지막 칸. 31일 달도 16칸 안에 들어간다. */
    private static final int SECOND_HALF_END = 32;

    private static final DateTimeFormatter STORED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 한 달치 표를 만든다.
     *
     * @param month 그릴 달
     * @param today 오늘. 오늘보다 뒤인 날은 기록이 있어도 비워 둔다
     * @param works 그 달의 근무 기록. 남의 프로필을 볼 때는 빈 목록이다
     * @return 두 줄짜리 표
     */
    public static WorkCalendar of(YearMonth month, LocalDate today, List<ProfileWork> works) {
        Map<LocalDate, ProfileWork> byDate = index(works);
        int endOfDay = month.atEndOfMonth().getDayOfMonth();

        return new WorkCalendar(
                half(month, today, byDate, 1, FIRST_HALF_END, endOfDay),
                half(month, today, byDate, FIRST_HALF_END + 1, SECOND_HALF_END, endOfDay));
    }

    /**
     * 날짜별 기록을 고른다.
     *
     * <p>출근 시각이 없는 기록은 <b>날짜를 알 수 없으므로 통째로 빠진다.</b> 퇴근만
     * 찍힌 날은 조회에는 걸리지만 달력에는 나오지 않는다.
     *
     * <p>같은 날에 기록이 여럿이면 마지막 것이 이긴다. 레거시가 그렇게 덮어썼다.
     */
    private static Map<LocalDate, ProfileWork> index(List<ProfileWork> works) {
        Map<LocalDate, ProfileWork> byDate = new HashMap<>();
        for (ProfileWork work : works) {
            LocalDate date = startDate(work);
            if (date != null) {
                byDate.put(date, work);
            }
        }
        return byDate;
    }

    private static LocalDate startDate(ProfileWork work) {
        if (work.startedAt() == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(work.startedAt(), STORED).toLocalDate();
        } catch (DateTimeParseException e) {
            // 레거시는 여기서 예외로 화면이 통째로 죽는다. 그 죽음은 옮기지 않는다.
            return null;
        }
    }

    private static WorkHalf half(YearMonth month, LocalDate today,
            Map<LocalDate, ProfileWork> byDate, int from, int to, int endOfDay) {

        List<WorkDay> days = new ArrayList<>();
        int normal = 0;
        int late = 0;
        int leave = 0;
        double vacation = 0d;

        for (int day = from; day <= to; ++day) {
            if (day > endOfDay) {
                days.add(WorkDay.outside(day));
                continue;
            }
            LocalDate date = month.atDay(day);
            boolean sunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean saturday = date.getDayOfWeek() == DayOfWeek.SATURDAY;
            boolean weekend = sunday || saturday;

            ProfileWork work = passed(date, today) ? byDate.get(date) : null;
            int status = work == null ? -1 : work.effectiveStatus();
            String paint = null;

            switch (status) {
                case 0 -> paint = weekend ? null : WorkStatus.styleClass(0);
                // 코드 1(근무 중)은 정상 근무와 같이 세어진다. 다만 색은 갈라 보인다 —
                // 지난 날인데 1 이면 퇴근을 안 찍은 날이다(D-125).
                case 1, 2 -> {
                    paint = WorkStatus.styleClass(status);
                    ++normal;
                }
                case 3 -> {
                    paint = WorkStatus.styleClass(3);
                    ++leave;
                }
                case 4 -> {
                    if (!weekend) {
                        paint = WorkStatus.styleClass(4);
                        ++late;
                    }
                }
                case 5 -> {
                    if (!weekend) {
                        paint = WorkStatus.styleClass(5);
                        vacation += 0.5d;
                    }
                }
                // 연차만 요일을 보지 않는다. 레거시 그대로다.
                case 6 -> {
                    paint = WorkStatus.styleClass(6);
                    vacation += 1.0d;
                }
                default -> {
                    // 기록이 없거나 아는 코드가 아니면 아무 색도 칠하지 않는다.
                }
            }
            days.add(WorkDay.inside(day, sunday, saturday, paint));
        }
        return new WorkHalf(List.copyOf(days), normal, late, leave, vacation);
    }

    /**
     * 오늘까지 지난 날인지.
     *
     * <p>레거시가 {@code Period.between(ld, today)} 의 «달» 과 «일» 이 모두 0 이상인지
     * 본다. {@link Period} 는 각 자리의 부호를 맞춰 주므로 사실상 «오늘보다 뒤가
     * 아니면» 과 같다. 셈을 그대로 옮겨 둔다.
     */
    private static boolean passed(LocalDate date, LocalDate today) {
        Period period = Period.between(date, today);
        return period.getMonths() >= 0 && period.getDays() >= 0;
    }
}
