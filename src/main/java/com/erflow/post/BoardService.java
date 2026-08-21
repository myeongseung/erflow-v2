package com.erflow.post;

import com.erflow.common.Pagination;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 업무.
 *
 * <p>레거시는 게시판 목록 화면 스크립틀릿에서 게시판마다 글 수와 최신글을 다시
 * 질의했다. 질의 횟수를 줄이면 결과가 같아도 순서나 경계가 달라질 수 있어, 호출을
 * 서비스로 옮기기만 하고 질의 구조는 그대로 둔다.
 */
@Service
public class BoardService {

    private final BoardMapper boardMapper;
    private final PostMapper postMapper;

    /**
     * @param boardMapper 게시판 매퍼
     * @param postMapper 게시글 매퍼
     */
    public BoardService(BoardMapper boardMapper, PostMapper postMapper) {
        this.boardMapper = boardMapper;
        this.postMapper = postMapper;
    }

    /**
     * 게시판 목록 한 페이지.
     *
     * <p>레거시는 {@code start} 를 계산해 두고 쓰지 않아 몇 페이지를 눌러도 0번부터
     * 15건이 나왔다(D-025 — 게시판이 4개뿐이라 드러나지 않았다). 2단계에서
     * 고쳤다(D-108) — {@code pagination.start()} 부터 자른다. 목록 순서는 레거시
     * 그대로 두었다({@code ORDER BY} 를 더하면 지금 화면의 순서가 달라진다).
     *
     * @param keyword 검색어
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public BoardPage list(String keyword, int requestedPage) {
        int total = boardMapper.countBy(keyword);
        Pagination pagination = Pagination.of(total, requestedPage);

        List<Board> boards = boardMapper.findAll(keyword);
        List<BoardRow> rows = new ArrayList<>();
        for (int i = pagination.start();
                i < pagination.start() + pagination.numPerPage() && i < boards.size(); ++i) {
            Board board = boards.get(i);
            int postCount = postMapper.countBy(board.id(), PostSearch.none());
            List<PostRow> recent =
                    postMapper.findPage(board.id(), PostSearch.none(), 0, 1);
            PostRow head = recent.isEmpty() ? null : recent.get(0);
            rows.add(new BoardRow(
                    board.id(),
                    board.subject(),
                    postCount,
                    head == null ? null : head.id(),
                    head == null ? null : head.subject(),
                    head == null ? null : head.createdAt()));
        }
        return new BoardPage(rows, pagination);
    }

    /**
     * 게시판 한 건을 읽는다.
     *
     * @param id 게시판 번호
     * @return 게시판. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public Board get(int id) {
        return boardMapper.findById(id);
    }

    /**
     * 게시판 목록 한 페이지.
     *
     * @param rows 게시판 줄
     * @param pagination 페이징
     */
    public record BoardPage(List<BoardRow> rows, Pagination pagination) {
    }
}
