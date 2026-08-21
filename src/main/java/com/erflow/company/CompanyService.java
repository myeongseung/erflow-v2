package com.erflow.company;

import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 협력업체 업무.
 *
 * <p>구매(1)와 영업(0)이 같은 화면을 쓰고 {@code subcontract} 로 갈린다. 권한도
 * 그렇게 갈리므로({@code migration/design/00-decisions.md} D-016) 이 값을 임의로
 * 기본값 처리하지 않는다.
 */
@Service
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final CompanyCodes codes;

    /**
     * @param companyMapper 협력업체 매퍼
     * @param codes 업종·은행 코드표
     */
    public CompanyService(CompanyMapper companyMapper, CompanyCodes codes) {
        this.companyMapper = companyMapper;
        this.codes = codes;
    }

    /**
     * 화면에서 넘어온 검색 값을 조건으로 바꾼다.
     *
     * <p>업종 검색이면 업종명에 검색어가 들어간 코드를 미리 푼다. 레거시도 SQL 을
     * 만들기 전에 같은 일을 했다.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @return 검색 조건
     */
    public CompanySearch toSearch(String keyfield, String keyword) {
        List<String> matched = CompanySearch.BY_FIELD.equals(keyfield)
                ? codes.fieldsMatching(keyword)
                : List.of();
        return CompanySearch.of(keyfield, keyword, matched);
    }

    /**
     * 목록 한 페이지와 페이징 정보를 함께 돌려준다.
     *
     * @param subcontract 0 영업 / 1 구매
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public CompanyPage list(int subcontract, CompanySearch search, int requestedPage) {
        int total = companyMapper.countBy(subcontract, search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<Company> rows =
                companyMapper.findPage(subcontract, search, pagination.start(), pagination.numPerPage());
        return new CompanyPage(rows, pagination);
    }

    /**
     * 협력업체 한 건을 읽는다.
     *
     * @param id 번호
     * @return 협력업체. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public Company get(int id) {
        return companyMapper.findById(id);
    }

    /**
     * 업종코드의 이름.
     *
     * @param code 업종코드
     * @return 업종명. 없으면 {@code null}
     */
    public String fieldName(String code) {
        return codes.fieldName(code);
    }

    /**
     * 은행코드의 이름.
     *
     * @param code 은행코드
     * @return 은행명. 없으면 {@code null}
     */
    public String bankName(String code) {
        return codes.bankName(code);
    }

    /**
     * 협력업체를 등록한다.
     *
     * @param company 등록할 협력업체
     * @return 등록되었으면 {@code true}
     */
    @Transactional
    public boolean create(Company company) {
        return companyMapper.insert(company) > 0;
    }

    /**
     * 협력업체를 수정한다.
     *
     * @param company 수정할 협력업체
     * @return 수정되었으면 {@code true}
     */
    @Transactional
    public boolean update(Company company) {
        return companyMapper.update(company) > 0;
    }

    /**
     * 선택된 협력업체를 지운다.
     *
     * <p>레거시는 한 건씩 지우며 결과를 모았다. 한 건이라도 실패하면 실패로 보고하되
     * 나머지 삭제는 그대로 진행했다.
     *
     * @param ids 지울 번호 목록
     * @return 전부 지워졌으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean all = true;
        for (Integer id : ids) {
            all &= companyMapper.delete(id) > 0;
        }
        return all;
    }

    /**
     * 목록 한 페이지.
     *
     * @param rows 이 페이지의 협력업체 목록
     * @param pagination 페이징 정보
     */
    public record CompanyPage(List<Company> rows, Pagination pagination) {
    }
}
