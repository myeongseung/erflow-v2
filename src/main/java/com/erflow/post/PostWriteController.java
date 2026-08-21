package com.erflow.post;

import com.erflow.auth.ErflowUserDetails;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 쓰기 — 등록·답변·수정·삭제와 첨부 내려받기.
 *
 * <pre>
 * postRegister.jsp      GET  /post/register
 * postRegisterProc.jsp  POST /post/register
 * postReply.jsp         GET  /post/reply
 * postReplyProc.jsp     POST /post/reply
 * postUpdate.jsp        GET  /post/update
 * postUpdateProc.jsp    POST /post/update
 * postDeleteProc.jsp    POST /post/delete
 * downloadFile.jsp      GET  /post/download
 * </pre>
 *
 * <p>레거시 처리 화면은 {@code alert} 후 목록으로 보낸다. 그 흐름을 그대로 둔다 —
 * 결과 화면을 새로 만들면 이관이 아니라 개선이다. 설비·협력업체에서 쓴
 * {@code popup-result} 와 같은 방식이다.
 */
@Controller
@RequestMapping("/post")
public class PostWriteController {

    private static final String PERMISSION_ERROR = "redirect:/permission-error";
    private static final String ACCESS_ERROR = "redirect:/access-error";
    private static final String RESULT = "post/result";

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
    public PostWriteController(
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
     * 글쓰기 화면.
     *
     * @param boardId 게시판 번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 등록 템플릿
     */
    @GetMapping("/register")
    public String registerForm(
            @RequestParam(defaultValue = "-1") int boardId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        Board board = writableBoard(boardId, user);
        if (board == null) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        model.addAttribute("boardId", boardId);
        model.addAttribute("board", board);
        return "post/register";
    }

    /**
     * 글쓰기 처리.
     *
     * @param boardId 게시판 번호
     * @param subject 제목
     * @param content 본문
     * @param filename 첨부
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register")
    public String register(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) List<MultipartFile> filename,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (writableBoard(boardId, user) == null) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        int postId = postService.register(boardId, user.id(), subject, content);
        if (postId != -1) {
            postFileService.attach(postId, filename);
        }
        return result(model, boardId,
                postId != -1 ? "게시물을 등록하였습니다." : "게시물을 등록하지 못했습니다.");
    }

    /**
     * 답변 화면.
     *
     * @param boardId 게시판 번호
     * @param postId 부모 글번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 답변 템플릿
     */
    @GetMapping("/reply")
    public String replyForm(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int postId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        Board board = writableBoard(boardId, user);
        if (board == null) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        model.addAttribute("boardId", boardId);
        model.addAttribute("board", board);
        model.addAttribute("postId", postId);
        return "post/reply";
    }

    /**
     * 답변 처리.
     *
     * @param boardId 게시판 번호
     * @param id 부모 글번호
     * @param subject 제목
     * @param content 본문
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/reply")
    public String reply(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int id,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String content,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (writableBoard(boardId, user) == null) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        PostRow parent = postService.get(id);
        boolean done = postService.reply(parent, user.id(), subject, content);
        return result(model, boardId,
                done ? "답변을 등록하였습니다." : "답변 등록을 실패하였습니다.");
    }

    /**
     * 글수정 화면.
     *
     * @param boardId 게시판 번호
     * @param id 글번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 수정 템플릿
     */
    @GetMapping("/update")
    public String updateForm(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int id,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        Board board = writableBoard(boardId, user);
        if (board == null) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        PostRow post = postService.get(id);
        if (post == null) {
            return ACCESS_ERROR;
        }
        model.addAttribute("boardId", boardId);
        model.addAttribute("board", board);
        model.addAttribute("post", post);
        model.addAttribute("attachments", postFileService.list(id));
        return "post/update";
    }

    /**
     * 글수정 처리.
     *
     * @param boardId 게시판 번호
     * @param id 글번호
     * @param subject 제목
     * @param content 본문
     * @param filename 추가 첨부
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update")
    public String update(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int id,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) List<MultipartFile> filename,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (writableBoard(boardId, user) == null) {
            return boardId == -1 ? ACCESS_ERROR : PERMISSION_ERROR;
        }
        boolean done = postService.modify(id, subject, content);
        if (done) {
            postFileService.attach(id, filename);
        }
        return result(model, boardId,
                done ? "게시물을 수정하였습니다." : "게시물을 수정하지 못했습니다.");
    }

    /**
     * 글삭제 처리.
     *
     * <p>레거시는 댓글 -> 첨부 -> 글 순서로 지운다. 순서를 바꾸면 참조가 남는다.
     *
     * @param boardId 게시판 번호
     * @param postId 글번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam(defaultValue = "-1") int boardId,
            @RequestParam(defaultValue = "-1") int postId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (boardId == -1 || postId == -1) {
            return ACCESS_ERROR;
        }
        if (writableBoard(boardId, user) == null) {
            return PERMISSION_ERROR;
        }
        commentService.removeAll(postId);
        postFileService.detachAll(postId);
        boolean done = postService.remove(postId);
        return result(model, boardId,
                done ? "게시물을 삭제하였습니다." : "게시물을 삭제하지 못했습니다.");
    }

    /**
     * 첨부 내려받기.
     *
     * <p>레거시는 요청에 실린 파일명을 그대로 저장 폴더에 붙여 찾았다. 여기서는
     * DB 에 등록된 이름만 받는다 — 등록되지 않은 이름은 파일이 있어도 내려가지
     * 않는다. D-028 참조.
     *
     * @param file 저장 파일명 (UUID)
     * @param user 로그인 사용자
     * @return 파일 응답. 없으면 404
     * @throws IOException 파일을 읽을 수 없을 때
     */
    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<Resource> download(
            @RequestParam(required = false) String file,
            @AuthenticationPrincipal ErflowUserDetails user) throws IOException {

        PostAttachment attachment = postFileService.find(file);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        PostRow post = postService.get(attachment.postId());
        Board board = post == null ? null : boardService.get(post.boardId());
        if (!BoardPermissions.canRead(user.deptPermission(), user.jobPermission(), board)) {
            return ResponseEntity.status(403).build();
        }
        Path path = postFileService.locate(attachment);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(attachment.displayName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .body(new FileSystemResource(path));
    }

    /**
     * 쓰기 권한이 있는 게시판을 돌려준다.
     *
     * @param boardId 게시판 번호
     * @param user 로그인 사용자
     * @return 게시판. 번호가 없거나 권한이 없으면 {@code null}
     */
    private Board writableBoard(int boardId, ErflowUserDetails user) {
        if (boardId == -1) {
            return null;
        }
        Board board = boardService.get(boardId);
        return BoardPermissions.canWrite(user.deptPermission(), user.jobPermission(), board)
                ? board : null;
    }

    /**
     * 결과 화면. 레거시는 처리 후 게시글 목록으로 보냈다.
     *
     * @param model 뷰 모델
     * @param boardId 게시판 번호
     * @param message alert 로 띄울 문구
     * @return 결과 템플릿
     */
    private String result(Model model, int boardId, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", "/post/list?boardId=" + boardId);
        return RESULT;
    }
}
