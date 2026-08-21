package com.erflow.admin.board;

import com.erflow.common.Pagination;
import com.erflow.post.PostSearch;
import com.erflow.post.PostService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 게시판 관리 화면.
 *
 * <pre>
 * adminBoardList.jsp     GET  /admin/board/list
 * boardRegister.jsp      GET  /admin/board/register        (팝업)
 * boardRegisterProc      POST /admin/board/register-proc
 * boardUpdate.jsp        GET  /admin/board/update?boardId= (팝업)
 * boardUpdateProc        POST /admin/board/update-proc
 * boardDeleteProc        POST /admin/board/delete-proc
 * boardDeptUpdate.jsp    GET  /admin/board/dept-update?id=&amp;flag=
 * boardDeptUpdateProc    POST /admin/board/dept-update-proc
 * boardJobUpdate.jsp     GET  /admin/board/job-update?id=&amp;flag=
 * boardJobUpdateProc     POST /admin/board/job-update-proc
 * postDeleteProc         POST /admin/board/post-delete-proc
 * </pre>
 *
 * <p>목록 화면 하나에 표가 둘이다 — 왼쪽이 게시판, 오른쪽이 <b>고른 게시판의 글</b>.
 * 페이징도 검색도 두 벌이며 숨은 {@code readFrm} 하나가 그 값을 모두 들고 다닌다.
 */
@Controller
@RequestMapping("/admin/board")
public class AdminBoardController {

    private static final String LIST = "/admin/board/list";

    private static final String RESULT = "admin/board/result";

    private final AdminBoardService boardService;

    private final PostService postService;

    /**
     * @param boardService 게시판 관리 업무
     * @param postService 게시글 업무. 오른쪽 표가 그대로 쓴다
     */
    public AdminBoardController(AdminBoardService boardService, PostService postService) {
        this.boardService = boardService;
        this.postService = postService;
    }

    /**
     * 게시판 관리 목록.
     *
     * @param board 게시판 이름 검색어
     * @param boardId 오른쪽 표에 띄울 게시판. 없으면 공지사항(1)
     * @param keyfield 글 검색 대상
     * @param keyword 글 검색어
     * @param nowBoard 게시판 목록 페이지
     * @param nowPage 게시글 목록 페이지
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String board,
            @RequestParam(defaultValue = "1") int boardId,
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowBoard,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        String boardName = board == null ? "" : board;
        PostSearch search = new PostSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);

        AdminBoardService.BoardPage boards = boardService.list(boardName, nowBoard);
        PostService.PostPage posts = postService.list(boardId, search, nowPage);

        model.addAttribute("boards", boards.rows());
        model.addAttribute("boardPage", boards.pagination());
        model.addAttribute("posts", posts.rows());
        model.addAttribute("postPage", posts.pagination());
        // 게시글 쪽 페이징 «블록» 은 레거시가 nowPage 가 아니라 nowBoard 로 계산한다.
        // 왼쪽 목록을 2페이지로 넘기면 오른쪽 페이지 번호가 함께 밀린다(D-067).
        model.addAttribute("postBlock",
                Pagination.of(posts.pagination().totalRecord(), nowBoard));
        model.addAttribute("boardName", boardName);
        model.addAttribute("boardId", boardId);
        model.addAttribute("keyfield", search.keyfield());
        model.addAttribute("keyword", search.keyword());
        return "admin/board/list";
    }

    /**
     * 게시판 생성 팝업.
     *
     * @return 등록 템플릿
     */
    @GetMapping("/register")
    public String registerForm() {
        return "admin/board/register";
    }

    /**
     * 게시판 생성 처리.
     *
     * @param subject 게시판 이름
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/register-proc")
    public String register(@RequestParam(required = false) String subject, Model model) {
        if (subject == null) {
            return "redirect:/access-error";
        }
        return popup(model, boardService.create(subject)
                ? "게시판을 등록하였습니다." : "게시판을 등록하지 못했습니다.");
    }

    /**
     * 게시판 수정 팝업.
     *
     * @param boardId 게시판 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/update")
    public String updateForm(@RequestParam(required = false) Integer boardId, Model model) {
        var board = boardId == null ? null : boardService.get(boardId);
        if (board == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("boardId", board.id());
        model.addAttribute("subject", board.subject());
        return "admin/board/update";
    }

    /**
     * 게시판 수정 처리.
     *
     * @param id 게시판 번호
     * @param subject 새 이름
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/update-proc")
    public String update(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String subject,
            Model model) {

        if (id == null || subject == null || subject.trim().isEmpty()) {
            return "redirect:/access-error";
        }
        return popup(model, boardService.update(id, subject)
                ? "게시판을 수정하였습니다." : "게시판을 수정하지 못했습니다.");
    }

    /**
     * 선택한 게시판 삭제. 그 안의 글·댓글·첨부도 함께 지운다.
     *
     * @param boardId 지울 게시판 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete-proc")
    public String delete(@RequestParam(required = false) List<Integer> boardId, Model model) {
        if (boardId == null) {
            return result(model, "삭제할 게시판을 선택해주세요.", LIST);
        }
        return result(model, boardService.delete(boardId)
                ? "게시판을 삭제하였습니다." : "게시판을 삭제하지 못했습니다.", LIST);
    }

    /**
     * 선택한 게시글 삭제.
     *
     * <p>돌아갈 곳에 게시판 번호를 달고 간다 — 지운 뒤 그 게시판이 그대로 보여야 한다.
     *
     * @param boardId 지금 보고 있는 게시판
     * @param postId 지울 글번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/post-delete-proc")
    public String deletePosts(
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false) List<Integer> postId,
            Model model) {

        if (boardId == null) {
            return "redirect:/access-error";
        }
        String next = LIST + "?boardId=" + boardId;
        if (postId == null) {
            return result(model, "삭제할 게시글을 선택해주세요.", next);
        }
        return result(model, boardService.deletePosts(postId)
                ? "게시글을 삭제하였습니다." : "게시글을 삭제하지 못했습니다.", next);
    }

    /**
     * 게시판 부서 권한 수정 화면.
     *
     * @param id 게시판 번호
     * @param flag {@code read} 또는 {@code write}
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/dept-update")
    public String deptForm(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String flag,
            Model model) {
        return permissionForm(id, flag, false, model, "admin/board/dept-update");
    }

    /**
     * 게시판 부서 권한 수정 처리.
     *
     * @param boardId 게시판 번호
     * @param flag {@code read} 또는 {@code write}
     * @param permissions 체크된 부서 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/dept-update-proc")
    public String deptUpdate(
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) List<Integer> permissions,
            Model model) {
        return permissionUpdate(boardId, flag, false, permissions, model, "부서");
    }

    /**
     * 게시판 직급 권한 수정 화면.
     *
     * @param id 게시판 번호
     * @param flag {@code read} 또는 {@code write}
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/job-update")
    public String jobForm(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String flag,
            Model model) {
        return permissionForm(id, flag, true, model, "admin/board/job-update");
    }

    /**
     * 게시판 직급 권한 수정 처리.
     *
     * @param boardId 게시판 번호
     * @param flag {@code read} 또는 {@code write}
     * @param permissions 체크된 직급 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/job-update-proc")
    public String jobUpdate(
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) List<Integer> permissions,
            Model model) {
        return permissionUpdate(boardId, flag, true, permissions, model, "직급");
    }

    private String permissionForm(
            Integer id, String flag, boolean job, Model model, String view) {

        if (id == null || !isFlag(flag)) {
            return "redirect:/access-error";
        }
        AdminBoardService.PermissionForm form =
                boardService.permissionForm(id, "write".equals(flag), job);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("form", form);
        model.addAttribute("flag", flag);
        return view;
    }

    private String permissionUpdate(
            Integer boardId, String flag, boolean job, List<Integer> permissions,
            Model model, String label) {

        if (boardId == null || !isFlag(flag)) {
            return "redirect:/access-error";
        }
        boolean updated = boardService.updatePermission(
                boardId, "write".equals(flag), job, permissions);
        return result(model, updated
                        ? label + " 권한 정보를 수정했습니다." : label + " 권한 정보를 수정하지 못했습니다.",
                LIST);
    }

    private static boolean isFlag(String flag) {
        return "read".equals(flag) || "write".equals(flag);
    }

    private String result(Model model, String message, String nextPage) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", nextPage);
        model.addAttribute("closePopup", false);
        return RESULT;
    }

    private String popup(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", LIST);
        model.addAttribute("closePopup", true);
        return RESULT;
    }
}
