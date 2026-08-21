package com.erflow.company;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 협력업체 찾기 팝업이 쓰는 조회.
 *
 * <p>레거시는 이 걸러내기를 화면 스크립틀릿에서 했다. 전부 읽어 와서 이름으로
 * 정렬해 두고 조건에 걸리는 것만 찍는다.
 */
@Service
public class CompanyFinder {

    private final CompanyMapper companyMapper;

    /**
     * @param companyMapper 협력업체 매퍼
     */
    public CompanyFinder(CompanyMapper companyMapper) {
        this.companyMapper = companyMapper;
    }

    /**
     * 검색어에 걸리는 협력업체.
     *
     * <h2>번호로는 찾지 못한다</h2>
     *
     * <p>도움말이 «협력업체 ID — 예) 1 -&gt; 삼성» 이라고 안내하지만 레거시 조건은
     * {@code value.contains(search)} 뿐이다. 번호를 넣으면 그 번호를 이름에 품은
     * 업체만 나온다. 은행·업종·제품 팝업은 같은 자리에서 코드도 훑는다 — 이 화면만
     * 빠졌다. 그대로 옮긴다. D-039 참조.
     *
     * <h2>검색어가 비면 전부 나온다</h2>
     *
     * <p>레거시가 {@code search.equals("")} 를 따로 보고 조건을 건너뛴다. 검색창을
     * 비운 채 누른 것과 창을 처음 연 것은 다르다 — 처음 열면 도움말이다.
     *
     * @param search 검색어. {@code null} 이면 빈 문자열로 본다
     * @return 이름 순으로 정렬한 협력업체 목록
     */
    @Transactional(readOnly = true)
    public List<CompanyRow> search(String search) {
        String needle = search == null ? "" : search;

        return companyMapper.findAllNames().stream()
                .filter(company -> needle.isEmpty() || company.name().contains(needle))
                // 레거시는 이름으로만 정렬한다. 이름이 같은 두 업체의 앞뒤는
                // HashMap 순서가 정하므로 재현할 수 없다 — 여기서는 번호 순이다.
                .sorted(Comparator.comparing(CompanyRow::name))
                .toList();
    }
}
