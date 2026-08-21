package com.erflow.post;

import com.erflow.auth.ErflowUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 게시판 화면.
 *
 * <p>레거시 라우팅은 {@code migration/design/00-decisions.md} 의 D-005 규칙으로 옮겼다.
 *
 * <pre>
 * boardList.jsp    GET  /post/board-list
 * postList.jsp     GET  /post/list?boardId=N
 * </pre>
 *
 * <h2>권한이 두 겹이다</h2>
 *
 * <p>{@code /post/board-list} 는 프로그램 권한으로 막힌다 — {@code screen} 테이블에
 * 행이 있어 {@link com.erflow.auth.ScreenAuthorizationManager} 가 먼저 판정한다.
 *
 * <p>{@code /post/list} 는 프로그램 권한 대상이 아니다. 레거시
 * {@code postList.jsp} 에도 {@code PROGRAM_CODE} 가 없고, 대신 게시판마다 다른
 * 읽기 마스크를 본다. 그래서 이 판정은 여기서 한다 — 게시판 번호를 알아야 하므로
 * 경로만 보는 인가 계층에서는 할 수 없다.
 */
@Controller
@RequestMapping("/post")
public class PostController {

    private static final String PERMISSION_ERROR = "redirect:/permission-error";
    private static final String ACCESS_ERROR = "redirect:/access-error";

    private final BoardService boardService;
    private final PostService postService;

    /**
     * @param boardService 게시판 업무
     * @param postService 게시글 업무
     */
    public PostController(BoardService boardService, PostService postService) {
        this.boardService = boardService;
        this.postService = postService;
    }

    /**
     * 게시판 목록.
     *
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/board-list")
    public String boardList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        BoardService.BoardPage page = boardService.list(keyword, nowPage);
        model.addAttribute("boards", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "post/board-list";
    }

    /**
     * 게시글 목록.
     *
     * <p>레거시는 {@code boardId} 가 없거나 {@code -1} 이면 접근 오류로 보냈고,
     * 게시판 읽기 권한이 없으면 권한 오류로 보냈다. 두 갈래를 그대로 둔다 —
     * 하나로 합치면 "게시판이 없다"와 "볼 수 없다"가 구별되지 않는다.
     *
     * @param boardId 게시판 번호
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (boardId == -1) {
            return ACCESS_ERROR;
        }
        Board board = boardService.get(boardId);
        if (!BoardPermissions.canRead(user.deptPermission(), user.jobPermission(), board)) {
            return PERMISSION_ERROR;
        }

        PostSearch search = new PostSearch(keyfield, keyword);
        PostService.PostPage page = postService.list(boardId, search, nowPage);

        model.addAttribute("board", board);
        model.addAttribute("posts", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("boardId", boardId);
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "post/list";
    }
}
