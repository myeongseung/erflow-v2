package com.erflow.document;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 찾기 팝업이 쓰는 조회.
 *
 * <p>레거시는 이 걸러내기를 화면 스크립틀릿에서 했다. 조건이 둘이라 눈으로 읽고
 * 넘기기 쉬운데, 하나는 결함이다({@link #search} 설명 참조).
 */
@Service
public class DocumentFinder {

    private final DocumentMapper documentMapper;

    /**
     * @param documentMapper 문서 매퍼
     */
    public DocumentFinder(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /**
     * 결재에 아직 쓰이지 않은 내 문서 중 검색어에 걸리는 것.
     *
     * <h2>검색어를 숫자로 읽어 보고, 안 되면 이름으로 찾는다</h2>
     *
     * <p>레거시 조건은 {@code searchKey == key || value.contains(search)} 다.
     * {@code searchKey} 는 검색어를 {@code Long} 으로 바꾼 값이고 실패하면 -1 로
     * 남는다. 즉 숫자를 넣으면 문서번호가 정확히 같은 것을, 글자를 넣으면 문서명에
     * 그 글자가 든 것을 찾는다. {@code like} 가 아니라 <b>부분 문자열</b>이며
     * 대소문자를 가린다.
     *
     * <h2>검색어가 비면 전부 나온다</h2>
     *
     * <p>{@code "".contains("")} 는 참이다. 그래서 창을 처음 열면 결재에 쓰이지 않은
     * 내 문서가 전부 뜬다. 은행·업종 팝업이 도움말을 보여주는 것과 다르다 —
     * 그쪽은 검색어가 {@code null} 이면 도움말인데, 이 화면은 {@code null} 이 될 수
     * 없게 만들어 두고 {@code null} 인지 검사한다. D-036 참조.
     *
     * @param userId 사번
     * @param search 검색어. {@code null} 이면 빈 문자열로 본다
     * @return 걸리는 문서 목록
     */
    @Transactional(readOnly = true)
    public List<DocumentRow> search(String userId, String search) {
        String needle = search == null ? "" : search;
        long wanted = asId(needle);

        return documentMapper.findByUser(userId).stream()
                .filter(document -> documentMapper.countProposals(document.id()) == 0)
                .filter(document -> wanted == document.id()
                        || document.subject().contains(needle))
                .toList();
    }

    /**
     * 검색어를 문서번호로 읽어 본다.
     *
     * @param search 검색어
     * @return 숫자면 그 값, 아니면 -1. 레거시가 실패 시 -1 로 두는 것과 같다
     */
    private static long asId(String search) {
        try {
            return Long.parseLong(search);
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }
}
