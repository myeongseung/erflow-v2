package com.erflow.message;

import com.erflow.auth.ErflowUserDetails;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 쪽지 화면.
 *
 * <pre>
 * message/index.jsp    GET  /message?class=receiver|sender
 * message/read.jsp     GET  /message/read?messageId=
 * message/register.jsp GET  /message/register        (쓰기 폼)
 * message/reply.jsp    GET  /message/reply           (답장 폼)
 * registerProc.jsp     POST /message/register        (전송)
 * deleteProc.jsp       POST /message/delete          (목록에서 삭제)
 * readDeleteProc.jsp   POST /message/read-delete     (읽기에서 삭제)
 * </pre>
 *
 * <p>받은쪽지함(receiver)과 보낸쪽지함(sender)이 같은 화면을 쓰고 {@code class} 로
 * 갈린다. 쓰기·읽기·답장은 {@code window.open} 으로 여는 팝업이다.
 *
 * <p>{@code screen} 테이블에는 {@code /message} 만 등록돼 있다 — 팝업은 로그인만
 * 검사한다(레거시도 그렇다).
 */
@Controller
public class MessageController {

    private final MessageService messageService;

    /**
     * @param messageService 쪽지 업무
     */
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 쪽지함 목록.
     *
     * <p>레거시는 {@code class} 가 없으면 receiver 로 보고, receiver/sender 가 아니면
     * 잘못된 접근으로 보냈다.
     *
     * @param className receiver 또는 sender. 없으면 receiver
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 쪽지함 템플릿. class 가 잘못되면 잘못된 접근 화면
     */
    @GetMapping("/message")
    public String index(
            @RequestParam(name = "class", required = false) String className,
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        String box = className == null || className.isBlank() ? MessageService.RECEIVER : className;
        if (!MessageService.RECEIVER.equals(box) && !MessageService.SENDER.equals(box)) {
            return "redirect:/access-error";
        }

        MessageSearch search = new MessageSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        MessageService.MessagePage page = messageService.list(box, user.id(), search, nowPage);

        model.addAttribute("className", box);
        model.addAttribute("messages", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "message/index";
    }

    /**
     * 쪽지 읽기. 여는 순간 «읽음» 으로 표시한다(D-046).
     *
     * @param messageId 쪽지 번호
     * @param model 뷰 모델
     * @return 읽기 템플릿. 번호가 없거나 쪽지가 없으면 잘못된 접근 화면
     */
    @GetMapping("/message/read")
    public String read(@RequestParam(required = false) Integer messageId, Model model) {
        if (messageId == null) {
            return "redirect:/access-error";
        }
        MessageDetail message = messageService.read(messageId);
        if (message == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("message", message);
        return "message/read";
    }

    /**
     * 쪽지 쓰기 폼.
     *
     * @return 쓰기 템플릿
     */
    @GetMapping("/message/register")
    public String registerForm() {
        return "message/register";
    }

    /**
     * 답장 폼. 받는 사람은 여는 창이 채운다.
     *
     * @return 답장 템플릿
     */
    @GetMapping("/message/reply")
    public String replyForm() {
        return "message/reply";
    }

    /**
     * 쪽지 전송.
     *
     * <p>받는 사람은 {@code ;} 로 이어 온다(사용자 찾기가 여러 명을 그렇게 넘긴다).
     * 받는 사람·내용이 모두 있어야 보낸다.
     *
     * @param receiverId {@code ;} 로 이은 받는 사람 사번
     * @param content 내용
     * @param user 로그인 사용자(보낸 사람)
     * @param model 뷰 모델
     * @return 결과 템플릿. 값이 없으면 잘못된 접근 화면
     */
    @PostMapping("/message/register")
    public String send(
            @RequestParam(required = false) String receiverId,
            @RequestParam(required = false) String content,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        if (receiverId == null || content == null) {
            return "redirect:/access-error";
        }
        boolean sent = messageService.send(
                user.id(), List.of(receiverId.split(";")), content);
        model.addAttribute("message", sent ? "쪽지를 전송했습니다." : "쪽지 전송을 실패했습니다.");
        return "message/result";
    }

    /**
     * 목록에서 선택 삭제. 처리 뒤 쪽지함으로 돌아간다.
     *
     * @param className receiver 또는 sender
     * @param messageId 삭제할 쪽지 번호 목록
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿. class 가 없으면 잘못된 접근 화면
     */
    @PostMapping("/message/delete")
    public String deleteFromList(
            @RequestParam(name = "class", required = false) String className,
            @RequestParam(required = false) List<Integer> messageId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        if (className == null || className.isBlank()) {
            return "redirect:/access-error";
        }
        boolean deleted = messageId != null && messageService.delete(user.id(), messageId);
        model.addAttribute("message", deleted ? "쪽지가 삭제되었습니다." : "선택된 쪽지가 없습니다.");
        model.addAttribute("nextPage", "/message?class=" + className);
        return "message/list-result";
    }

    /**
     * 읽기 팝업에서 삭제. 처리 뒤 여는 창을 새로 고치고 팝업을 닫는다.
     *
     * @param messageId 삭제할 쪽지 번호 목록
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿(팝업)
     */
    @PostMapping("/message/read-delete")
    public String deleteFromRead(
            @RequestParam(required = false) List<Integer> messageId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {
        boolean deleted = messageId != null && messageService.delete(user.id(), messageId);
        model.addAttribute("message", deleted ? "쪽지가 삭제되었습니다." : "선택된 쪽지가 없습니다.");
        return "message/result";
    }
}
