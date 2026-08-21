package com.erflow.bound;

import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입·출고 업무.
 *
 * <p>레거시는 페이징 계산을 {@code inbound.jsp}/{@code outbound.jsp} 스크립틀릿에,
 * DB 접근을 {@code BoundServiceImpl} 에 두었다. 화면에서 로직을 걷어내 여기로 모은다.
 */
@Service
public class BoundService {

    /** 입고. 레거시 {@code type} 0. */
    public static final int INBOUND = 0;

    /** 출고. 레거시 {@code type} 1. */
    public static final int OUTBOUND = 1;

    private final BoundMapper boundMapper;

    /**
     * @param boundMapper 입·출고 매퍼
     */
    public BoundService(BoundMapper boundMapper) {
        this.boundMapper = boundMapper;
    }

    /**
     * 목록 한 페이지와 페이징 정보.
     *
     * @param type 0 입고 / 1 출고
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public BoundPage list(int type, BoundSearch search, int requestedPage) {
        int total = boundMapper.countBy(type, search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<BoundRow> rows = boundMapper.findPage(
                type, search, pagination.start(), pagination.numPerPage());
        return new BoundPage(rows, pagination);
    }

    /**
     * 입·출고 한 건을 등록한다.
     *
     * @param bound 등록할 입·출고
     * @return 들어갔으면 {@code true}
     */
    @Transactional
    public boolean create(Bound bound) {
        return boundMapper.insert(bound) == 1;
    }

    /**
     * 수정 화면용으로 한 건을 읽는다.
     *
     * @param id 번호
     * @param type 0 입고 / 1 출고
     * @return 입·출고. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public BoundDetail get(int id, int type) {
        return boundMapper.findForUpdate(id, type);
    }

    /**
     * 본체를 수정한다.
     *
     * @param bound 수정 값
     * @return 수정됐으면 {@code true}
     */
    @Transactional
    public boolean update(BoundUpdate bound) {
        return boundMapper.update(bound) == 1;
    }

    /**
     * 선택된 입·출고를 지운다. type 이 맞아야 지워진다(입고 목록에서 고른 것은 입고만).
     *
     * @param ids 지울 번호 목록
     * @param type 0 입고 / 1 출고
     * @return 전부 지워졌으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Integer> ids, int type) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean all = true;
        for (int id : ids) {
            all &= boundMapper.delete(id, type) == 1;
        }
        return all;
    }

    /**
     * 목록 한 페이지.
     *
     * @param rows 이 페이지의 입·출고 목록
     * @param pagination 페이징 정보
     */
    public record BoundPage(List<BoundRow> rows, Pagination pagination) {
    }
}
