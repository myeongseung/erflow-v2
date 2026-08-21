package com.erflow.post;

import com.erflow.auth.ErflowUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 게시글 보기.
 *
 * <pre>
 * postView.jsp  GET /post/view?boardId=N&amp;id=M
 * </pre>
 *
 * <p>조회수 집계가 여기 붙는다. 판단 규칙은 {@link PostViewCounter} 로 떼어 두었다 —
 * 레거시 조건이 뒤집혀 있어, 코드에 섞여 있으면 그대로 옮겼는지 확인할 수 없다.
 */
@Controller
@RequestMapping("/post")
public class PostViewController {

    private static final String PERMISSION_ERROR = "redirect:/permission-error";
    private static final String ACCESS_ERROR = "redirect:/access-error";

    /** 레거시 {@code cookie.setMaxAge(60*60*1)}. 한 시간. */
    private static final int COOKIE_MAX_AGE = 60 * 60;

    private final BoardService boardService;
    private final PostService postService;
    private final CommentService commentService;
    private final PostFileService postFileService;

    /**
     * @param boardService 게시판 업무
     * @param postService 게시글 업무
     * @param commentService 댓글 업무
     * @param postFileService 첨부 업무
     */
    public PostViewController(
            BoardService boardService,
            PostService postService,
            CommentService commentService,
            PostFileService postFileService) {
        this.boardService = boardService;
        this.postService = postService;
        this.commentService = commentService;
        this.postFileService = postFileService;
    }

    /**
     * 게시글 보기.
     *
     * @param boardId 게시판 번호
     * @param id 글번호
     * @param seenCookie 이미 본 글 번호가 쌓인 쿠키
     * @param user 로그인 사용자
     * @param response 쿠키를 실어 보낼 응답
     * @param model 뷰 모델
     * @return 보기 템플릿
     */
    @GetMapping("/view")
    public String view(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int id,
            @CookieValue(name = PostViewCounter.COOKIE_NAME, required = false) String seenCookie,
            @AuthenticationPrincipal ErflowUserDetails user,
            HttpServletResponse response,
            Model model) {

        if (boardId == -1 || id == -1) {
            return ACCESS_ERROR;
        }
        Board board = boardService.get(boardId);
        if (!BoardPermissions.canRead(user.deptPermission(), user.jobPermission(), board)) {
            return PERMISSION_ERROR;
        }
        PostRow post = postService.get(id);
        if (post == null) {
            return ACCESS_ERROR;
        }

        // 글쓴이 본인이 열면 조회수를 세지 않는다. 레거시 isCreatedUser 분기다.
        boolean mine = post.userId() != null && post.userId().equals(user.id());
        if (!mine && countView(seenCookie, id, response)) {
            post = postService.get(id);
        }

        List<CommentRow> comments = commentService.list(id);
        model.addAttribute("board", board);
        model.addAttribute("boardId", boardId);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentCount", comments.size());
        model.addAttribute("attachments", postFileService.list(id));
        model.addAttribute("mine", mine);
        model.addAttribute("admin", user.admin());
        model.addAttribute("currentUserName", user.name());
        model.addAttribute("currentUserId", user.id());
        return "post/view";
    }

    /**
     * 조회수 집계. 레거시 쿠키 처리를 그대로 옮긴다.
     *
     * @param seenCookie 지금 쿠키 값
     * @param postId 글번호
     * @param response 쿠키를 실어 보낼 응답
     * @return 조회수를 올렸으면 {@code true}
     */
    private boolean countView(String seenCookie, int postId, HttpServletResponse response) {
        if (!PostViewCounter.shouldIncrement(seenCookie, postId)) {
            return false;
        }
        postService.incrementView(postId);

        Cookie cookie = new Cookie(
                PostViewCounter.COOKIE_NAME, PostViewCounter.nextValue(seenCookie, postId));
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        // 레거시에는 없던 표시다. 이 쿠키를 스크립트가 읽을 이유가 없다.
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return true;
    }
}
