package com.erflow.admin.home;

import com.erflow.common.WorkStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 대시보드 «출/퇴근관리» 칸 한 줄. {@code work_view} 한 행.
 *
 * @param userName 이름
 * @param startedAt 출근 시각
 * @param endedAt 퇴근 시각
 * @param status 근무 상태 코드
 */
public record WorkRow(String userName, String startedAt, String endedAt, int status) {

    /** 이름을 잘라 내는 길이. 레거시가 6글자에서 자르고 «...» 을 붙인다. */
    private static final int NAME_LIMIT = 6;

    private static final DateTimeFormatter STORED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 시간을 찍지 않는 자리에 들어가는 글자. */
    private static final String NO_TIME = "-";

    /**
     * 화면에 찍히는 이름.
     *
     * @return 6글자가 넘으면 잘라 «...» 을 붙인 이름
     */
    public String shortName() {
        return userName != null && userName.length() > NAME_LIMIT
                ? userName.substring(0, NAME_LIMIT) + "..."
                : userName;
    }

    /**
     * @return 근무 상태 라벨
     */
    public String statusLabel() {
        return WorkStatus.label(status);
    }

    /**
     * 화면에 찍히는 근무 시간.
     *
     * <p>레거시가 <b>한 시간을 뺀다</b>(점심시간으로 보인다). 출근·퇴근 시각 중 없는
     * 쪽은 «지금» 으로 채우므로, 근무 중인 사람은 화면을 열 때마다 값이 늘어난다.
     *
     * @return {@code HH:mm}. 시간을 세지 않는 상태면 {@code -}
     */
    public String workedTime() {
        if (!WorkStatus.counted(status)) {
            return NO_TIME;
        }
        Duration worked = Duration.between(
                parse(startedAt), parse(endedAt)).minusHours(1L);
        return String.format("%02d:%02d", worked.toHoursPart(), worked.toMinutesPart());
    }

    private static LocalDateTime parse(String value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, STORED);
        } catch (DateTimeParseException e) {
            // 레거시는 여기서 예외로 화면이 통째로 죽는다. 그 죽음은 옮기지 않는다.
            return LocalDateTime.now();
        }
    }
}
