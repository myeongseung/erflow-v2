package com.erflow.admin.board;

import com.erflow.admin.permission.AdminPermissionMapper;
import com.erflow.admin.permission.Levels;
import com.erflow.admin.permission.PermissionChoice;
import com.erflow.admin.permission.PermissionRow;
import com.erflow.auth.Permissions;
import com.erflow.common.Pagination;
import com.erflow.post.Board;
import com.erflow.post.BoardMapper;
import com.erflow.post.CommentMapper;
import com.erflow.post.PostFileService;
import com.erflow.post.PostService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 관리 업무.
 *
 * <p>게시판 권한도 부서·직급 비트마스크다. 프로그램 권한과 같은 규칙으로 다룬다 —
 * <b>관리자 비트로 시작</b>해서 체크된 것을 얹고, 계산은 Java 에서 한다.
 * {@code migration/design/02-permission-bitmask.md} 참조.
 */
@Service
public class AdminBoardService {

    private final AdminBoardMapper adminBoardMapper;

    private final BoardMapper boardMapper;

    private final AdminPermissionMapper permissionMapper;

    private final PostService postService;

    private final CommentMapper commentMapper;

    private final PostFileService postFileService;

    /**
     * @param adminBoardMapper 게시판 관리 매퍼
     * @param boardMapper 게시판 조회 매퍼(권한 네 칸을 읽는다)
     * @param permissionMapper 부서·직급 권한 매퍼
     * @param postService 게시글 업무. 목록과 삭제를 그대로 쓴다
     * @param commentMapper 댓글 매퍼
     * @param postFileService 첨부 업무. 파일까지 지우므로 매퍼가 아니라 이쪽을 부른다(D-132)
     */
    public AdminBoardService(
            AdminBoardMapper adminBoardMapper,
            BoardMapper boardMapper,
            AdminPermissionMapper permissionMapper,
            PostService postService,
            CommentMapper commentMapper,
            PostFileService postFileService) {
        this.adminBoardMapper = adminBoardMapper;
        this.boardMapper = boardMapper;
        this.permissionMapper = permissionMapper;
        this.postService = postService;
        this.commentMapper = commentMapper;
        this.postFileService = postFileService;
    }

    /**
     * 게시판 목록 한 페이지.
     *
     * @param keyword 게시판 이름 검색어
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public BoardPage list(String keyword, int requestedPage) {
        Pagination pagination = Pagination.of(adminBoardMapper.countBy(keyword), requestedPage);
        return new BoardPage(
                adminBoardMapper.findPage(keyword, pagination.start(), pagination.numPerPage()),
                pagination);
    }

    /**
     * 게시판 하나.
     *
     * @param id 게시판 번호
     * @return 게시판. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public Board get(int id) {
        return boardMapper.findById(id);
    }

    /**
     * 게시판을 만든다.
     *
     * <p>권한 네 칸이 <b>관리자 비트</b>로 시작한다. 새 게시판은 관리자만 읽고 쓸 수
     * 있고, 권한 수정 화면에서 열어 준다.
     *
     * @param subject 게시판 이름
     * @return 만들었으면 {@code true}. 같은 이름이 있으면 {@code false}
     */
    @Transactional
    public boolean create(String subject) {
        if (hasName(subject)) {
            return false;
        }
        return adminBoardMapper.insertBoard(subject, Permissions.ADMIN_BIT) == 1;
    }

    /**
     * 게시판 이름을 고친다.
     *
     * <p>같은 이름이 이미 있으면 고치지 않는다 — 자기 이름을 그대로 둔 경우는 예외다.
     *
     * @param id 게시판 번호
     * @param subject 새 이름
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean update(int id, String subject) {
        Board board = boardMapper.findById(id);
        if (board == null || (!board.subject().equals(subject) && hasName(subject))) {
            return false;
        }
        return adminBoardMapper.updateBoard(id, subject) == 1;
    }

    /**
     * 게시판들을 지운다. 그 안의 글·댓글·첨부도 함께 지운다.
     *
     * <p>레거시는 이 여러 삭제를 트랜잭션 없이 던졌다. 중간에 끊기면 글이 반쯤 지워진
     * 게시판이 남는다. 한 트랜잭션으로 묶는다(D-043·D-051·D-061 과 같은 판단).
     *
     * <p>디스크의 첨부 파일은 지우지 않는다 — 레거시도 DB 행만 지운다(O-009).
     *
     * @param boardIds 지울 게시판 번호들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Integer> boardIds) {
        boolean result = true;
        for (int boardId : boardIds) {
            for (Integer postId : adminBoardMapper.findPostIds(boardId)) {
                removePost(postId);
            }
            result &= adminBoardMapper.deleteBoard(boardId) == 1;
        }
        return result;
    }

    /**
     * 게시글들을 지운다. 댓글과 첨부도 함께 지운다.
     *
     * @param postIds 지울 글번호들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean deletePosts(List<Integer> postIds) {
        boolean result = true;
        for (int postId : postIds) {
            result &= removePost(postId);
        }
        return result;
    }

    /**
     * 게시판 권한 수정 화면 한 벌.
     *
     * @param boardId 게시판 번호
     * @param write 쓰기 권한이면 {@code true}, 읽기면 {@code false}
     * @param job 직급 권한이면 {@code true}, 부서면 {@code false}
     * @return 화면 한 벌. 게시판이 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public PermissionForm permissionForm(int boardId, boolean write, boolean job) {
        Board board = boardMapper.findById(boardId);
        if (board == null) {
            return null;
        }
        long level = levelOf(board, write, job);
        List<PermissionRow> all = job
                ? permissionMapper.findJobPermissions(null)
                : permissionMapper.findDeptPermissions(null);

        List<PermissionChoice> choices = new ArrayList<>();
        for (PermissionRow row : all) {
            choices.add(new PermissionChoice(
                    row.classId(), row.name(), Levels.has(level, row.level())));
        }
        return new PermissionForm(boardId, choices);
    }

    /**
     * 게시판 권한을 바꾼다.
     *
     * <p>고치지 않는 쪽(부서 화면이면 직급)은 <b>읽어 온 값을 그대로 되돌려 놓는다.</b>
     * 갱신 문장이 두 칸을 함께 쓰기 때문이며, 되돌려 놓지 않으면 반대쪽 권한이 지워진다.
     *
     * @param boardId 게시판 번호
     * @param write 쓰기 권한이면 {@code true}
     * @param job 직급 권한이면 {@code true}
     * @param checked 체크된 부서 또는 직급 번호들
     * @return 바꿨으면 {@code true}
     */
    @Transactional
    public boolean updatePermission(
            int boardId, boolean write, boolean job, List<Integer> checked) {

        Board board = boardMapper.findById(boardId);
        if (board == null) {
            return false;
        }
        List<PermissionRow> all = job
                ? permissionMapper.findJobPermissions(null)
                : permissionMapper.findDeptPermissions(null);

        List<Long> levels = new ArrayList<>();
        for (PermissionRow row : all) {
            if (checked != null && checked.contains(row.classId())) {
                levels.add(row.level());
            }
        }
        // 프로그램 권한과 같은 규칙 — 관리자 비트로 시작한다.
        long changed = Levels.combine(Permissions.ADMIN_BIT, levels);
        long dept = job ? levelOf(board, write, false) : changed;
        long jobLevel = job ? changed : levelOf(board, write, true);

        return (write
                ? adminBoardMapper.updateWritePermission(boardId, dept, jobLevel)
                : adminBoardMapper.updateReadPermission(boardId, dept, jobLevel)) == 1;
    }

    private boolean removePost(int postId) {
        commentMapper.deleteByPost(postId);
        postFileService.detachAll(postId);
        return postService.remove(postId);
    }

    private boolean hasName(String subject) {
        // 레거시는 count == 1 로 본다. 같은 이름이 둘이면 «없다» 가 된다.
        return adminBoardMapper.countByName(subject) == 1;
    }

    private static long levelOf(Board board, boolean write, boolean job) {
        if (write) {
            return job ? board.writeJobLevel() : board.writeDeptLevel();
        }
        return job ? board.readJobLevel() : board.readDeptLevel();
    }

    /**
     * 게시판 목록 한 페이지.
     *
     * @param rows 게시판 줄
     * @param pagination 페이징 정보
     */
    public record BoardPage(List<AdminBoardRow> rows, Pagination pagination) {
    }

    /**
     * 게시판 권한 수정 화면 한 벌.
     *
     * @param boardId 게시판 번호
     * @param choices 부서 또는 직급 체크박스
     */
    public record PermissionForm(int boardId, List<PermissionChoice> choices) {
    }
}
