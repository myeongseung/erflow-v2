package com.erflow.unit;

import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설비 업무.
 *
 * <p>레거시는 이 로직이 두 군데 흩어져 있었다 — 페이징 계산과 파라미터 정리는
 * {@code unitList.jsp} / {@code unitRegisterProc.jsp} 스크립틀릿에, DB 접근은
 * {@code UnitServiceImpl} 에. 화면에서 로직을 걷어내 여기로 모은다.
 */
@Service
public class UnitService {

    private final UnitMapper unitMapper;

    /**
     * @param unitMapper 설비 매퍼
     */
    public UnitService(UnitMapper unitMapper) {
        this.unitMapper = unitMapper;
    }

    /**
     * 목록 한 페이지와 페이징 정보를 함께 돌려준다.
     *
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public UnitPage list(UnitSearch search, int requestedPage) {
        int total = unitMapper.countBy(search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<UnitRow> rows =
                unitMapper.findPage(search, pagination.start(), pagination.numPerPage());
        return new UnitPage(rows, pagination);
    }

    /**
     * 설비 한 건을 읽는다.
     *
     * @param id 장비ID
     * @return 설비. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public Unit get(String id) {
        return id == null ? null : unitMapper.findById(id);
    }

    /**
     * 설비를 등록한다.
     *
     * @param unit 등록할 설비
     * @return 등록되었으면 {@code true}
     */
    @Transactional
    public boolean create(Unit unit) {
        return unitMapper.insert(unit) > 0;
    }

    /**
     * 설비를 수정한다.
     *
     * @param unit 수정할 설비
     * @return 수정되었으면 {@code true}
     */
    @Transactional
    public boolean update(Unit unit) {
        return unitMapper.update(unit) > 0;
    }

    /**
     * 선택된 설비를 지운다.
     *
     * <p>레거시는 한 건씩 지우면서 결과를 {@code &=} 로 모았다. 한 건이라도 실패하면
     * 실패로 보고하되 나머지 삭제는 그대로 진행했다는 뜻이다. 그 동작을 유지한다.
     *
     * @param ids 지울 장비ID 목록
     * @return 전부 지워졌으면 {@code true}
     */
    @Transactional
    public boolean delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean all = true;
        for (String id : ids) {
            all &= unitMapper.delete(id) > 0;
        }
        return all;
    }

    /**
     * 목록 한 페이지.
     *
     * @param rows 이 페이지의 설비 목록
     * @param pagination 페이징 정보
     */
    public record UnitPage(List<UnitRow> rows, Pagination pagination) {
    }
}
