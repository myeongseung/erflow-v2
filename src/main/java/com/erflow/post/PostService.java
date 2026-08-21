package com.erflow.post;

import com.erflow.common.Pagination;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 업무.
 *
 * <p>레거시는 페이징 계산과 줄마다의 부가 질의를 {@code postList.jsp} 스크립틀릿에서
 * 했다. 계산을 밖으로 꺼내 시험할 수 있게 한다.
 */
@Service
public class PostService {

    private final PostMapper postMapper;

    /**
     * @param postMapper 게시글 매퍼
     */
    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    /**
     * 게시글 목록 한 페이지.
     *
     * <p>화면에 찍히는 글번호는 실제 ID 가 아니라
     * {@code totalRecord - (start + i)} 다 — 레거시 그대로다. 전체 건수에서 위치를
     * 빼는 방식이라 검색 결과에서는 번호가 건너뛴다.
     *
     * @param boardId 게시판 번호
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public PostPage list(int boardId, PostSearch search, int requestedPage) {
        int total = postMapper.countBy(boardId, search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<PostRow> posts = postMapper.findPage(
                boardId, search, pagination.start(), pagination.numPerPage());

        List<PostListRow> rows = new ArrayList<>();
        for (int i = 0; i < posts.size(); ++i) {
            PostRow post = posts.get(i);
            rows.add(new PostListRow(
                    post,
                    total - (pagination.start() + i),
                    postMapper.countComments(post.id()),
                    postMapper.findFirstAttachmentName(post.id())));
        }
        return new PostPage(rows, pagination);
    }

    /**
     * 게시글 한 건을 읽는다.
     *
     * @param id 글번호
     * @return 게시글. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public PostRow get(int id) {
        return postMapper.findById(id);
    }

    /**
     * 조회수를 하나 올린다.
     *
     * <p>올릴지 말지는 {@link PostViewCounter} 가 정한다. 레거시의 판단이 뒤집혀
     * 있어 그 규칙을 따로 떼어 시험할 수 있게 했다.
     *
     * @param postId 글번호
     * @return 올렸으면 {@code true}
     */
    @Transactional
    public boolean incrementView(int postId) {
        return postMapper.incrementCount(postId) == 1;
    }

    /**
     * 새 글을 등록한다.
     *
     * <p>레거시 {@code createPost} 를 그대로 옮겼다. 게시판에서 가장 큰 스레드
     * 그룹 번호에 1 을 더해 새 그룹을 만들고, 등록 직후 자기 자신을 뿌리로
     * 가리키게 한다.
     *
     * @param boardId 게시판 번호
     * @param userId 작성자 사번
     * @param subject 제목
     * @param content 본문
     * @return 등록된 글번호. 실패하면 -1
     */
    @Transactional
    public int register(int boardId, String userId, String subject, String content) {
        Integer maxDepth = postMapper.findMaxDepth(boardId);
        int depth = (maxDepth == null ? 0 : maxDepth) + 1;

        PostWrite write = new PostWrite(userId, boardId, null, subject, content, depth, 0);
        if (postMapper.insert(write) != 1) {
            return -1;
        }
        postMapper.pointToSelf(write.getId());
        return write.getId();
    }

    /**
     * 답변글을 등록한다.
     *
     * <p>같은 스레드에서 뒤에 오는 글들을 한 칸씩 밀고 그 자리에 넣는다.
     *
     * <h2>레거시는 게시판 번호를 버린다</h2>
     *
     * <pre>
     * int boardId = 2; // 외래키오류때문에 임시
     * </pre>
     *
     * <p>{@code postReplyProc.jsp} 가 올바른 번호를 담아 넘기는데 서비스가 이 줄로
     * 덮어쓴다. 어느 게시판에 답변을 달든 자유게시판(2번)에 쌓인다. 옮기면서
     * 되살리지 않았다 — 넘어온 값을 쓴다. D-030 참조.
     *
     * @param parent 부모 글
     * @param userId 작성자 사번
     * @param subject 제목
     * @param content 본문
     * @return 등록되었으면 {@code true}
     */
    @Transactional
    public boolean reply(PostRow parent, String userId, String subject, String content) {
        if (parent == null) {
            return false;
        }
        int refId = parent.refId() == null ? parent.id() : parent.refId();
        postMapper.shiftPositions(refId, parent.pos());

        PostWrite write = new PostWrite(userId, parent.boardId(), refId,
                subject, content, parent.depth(), parent.pos() + 1);
        return postMapper.insertReply(write) == 1;
    }

    /**
     * 제목과 본문을 고친다.
     *
     * @param id 글번호
     * @param subject 제목
     * @param content 본문
     * @return 고쳐졌으면 {@code true}
     */
    @Transactional
    public boolean modify(int id, String subject, String content) {
        return postMapper.update(id, subject, content) == 1;
    }

    /**
     * 글을 지운다.
     *
     * <p>답변글이 달린 글은 지우지 않고 제목·본문을 «삭제된 글입니다.» 로 바꾸고
     * {@code delete} 를 1 로 세운다. 지우면 답변글이 스레드 뿌리를 잃는다.
     *
     * <p>스레드 건수에 자기 자신이 포함되므로 레거시는 1 이하일 때 진짜 삭제로
     * 본다. 그 경계를 그대로 쓴다.
     *
     * @param id 글번호
     * @return 처리되었으면 {@code true}
     */
    @Transactional
    public boolean remove(int id) {
        return postMapper.countInThread(id) <= 1
                ? postMapper.delete(id) == 1
                : postMapper.markDeleted(id) == 1;
    }

    /**
     * 게시글 목록 한 페이지.
     *
     * @param rows 게시글 줄
     * @param pagination 페이징
     */
    public record PostPage(List<PostListRow> rows, Pagination pagination) {
    }
}
