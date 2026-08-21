package com.erflow.calendar;

import com.erflow.auth.ErflowUserDetails;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메인 화면 달력이 부르는 JSON 창구.
 *
 * <pre>
 * CalendarViewRequest      GET  /calendar/view
 * CalendarRegisterRequest  POST /calendar/insert
 * CalendarUpdateRequest    POST /calendar/update
 * CalendarDeleteRequest    POST /calendar/delete
 * </pre>
 *
 * <p>레거시는 서블릿 넷이었고, 넷 다 <b>보낸 사번이 로그인한 사번과 같은지</b> 를 먼저
 * 본다. 클라이언트가 자기 사번을 실어 보내는 구조라 남을 사칭할 수는 없다. 그대로
 * 옮긴다 — 없어도 되는 검사지만 빼면 동작이 달라진다.
 *
 * <p>돌려주는 모양도 그대로다. 성공·실패를 HTTP 상태가 아니라 본문의
 * {@code status} 로 알린다.
 */
@RestController
@RequestMapping("/calendar")
public class CalendarController {

    /**
     * 종료 시각을 검사할 때 쓰는 형식.
     *
     * <p>레거시가 세 가지를 함께 받는다. 가운데 {@code 'A'} 는 화면이 보낼 일이 없는
     * 모양인데, {@code 'T'} 를 적으려다 잘못 적은 것으로 보인다 — 그대로 둔다.
     */
    private static final DateTimeFormatter LENIENT = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd HH:mm:ss]")
            .appendPattern("[yyyy-MM-dd'A'HH:mm]")
            .appendPattern("[yyyy-MM-dd'T'HH:mm]")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    private final CalendarService calendarService;

    /**
     * @param calendarService 일정 업무
     */
    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * 일정 만들기 요청.
     *
     * @param userId 보낸 사람 사번
     * @param subject 제목
     * @param content 내용
     * @param start 시작 시각
     * @param end 종료 시각
     * @param type 공개 범위
     */
    public record InsertRequest(
            String userId, String subject, String content,
            String start, String end, int type) {
    }

    /**
     * 일정 고치기 요청. 이름이 만들기와 다르다 — 레거시 화면이 그렇게 보낸다.
     *
     * @param userId 보낸 사람 사번
     * @param eventId 일정 번호
     * @param subject 제목
     * @param content 내용
     * @param startDate 시작 시각
     * @param endDate 종료 시각
     * @param type 공개 범위
     */
    public record UpdateRequest(
            String userId, int eventId, String subject, String content,
            String startDate, String endDate, int type) {
    }

    /**
     * 일정 지우기 요청.
     *
     * @param userId 보낸 사람 사번
     * @param eventId 일정 번호
     */
    public record DeleteRequest(String userId, int eventId) {
    }

    /**
     * 달력에 그릴 일정 전부.
     *
     * @param user 로그인 사용자
     * @return 일정 목록
     */
    @GetMapping(value = "/view", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CalendarEvent> view(@AuthenticationPrincipal ErflowUserDetails user) {
        return calendarService.visible(user.id());
    }

    /**
     * 일정 만들기.
     *
     * @param request 요청 본문
     * @param user 로그인 사용자
     * @return 처리 결과
     */
    @PostMapping("/insert")
    public Map<String, String> insert(
            @RequestBody InsertRequest request,
            @AuthenticationPrincipal ErflowUserDetails user) {

        boolean done = user.id().equals(request.userId())
                && calendarService.create(user, new CalendarEvent(
                        0, request.userId(), 0, null, request.subject(), request.content(),
                        request.start(), readableOrNull(request.end()), request.type()));

        return result(done, "이벤트가 성공적으로 추가되었습니다.", "이벤트 추가에 실패했습니다.");
    }

    /**
     * 일정 고치기.
     *
     * @param request 요청 본문
     * @param user 로그인 사용자
     * @return 처리 결과
     */
    @PostMapping("/update")
    public Map<String, String> update(
            @RequestBody UpdateRequest request,
            @AuthenticationPrincipal ErflowUserDetails user) {

        boolean done = user.id().equals(request.userId())
                && calendarService.update(user, new CalendarEvent(
                        request.eventId(), request.userId(), 0, null,
                        request.subject(), request.content(),
                        request.startDate(), readableOrNull(request.endDate()),
                        request.type()));

        return result(done, "이벤트가 성공적으로 변경되었습니다.", "이벤트 변경에 실패했습니다.");
    }

    /**
     * 일정 지우기.
     *
     * @param request 요청 본문
     * @param user 로그인 사용자
     * @return 처리 결과
     */
    @PostMapping("/delete")
    public Map<String, String> delete(
            @RequestBody DeleteRequest request,
            @AuthenticationPrincipal ErflowUserDetails user) {

        boolean done = user.id().equals(request.userId())
                && calendarService.delete(user.id(), request.eventId());

        return result(done, "이벤트가 성공적으로 삭제되었습니다.", "이벤트 삭제에 실패했습니다.");
    }

    /**
     * 읽을 수 없는 종료 시각은 «없음» 으로 만든다.
     *
     * <p>레거시가 파싱만 해 보고 실패하면 {@code null} 을 넣는다 — 파싱 결과는 쓰지
     * 않는다. 종료일 없는 일정이 되는 것이므로 거절이 아니다.
     */
    private static String readableOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            LocalDateTime.parse(value, LENIENT);
            return value;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Map<String, String> result(boolean done, String ok, String fail) {
        return Map.of(
                "status", done ? "success" : "error",
                "message", done ? ok : fail);
    }
}
