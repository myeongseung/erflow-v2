package com.erflow.index;

import com.erflow.message.MessageMapper;
import com.erflow.message.MessageRow;
import com.erflow.message.MessageSearch;
import com.erflow.post.PostMapper;
import com.erflow.post.PostRow;
import com.erflow.post.PostSearch;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메인 화면 업무.
 *
 * <p>위젯 넷은 각자 도메인의 조회를 그대로 부른다. 메인 화면만의 조회는 결재 하나뿐이다.
 *
 * <h2>자르는 규칙이 위젯마다 다르다</h2>
 *
 * <pre>
 * 공지사항     제목 10자
 * 자유게시판   제목 10자 + 작성자 3자
 * 받은 쪽지함  내용 10자
 * 전자결재     자르지 않는다
 * </pre>
 *
 * <p>나란히 놓인 두 게시판 위젯이 작성자만 다르게 자른다. 레거시가 그렇게 적어 두었고
 * 그대로 옮긴다(D-082).
 */
@Service
public class IndexService {

    /** 위젯마다 보여주는 건수. 레거시가 넷 다 15로 고정해 두었다. */
    private static final int WIDGET_SIZE = 15;

    /** 공지사항 게시판 번호. */
    static final int NOTICE_BOARD = 1;

    /** 자유게시판 번호. */
    static final int FREE_BOARD = 2;

    private static final int SUBJECT_LIMIT = 10;
    private static final int NAME_LIMIT = 3;
    private static final int CONTENT_LIMIT = 10;

    /** 레거시가 화면에 찍던 모양. {@code post_view} 가 글자로 주던 값이다. */
    private static final DateTimeFormatter SHOWN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PostMapper postMapper;
    private final MessageMapper messageMapper;
    private final IndexMapper indexMapper;

    /**
     * @param postMapper 게시글 조회
     * @param messageMapper 쪽지 조회
     * @param indexMapper 최근 결재 조회
     */
    public IndexService(PostMapper postMapper, MessageMapper messageMapper,
            IndexMapper indexMapper) {
        this.postMapper = postMapper;
        this.messageMapper = messageMapper;
        this.indexMapper = indexMapper;
    }

    /**
     * 게시판 위젯 한 벌.
     *
     * <p>번호는 <b>전체 건수에서 줄 순서를 뺀 값</b>이다. 그래서 첫 줄이 가장 큰 번호를
     * 갖고, 글이 지워지면 남은 글의 번호가 함께 밀린다 — 글번호가 아니라 «몇 번째로
     * 최신인가» 다.
     *
     * @param boardId 게시판 번호
     * @param cutName 작성자 이름도 자를지. 자유게시판만 자른다
     * @return 위젯에 그릴 줄
     */
    @Transactional(readOnly = true)
    public List<IndexPostRow> posts(int boardId, boolean cutName) {
        List<PostRow> found =
                postMapper.findPage(boardId, PostSearch.none(), 0, WIDGET_SIZE);
        int total = postMapper.countBy(boardId, PostSearch.none());

        List<IndexPostRow> rows = new ArrayList<>();
        for (int i = 0; i < found.size(); ++i) {
            PostRow post = found.get(i);
            String name = cutName ? cut(post.name(), NAME_LIMIT) : post.name();
            rows.add(new IndexPostRow(
                    total - i,
                    post.id(),
                    boardId,
                    cut(post.subject(), SUBJECT_LIMIT),
                    name,
                    post.userId(),
                    post.createdAt() == null ? null : post.createdAt().format(SHOWN),
                    post.count(),
                    postMapper.countComments(post.id())));
        }
        return rows;
    }

    /**
     * 전자결재 위젯.
     *
     * @param userId 사번
     * @return 위젯에 그릴 줄
     */
    @Transactional(readOnly = true)
    public List<IndexProposalRow> proposals(String userId) {
        return indexMapper.findRecentProposals(userId, 0, WIDGET_SIZE);
    }

    /**
     * 받은 쪽지함 위젯.
     *
     * @param userId 사번
     * @return 위젯에 그릴 줄
     */
    @Transactional(readOnly = true)
    public List<IndexMessageRow> messages(String userId) {
        MessageSearch search = new MessageSearch(null, null);
        List<MessageRow> found =
                messageMapper.findPage("receiver", userId, search, 0, WIDGET_SIZE);
        int total = messageMapper.countBy("receiver", userId, search);

        List<IndexMessageRow> rows = new ArrayList<>();
        for (int i = 0; i < found.size(); ++i) {
            MessageRow message = found.get(i);
            rows.add(new IndexMessageRow(
                    total - i,
                    message.id(),
                    message.senderLabel(),
                    cut(message.content(), CONTENT_LIMIT),
                    message.createdAt()));
        }
        return rows;
    }

    /**
     * 글자를 잘라 «...» 을 붙인다.
     *
     * <p>레거시가 위젯마다 {@code substring(0, n) + "..."} 을 되풀이한다. 길이만 다르다.
     *
     * @param text 원래 글자
     * @param limit 넘으면 자르는 길이
     * @return 잘린 글자. 짧으면 그대로
     */
    static String cut(String text, int limit) {
        return text != null && text.length() > limit
                ? text.substring(0, limit) + "..."
                : text;
    }
}
