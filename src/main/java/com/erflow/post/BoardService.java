package com.erflow.post;

import com.erflow.common.Pagination;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 게시판 목록 업무.
 *
 * <p>목록 한 쪽은 게시판 전부를 읽어 쪽만큼 잘라 낸 뒤, 그 쪽의 글 수와 최신 글을
 * <b>한 번에</b> 읽는다(D-154). 게시판마다 따로 물으면 DB 왕복이 둘씩 는다.
 */
@Service
public class BoardService {

    private final BoardMapper boardMapper;

    /**
     * @param boardMapper 게시판 매퍼
     */
    public BoardService(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    /**
     * 게시판 목록 한 쪽 — 글 수와 최신 글을 달고.
     *
     * @param keyword 게시판 이름 검색어. {@code null} 이거나 비면 전체
     * @param requestedPage 요청된 쪽
     * @return 목록과 쪽 번호
     */
    public BoardPage list(String keyword, int requestedPage) {
        List<Board> boards = boardMapper.findAll(keyword);
        Pagination pagination = Pagination.of(boards.size(), requestedPage);
        int from = Math.min(pagination.start(), boards.size());
        int to = Math.min(pagination.start() + pagination.numPerPage(), boards.size());
        List<Board> page = boards.subList(from, to);

        Map<Integer, BoardSummary> summaries = new HashMap<>();
        if (!page.isEmpty()) {
            for (BoardSummary summary : boardMapper.findSummaries(
                    page.stream().map(Board::id).toList())) {
                summaries.put(summary.boardId(), summary);
            }
        }

        List<BoardRow> rows = new ArrayList<>(page.size());
        for (Board board : page) {
            BoardSummary summary = summaries.get(board.id());
            rows.add(new BoardRow(
                    board.id(),
                    board.subject(),
                    summary == null ? 0 : summary.postCount(),
                    summary == null ? null : summary.recentPostId(),
                    summary == null ? null : summary.recentSubject(),
                    summary == null ? null : summary.recentCreatedAt()));
        }
        return new BoardPage(rows, pagination);
    }

    /**
     * 게시판 한 건.
     *
     * @param id 게시판 번호
     * @return 게시판. 없으면 {@code null}
     */
    public Board get(int id) {
        return boardMapper.findById(id);
    }

    /**
     * 목록 한 쪽.
     *
     * @param rows 게시판 줄들
     * @param pagination 쪽 번호
     */
    public record BoardPage(List<BoardRow> rows, Pagination pagination) {
    }
}
