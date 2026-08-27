package com.erflow.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.erflow.common.WorkTally;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 근무 현황 표의 셈.
 *
 * <p>게이트는 이 표를 «머리글 몇 칸» 으로만 본다. 어떤 날이 무슨 색이 되고 통계가
 * 어떻게 세어지는지는 보지 못한다 — 그 자리를 여기서 못 박는다.
 *
 * <p>DB 를 타지 않는다. 셈만 확인하므로 시계도 값으로 넣는다.
 */
class WorkCalendarTest {

    /** 2023년 11월. 1일이 수요일이라 4·5일이 첫 주말이다. */
    private static final YearMonth NOVEMBER = YearMonth.of(2023, 11);

    /** 그 달이 다 지난 뒤. 오늘 검사에 걸리는 날이 없다. */
    private static final LocalDate LATER = LocalDate.of(2024, 1, 1);

    @Test
    @DisplayName("한 달이 두 줄로 나뉜다 — 줄마다 16칸이다")
    void twoRowsOfSixteen() {
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER, List.of());

        assertThat(calendar.firstHalf().days()).hasSize(16);
        assertThat(calendar.secondHalf().days()).hasSize(16);
        assertThat(calendar.firstHalf().days().get(0).label()).isEqualTo("01");
        assertThat(calendar.secondHalf().days().get(0).label()).isEqualTo("17");
    }

    @Test
    @DisplayName("30일 달은 아랫줄 끝 두 칸이 빈칸이다 — 높이도 붙지 않는다")
    void tailOfAShortMonthIsEmpty() {
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER, List.of());
        List<WorkDay> second = calendar.secondHalf().days();

        // 17 + 13 = 30 이 마지막 날이다.
        assertThat(second.get(13).inMonth()).isTrue();
        assertThat(second.get(14).inMonth()).isFalse();
        assertThat(second.get(14).label()).isEmpty();
        assertThat(second.get(14).cellClass()).isNull();
        assertThat(second.get(13).cellClass()).isEqualTo("work-cell");
    }

    @Test
    @DisplayName("주말 날짜는 색이 다르다 — 일요일 빨강, 토요일 파랑")
    void weekendHeadersAreColoured() {
        List<WorkDay> days = WorkCalendar.of(NOVEMBER, LATER, List.of()).firstHalf().days();

        // 2023-11-04 토, 11-05 일
        assertThat(days.get(3).headerClass()).isEqualTo("is-saturday");
        assertThat(days.get(4).headerClass()).isEqualTo("is-sunday");
        assertThat(days.get(5).headerClass()).isNull();
    }

    @Test
    @DisplayName("줄별 통계는 그대로다 — 줄마다 따로 세어진다")
    void statisticsResetBetweenRows() {
        // 화면은 이제 한 달 합계만 보여 준다(D-127). 그래도 줄별 셈은 남겨 둔다 —
        // total() 이 이 둘을 더해 만드는 것이므로 여기가 틀어지면 합계도 틀린다.
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER, List.of(
                worked("2023-11-01", 2), worked("2023-11-02", 2),
                worked("2023-11-20", 2)));

        assertThat(calendar.firstHalf().normal()).isEqualTo(2);
        assertThat(calendar.secondHalf().normal()).isEqualTo(1);
    }

    @Test
    @DisplayName("근무 중(1)도 정상 근무로 세어진다")
    void workingCountsAsNormal() {
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER,
                List.of(worked("2023-11-01", 1)));

        assertThat(calendar.firstHalf().normal()).isEqualTo(1);
        assertThat(calendar.firstHalf().days().get(0).cellClass())
                .isEqualTo("work-cell is-working");
    }

    @Test
    @DisplayName("결근·지각·반차는 주말이면 세지 않는다")
    void weekendIsForgivenForSomeStatuses() {
        // 2023-11-04 는 토요일이다.
        WorkCalendar late = WorkCalendar.of(NOVEMBER, LATER,
                List.of(worked("2023-11-04", 4)));
        WorkCalendar absent = WorkCalendar.of(NOVEMBER, LATER,
                List.of(worked("2023-11-04", 0)));

        assertThat(late.firstHalf().late()).isZero();
        assertThat(late.firstHalf().days().get(3).cellClass()).isEqualTo("work-cell");
        assertThat(absent.firstHalf().days().get(3).cellClass()).isEqualTo("work-cell");
    }

    @Test
    @DisplayName("연차만 주말에도 세어진다 — 그 자리에만 요일 검사가 없다")
    void annualLeaveIgnoresTheWeekend() {
        WorkCalendar weekend = WorkCalendar.of(NOVEMBER, LATER,
                List.of(worked("2023-11-04", 6)));
        WorkCalendar halfDay = WorkCalendar.of(NOVEMBER, LATER,
                List.of(worked("2023-11-04", 5)));

        assertThat(weekend.firstHalf().vacation()).isEqualTo(1.0d);
        // 같은 토요일인데 반차는 세어지지 않는다.
        assertThat(halfDay.firstHalf().vacation()).isZero();
    }

    @Test
    @DisplayName("반차는 0.5로 더해져 소수로 찍힌다")
    void halfDayIsAHalf() {
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER, List.of(
                worked("2023-11-01", 5), worked("2023-11-02", 6)));

        assertThat(calendar.firstHalf().vacationLabel()).isEqualTo("1.5");
        assertThat(calendar.secondHalf().vacationLabel()).isEqualTo("0.0");
    }

    @Test
    @DisplayName("출근이 안 찍힌 기록은 달력에 오르지 못한다")
    void recordsWithoutAStartTimeVanish() {
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER,
                List.of(new ProfileWork(null, "2023-11-01 18:00:00", 2)));

        assertThat(calendar.firstHalf().normal()).isZero();
        assertThat(calendar.firstHalf().days().get(0).cellClass()).isEqualTo("work-cell");
    }

    @Test
    @DisplayName("출근도 퇴근도 없으면 무슨 코드든 결근이다")
    void bothTimesMissingMeansAbsent() {
        assertThat(new ProfileWork(null, null, 6).effectiveStatus()).isZero();
        assertThat(new ProfileWork("2023-11-01 09:00:00", null, 6).effectiveStatus())
                .isEqualTo(6);
    }

    @Test
    @DisplayName("오늘보다 뒤인 날은 기록이 있어도 비어 있다")
    void futureDaysStayBlank() {
        LocalDate today = LocalDate.of(2023, 11, 10);
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, today, List.of(
                worked("2023-11-10", 2), worked("2023-11-11", 2)));

        assertThat(calendar.firstHalf().normal()).isEqualTo(1);
        assertThat(calendar.firstHalf().days().get(10).cellClass())
                .isEqualTo("work-cell");
    }

    @Test
    @DisplayName("같은 날 기록이 여럿이면 마지막 것이 이긴다")
    void lastRecordOfADayWins() {
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER, List.of(
                worked("2023-11-01 09:00:00", 2), worked("2023-11-01 13:00:00", 3)));

        assertThat(calendar.firstHalf().normal()).isZero();
        assertThat(calendar.firstHalf().leave()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 달 합계는 두 줄을 가로지른다 — 줄별 합계가 아니다")
    void totalSpansBothHalves() {
        // 윗줄(1~16)에 정상 둘·지각 하나, 아랫줄(17~)에 정상 하나·조퇴 하나·반차 하나.
        WorkCalendar calendar = WorkCalendar.of(NOVEMBER, LATER, List.of(
                worked("2023-11-01", 2), worked("2023-11-02", 2), worked("2023-11-03", 4),
                worked("2023-11-20", 2), worked("2023-11-21", 3), worked("2023-11-22", 5)));

        // 레거시가 보여 주던 것 — 줄마다 따로다.
        assertThat(calendar.firstHalf().normal()).isEqualTo(2);
        assertThat(calendar.secondHalf().normal()).isEqualTo(1);

        // 화면 어디에도 없던 숫자(D-077).
        WorkTally total = calendar.total();
        assertThat(total.normal()).isEqualTo(3);
        assertThat(total.late()).isEqualTo(1);
        assertThat(total.leave()).isEqualTo(1);
        assertThat(total.vacation()).isEqualTo(0.5d);
        assertThat(total.vacationLabel()).isEqualTo("0.5");
    }

    private static ProfileWork worked(String day, int status) {
        String startedAt = day.length() > 10 ? day : day + " 09:00:00";
        return new ProfileWork(startedAt, startedAt.substring(0, 10) + " 18:00:00", status);
    }
}
