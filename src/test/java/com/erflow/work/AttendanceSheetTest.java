package com.erflow.work;

import static org.assertj.core.api.Assertions.assertThat;

import com.erflow.profile.ProfileWork;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 근태 표의 셈.
 *
 * <p>게이트는 이 표를 «머리글 몇 칸» 으로만 본다. 칸에 무슨 글자가 어떤 색으로
 * 찍히는지, 통계가 어떻게 세어지는지는 여기서 못 박는다. DB 를 타지 않는다.
 */
class AttendanceSheetTest {

    /** 2023년 11월. 1일이 수요일이라 4·5일이 첫 주말, 18·19일이 세 번째 주말이다. */
    private static final YearMonth NOVEMBER = YearMonth.of(2023, 11);

    /** 그 달이 다 지난 뒤. 오늘 검사에 걸리는 날이 없다. */
    private static final LocalDate LATER = LocalDate.of(2024, 1, 1);

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 12, 0);

    @Test
    @DisplayName("정상 근무는 일한 시간이 찍힌다 — 한 시간을 빼고")
    void normalDayShowsWorkedTime() {
        AttendanceSheet sheet = sheet(work("2023-11-01 09:00:00", "2023-11-01 18:30:00", 2));

        AttendanceCell cell = sheet.first().cells().get(0);
        assertThat(cell.text()).isEqualTo("08:30");
        assertThat(cell.style()).isNull();
        assertThat(sheet.first().normal()).isEqualTo(1);
    }

    @Test
    @DisplayName("근무 중(1)도 시간이 찍히고 정상으로 세어진다 — 퇴근 자리는 «지금»")
    void workingDayGrowsUntilNow() {
        AttendanceSheet sheet = sheet(work("2023-11-01 09:00:00", null, 1));

        // 09:00 -> NOW(정오) 인데 날짜가 달라 몇천 시간이 된다. 레거시도 그렇다 —
        // 여기서 확인할 것은 «시간이 찍히고 세어진다» 는 사실이다.
        assertThat(sheet.first().cells().get(0).text()).contains(":");
        assertThat(sheet.first().normal()).isEqualTo(1);
    }

    @Test
    @DisplayName("결근은 빨간 글자 — 주말이면 글자만 지워지고 색은 남는다")
    void absentKeepsItsColourOnWeekends() {
        AttendanceSheet weekday = sheet(work("2023-11-01 09:00:00", null, 0));
        AttendanceSheet weekend = sheet(work("2023-11-04 09:00:00", null, 0));

        assertThat(weekday.first().cells().get(0).text()).isEqualTo("결근");
        assertThat(weekday.first().cells().get(0).style()).contains("#E51A2E");
        assertThat(weekend.first().cells().get(3).text()).isEmpty();
        // 글자 없는 칸에 style 만 남는다. 보이지 않지만 레거시가 그렇게 그린다.
        assertThat(weekend.first().cells().get(3).style()).contains("#E51A2E");
    }

    @Test
    @DisplayName("조퇴의 주말 처리가 줄마다 다르다 — 윗줄은 세고 아랫줄은 지운다")
    void earlyLeaveWeekendRuleDiffersByHalf() {
        // 11-04 는 윗줄 주말, 11-18 은 아랫줄 주말이다.
        AttendanceSheet sheet = sheet(
                work("2023-11-04 09:00:00", "2023-11-04 14:00:00", 3),
                work("2023-11-18 09:00:00", "2023-11-18 14:00:00", 3));

        assertThat(sheet.first().cells().get(3).text()).isEqualTo("조퇴");
        assertThat(sheet.first().leave()).isEqualTo(1);
        assertThat(sheet.second().cells().get(1).text()).isEmpty();
        assertThat(sheet.second().leave()).isZero();
    }

    @Test
    @DisplayName("지각·반차는 두 줄 다 주말이면 지워지고, 연차만 주말에도 세어진다")
    void weekendRulesForTheRest() {
        AttendanceSheet sheet = sheet(
                work("2023-11-04 09:30:00", "2023-11-04 18:00:00", 4),
                work("2023-11-05 09:00:00", "2023-11-05 13:00:00", 5),
                work("2023-11-18 09:00:00", null, 6));

        assertThat(sheet.first().late()).isZero();
        assertThat(sheet.first().vacation()).isZero();
        assertThat(sheet.second().cells().get(1).text()).isEqualTo("연차");
        assertThat(sheet.second().vacation()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("통계는 줄마다 따로 세어진다 — 프로필 달력과 같다(D-077)")
    void statisticsResetBetweenHalves() {
        AttendanceSheet sheet = sheet(
                work("2023-11-01 09:00:00", "2023-11-01 18:00:00", 2),
                work("2023-11-20 09:00:00", "2023-11-20 18:00:00", 2));

        assertThat(sheet.first().normal()).isEqualTo(1);
        assertThat(sheet.second().normal()).isEqualTo(1);
    }

    @Test
    @DisplayName("아는 코드가 아니면 «결근» 글자가 색 없이 남는다")
    void unknownStatusFallsBackToAbsentText() {
        AttendanceSheet sheet = sheet(work("2023-11-01 09:00:00", null, 9));

        assertThat(sheet.first().cells().get(0).text()).isEqualTo("결근");
        assertThat(sheet.first().cells().get(0).style()).isNull();
    }

    @Test
    @DisplayName("오늘보다 뒤인 날과 기록 없는 날은 빈 칸이다")
    void futureAndMissingDaysAreBlank() {
        LocalDate today = LocalDate.of(2023, 11, 10);
        AttendanceSheet sheet = AttendanceSheet.of(NOVEMBER, today, NOW, List.of(
                work("2023-11-10 09:00:00", "2023-11-10 18:00:00", 2),
                work("2023-11-11 09:00:00", "2023-11-11 18:00:00", 2)));

        assertThat(sheet.first().cells().get(9).text()).isEqualTo("08:00");
        assertThat(sheet.first().cells().get(10)).isEqualTo(AttendanceCell.EMPTY);
        assertThat(sheet.first().cells().get(2)).isEqualTo(AttendanceCell.EMPTY);
    }

    @Test
    @DisplayName("30일 달의 아랫줄 끝 두 칸은 빈 칸이다")
    void shortMonthTailIsBlank() {
        AttendanceSheet sheet = sheet();

        assertThat(sheet.second().cells()).hasSize(16);
        assertThat(sheet.second().cells().get(14)).isEqualTo(AttendanceCell.EMPTY);
        assertThat(sheet.second().cells().get(15)).isEqualTo(AttendanceCell.EMPTY);
    }

    @Test
    @DisplayName("반차는 0.5로 더해져 «0.5» 로 찍힌다")
    void halfDayShowsAsDecimal() {
        AttendanceSheet sheet = sheet(work("2023-11-01 09:00:00", "2023-11-01 13:00:00", 5));

        assertThat(sheet.first().vacationLabel()).isEqualTo("0.5");
        assertThat(sheet.second().vacationLabel()).isEqualTo("0.0");
    }

    private static AttendanceSheet sheet(ProfileWork... works) {
        return AttendanceSheet.of(NOVEMBER, LATER, NOW, List.of(works));
    }

    private static ProfileWork work(String started, String ended, int status) {
        return new ProfileWork(started, ended, status);
    }
}
