package com.erflow.work;

import com.erflow.common.WorkTally;
import com.erflow.common.WorkStatus;
import com.erflow.profile.ProfileWork;
import java.time.DayOfWeek;
import java.time.Duration;
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
 * 근태 확인 표에서 한 사람의 두 줄. 윗줄이 1~16일, 아랫줄이 17일부터 말일까지다.
 *
 * <p>프로필 달력(D-077)과 데이터는 같지만 그리는 법이 다르다 — 여기는 색이 아니라
 * <b>글자</b>다. 정상 근무는 일한 시간(HH:mm), 나머지는 상태 이름이 색 글자로 찍힌다.
 *
 * <h2>주말 규칙이 줄마다 또 다르다</h2>
 *
 * <p>지각·반차는 두 줄 다 주말이면 지워진다. 연차는 두 줄 다 주말에도 세어진다.
 * 그런데 <b>조퇴는 윗줄에서만 주말을 보지 않는다</b> — 1~16일의 주말 조퇴는 세어지고
 * 색까지 칠해지는데, 17일 뒤의 주말 조퇴는 지워진다. 레거시가 두 벌로 복사한 switch
 * 가 서로 어긋난 결과이며 그대로 옮긴다(D-099).
 *
 * <h2>일한 시간은 열 때마다 자란다</h2>
 *
 * <p>퇴근이 안 찍힌 기록은 «지금» 을 퇴근으로 놓고 시간을 센다. 점심으로 보이는 한
 * 시간을 빼는 것까지 대시보드(D-068 주변)와 같다.
 *
 * @param first 1~16일
 * @param second 17일부터 말일까지
 */
public record AttendanceSheet(AttendanceHalf first, AttendanceHalf second) {

    /**
     * 한 달 합계.
     *
     * <p>두 줄을 더한다. 레거시는 이 숫자를 어디에도 보여 주지 않았다 —
     * 줄마다의 합계를 위아래로 붙여 놓아 <b>합계처럼 보이게 해 두었다</b>(D-077).
     *
     * @return 정상·지각·조퇴·연차 한 달 합계
     */
    public WorkTally total() {
        return WorkTally.sum(first.tally(), second.tally());
    }

    /** 윗줄 마지막 날. */
    private static final int FIRST_HALF_END = 16;

    /** 아랫줄에 자리를 잡아 두는 마지막 칸. 31일 달도 16칸 안에 들어간다. */
    private static final int SECOND_HALF_END = 32;

    private static final DateTimeFormatter STORED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
      색은 여기 없다. 레거시는 칸마다 style="color: #8507F7; font-weight: bold;" 를
      뱉었고, 그래서 같은 조퇴가 달력과 이 표에서 다른 색이었다. 이제
      상태 클래스만 내보내고 app.css 의 .att-cell 이 글자색을 정한다(D-125).
    */
    private static final String ABSENT = WorkStatus.styleClass(0);
    private static final String LEAVE = WorkStatus.styleClass(3);
    private static final String LATE = WorkStatus.styleClass(4);
    private static final String HALF_DAY = WorkStatus.styleClass(5);
    private static final String VACATION = WorkStatus.styleClass(6);

    /**
     * 한 사람의 두 줄을 만든다.
     *
     * @param month 그릴 달
     * @param today 오늘. 오늘보다 뒤인 날은 기록이 있어도 비워 둔다
     * @param now 일한 시간을 셀 때의 «지금». 퇴근이 안 찍힌 기록에 쓴다
     * @param works 그 달의 근무 기록
     * @return 두 줄
     */
    public static AttendanceSheet of(
            YearMonth month, LocalDate today, LocalDateTime now, List<ProfileWork> works) {

        Map<LocalDate, ProfileWork> byDate = index(works);
        int endOfDay = month.atEndOfMonth().getDayOfMonth();
        return new AttendanceSheet(
                half(month, today, now, byDate, 1, FIRST_HALF_END, endOfDay, true),
                half(month, today, now, byDate,
                        FIRST_HALF_END + 1, SECOND_HALF_END, endOfDay, false));
    }

    /**
     * 날짜별 기록. 출근이 안 찍힌 기록은 날짜를 알 수 없어 빠지고, 같은 날이 겹치면
     * 마지막 것이 이긴다 — 프로필 달력과 같다.
     */
    private static Map<LocalDate, ProfileWork> index(List<ProfileWork> works) {
        Map<LocalDate, ProfileWork> byDate = new HashMap<>();
        for (ProfileWork work : works) {
            if (work.startedAt() == null) {
                continue;
            }
            try {
                byDate.put(LocalDateTime.parse(work.startedAt(), STORED).toLocalDate(), work);
            } catch (DateTimeParseException e) {
                // 레거시는 여기서 예외로 화면이 통째로 죽는다. 그 죽음은 옮기지 않는다.
            }
        }
        return byDate;
    }

    private static AttendanceHalf half(
            YearMonth month, LocalDate today, LocalDateTime now,
            Map<LocalDate, ProfileWork> byDate,
            int from, int to, int endOfDay, boolean firstHalf) {

        List<AttendanceCell> cells = new ArrayList<>();
        int normal = 0;
        int late = 0;
        int leave = 0;
        double vacation = 0d;

        for (int day = from; day <= to; ++day) {
            if (day > endOfDay) {
                cells.add(AttendanceCell.EMPTY);
                continue;
            }
            LocalDate date = month.atDay(day);
            ProfileWork work = passed(date, today) ? byDate.get(date) : null;
            if (work == null) {
                cells.add(AttendanceCell.EMPTY);
                continue;
            }
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            switch (work.status()) {
                case 0 ->
                    // 주말 결근은 글자만 지운다. 빨간 style 은 빈 칸에도 남는다 —
                    // 레거시가 글자만 비우고 색은 그대로 둔다. 보이지는 않는다.
                    cells.add(new AttendanceCell(weekend ? "" : "결근", ABSENT));
                // 근무 중(1)은 아래로 흘러 정상 퇴근과 같이 세어진다. 레거시 switch 에
                // break 가 없다 — 같은 시간을 두 번 계산할 뿐 결과는 같다.
                case 1, 2 -> {
                    cells.add(new AttendanceCell(workedTime(work, now), null));
                    ++normal;
                }
                case 3 -> {
                    // 조퇴만 줄이 갈린다. 윗줄은 주말을 보지 않는다(D-099).
                    if (firstHalf || !weekend) {
                        cells.add(new AttendanceCell("조퇴", LEAVE));
                        ++leave;
                    } else {
                        cells.add(AttendanceCell.EMPTY);
                    }
                }
                case 4 -> {
                    if (weekend) {
                        cells.add(AttendanceCell.EMPTY);
                    } else {
                        cells.add(new AttendanceCell("지각", LATE));
                        ++late;
                    }
                }
                case 5 -> {
                    if (weekend) {
                        cells.add(AttendanceCell.EMPTY);
                    } else {
                        cells.add(new AttendanceCell("반차", HALF_DAY));
                        vacation += 0.5d;
                    }
                }
                case 6 -> {
                    // 연차만 두 줄 다 요일을 보지 않는다. 프로필 달력과 같다(D-077).
                    cells.add(new AttendanceCell("연차", VACATION));
                    vacation += 1.0d;
                }
                // 아는 코드가 아니면 «결근» 글자가 색 없이 남는다. 레거시가 기본
                // 문구를 결근으로 두고 switch 에 default 가 없다.
                default -> cells.add(new AttendanceCell("결근", null));
            }
        }
        return new AttendanceHalf(List.copyOf(cells), normal, late, leave, vacation);
    }

    /**
     * 일한 시간. 레거시가 <b>한 시간을 뺀다</b>(점심으로 보인다).
     */
    private static String workedTime(ProfileWork work, LocalDateTime now) {
        LocalDateTime started = parse(work.startedAt(), now);
        LocalDateTime ended = parse(work.endedAt(), now);
        Duration worked = Duration.between(started, ended).minusHours(1L);
        return String.format("%02d:%02d", worked.toHoursPart(), worked.toMinutesPart());
    }

    private static LocalDateTime parse(String value, LocalDateTime now) {
        if (value == null) {
            return now;
        }
        try {
            return LocalDateTime.parse(value, STORED);
        } catch (DateTimeParseException e) {
            return now;
        }
    }

    /**
     * 오늘까지 지난 날인지. 프로필 달력과 같은 셈이다({@code Period} 의 달·일 부호).
     */
    private static boolean passed(LocalDate date, LocalDate today) {
        Period period = Period.between(date, today);
        return period.getMonths() >= 0 && period.getDays() >= 0;
    }
}
