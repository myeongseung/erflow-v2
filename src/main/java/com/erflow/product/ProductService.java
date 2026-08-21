package com.erflow.product;

import com.erflow.common.Pagination;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제품 업무.
 *
 * <p>원재료·가공품·완제품이 <b>한 표</b>에 {@code type} 으로 나뉘어 있다. 화면 셋도 같은
 * 조회에 코드만 다르게 넘긴다.
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;

    /**
     * @param productMapper 제품 매퍼
     */
    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 분류별 제품 목록 한 페이지.
     *
     * @param type 분류
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public ProductPage list(ProductType type, ProductSearch search, int requestedPage) {
        int total = productMapper.countBy(type.code(), search);
        Pagination pagination = Pagination.of(total, requestedPage);
        return new ProductPage(
                productMapper.findPage(
                        type.code(), search, pagination.start(), pagination.numPerPage()),
                pagination);
    }

    /**
     * 제품 한 건.
     *
     * @param id 제품 ID
     * @return 제품. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public ProductListRow get(String id) {
        return productMapper.findById(id);
    }

    /**
     * 제품을 만든다.
     *
     * <p>레거시는 예외를 삼키고 «등록에 실패했습니다» 를 띄웠다. 이미 있는 제품 ID 가
     * 그 길로 간다 — 제품 ID 는 기본키다.
     *
     * @param id 제품 ID
     * @param name 이름
     * @param count 수량
     * @param type 분류
     * @return 만들었으면 {@code true}
     */
    @Transactional
    public boolean create(String id, String name, int count, ProductType type) {
        try {
            return productMapper.insertProduct(id, name, count, type.code()) == 1;
        } catch (DataIntegrityViolationException expected) {
            return false;
        }
    }

    /**
     * 제품을 고친다.
     *
     * @param id 제품 ID
     * @param name 이름
     * @param count 수량
     * @param type 분류
     * @return 고쳤으면 {@code true}
     */
    @Transactional
    public boolean update(String id, String name, int count, ProductType type) {
        return productMapper.updateProduct(id, name, count, type.code()) == 1;
    }

    /**
     * 제품들을 지운다.
     *
     * @param ids 지울 제품 ID 들
     * @return 전부 지웠으면 {@code true}
     */
    @Transactional
    public boolean delete(List<String> ids) {
        boolean result = true;
        for (String id : ids) {
            result &= productMapper.deleteProduct(id) == 1;
        }
        return result;
    }

    /**
     * 제품 목록 한 페이지.
     *
     * @param rows 제품 줄
     * @param pagination 페이징 정보
     */
    public record ProductPage(List<ProductListRow> rows, Pagination pagination) {
    }
}
