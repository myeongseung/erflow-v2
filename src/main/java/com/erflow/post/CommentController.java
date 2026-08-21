package com.erflow.post;

import com.erflow.auth.ErflowUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 댓글 처리.
 *
 * <pre>
 * commentRegisterProc.jsp  POST /post/comment/register
 * commentReplyProc.jsp     POST /post/comment/reply
 * commentUpdateProc.jsp    POST /post/comment/update
 * commentDeleteProc.jsp    POST /post/comment/delete
 * </pre>
 *
 * <h2>삭제가 GET 이었다</h2>
 *
 * <p>레거시 {@code commentDeleteProc.jsp} 는 링크로 걸려 있어 GET 으로 지워졌다.
 * 여기서는 POST 로 받는다 — 이관하며 CSRF 방어를 켰고(D-013), GET 으로 두면 그
 * 방어가 댓글 삭제에만 비켜 간다. 화면에 보이는 것은 달라지지 않는다. D-031 참조.
 *
 * <h2>권한 판정은 레거시 그대로다</h2>
 *
 * <p>본인 댓글이거나 관리자면 지울 수 있다. 수정은 본인만 할 수 있다.
 * 레거시가 화면에서 버튼을 감추는 것과 같은 조건을 서버에서도 본다 —
 * 감추기만 하면 요청을 직접 보내는 것은 막지 못한다.
 */
@Controller
@RequestMapping("/post/comment")
public class CommentController {

    private static final String PERMISSION_ERROR = "redirect:/permission-error";
    private static final String ACCESS_ERROR = "redirect:/access-error";
    private static final String RESULT = "post/result";

    private final BoardService boardService;
    private final CommentService commentService;

    /**
     * @param boardService 게시판 업무
     * @param commentService 댓글 업무
     */
    public CommentController(BoardService boardService, CommentService commentService) {
        this.boardService = boardService;
        this.commentService = commentService;
    }

    /**
     * 댓글 등록.
     *
     * @param boardId 게시판 번호
     * @param postId 글번호
     * @param comment 내용
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register")
    public String register(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int postId,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (!canRead(boardId, user)) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        boolean done = commentService.register(postId, user.id(), comment);
        return result(model, boardId, postId,
                done ? "댓글을 등록하였습니다." : "댓글을 등록하지 못했습니다.");
    }

    /**
     * 답글 등록.
     *
     * @param boardId 게시판 번호
     * @param postId 글번호
     * @param refId 부모 댓글 번호
     * @param comment 내용
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/reply")
    public String reply(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int postId,
            @RequestParam(defaultValue = "-1") int refId,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (!canRead(boardId, user)) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        boolean done = commentService.reply(postId, refId, user.id(), comment);
        return result(model, boardId, postId,
                done ? "답글을 등록하였습니다." : "답글을 등록하지 못했습니다.");
    }

    /**
     * 댓글 수정. 본인만 할 수 있다.
     *
     * @param boardId 게시판 번호
     * @param postId 글번호
     * @param id 댓글 번호
     * @param comment 내용
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update")
    public String update(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int postId,
            @RequestParam(defaultValue = "-1") int id,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (!canRead(boardId, user)) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        CommentRow target = commentService.get(id);
        if (target == null || !target.userId().equals(user.id())) {
            return PERMISSION_ERROR;
        }
        boolean done = commentService.modify(id, comment);
        return result(model, boardId, postId,
                done ? "댓글을 수정하였습니다." : "댓글을 수정하지 못했습니다.");
    }

    /**
     * 댓글 삭제. 본인이거나 관리자만 할 수 있다.
     *
     * @param boardId 게시판 번호
     * @param postId 글번호
     * @param id 댓글 번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int postId,
            @RequestParam(defaultValue = "-1") int id,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (!canRead(boardId, user)) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        CommentRow target = commentService.get(id);
        if (target == null || !(target.userId().equals(user.id()) || user.admin())) {
            return PERMISSION_ERROR;
        }
        boolean done = commentService.remove(id);
        return result(model, boardId, postId,
                done ? "댓글을 삭제하였습니다." : "댓글을 삭제하지 못했습니다.");
    }

    private boolean canRead(int boardId, ErflowUserDetails user) {
        return boardId != -1 && BoardPermissions.canRead(
                user.deptPermission(), user.jobPermission(), boardService.get(boardId));
    }

    /**
     * 결과 화면. 레거시 댓글 처리는 보던 글로 돌아간다.
     *
     * @param model 뷰 모델
     * @param boardId 게시판 번호
     * @param postId 글번호
     * @param message alert 로 띄울 문구
     * @return 결과 템플릿
     */
    private String result(Model model, int boardId, int postId, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", "/post/view?boardId=" + boardId + "&id=" + postId);
        return RESULT;
    }
}
