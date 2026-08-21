package com.erflow.product;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제품 찾기 팝업이 쓰는 조회.
 *
 * <p>팝업이 둘이다. 하나는 눌러서 한 제품을 고르고({@code findProduct.jsp}), 하나는
 * 표에서 여럿을 고른다({@code findMultiProduct.jsp}). 걸러내는 규칙은 같다.
 */
@Service
public class ProductFinder {

    private final ProductMapper productMapper;

    /**
     * @param productMapper 제품 매퍼
     */
    public ProductFinder(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 검색어에 걸리는 제품.
     *
     * <p>코드와 이름 중 하나라도 검색어를 품으면 걸린다. {@code like} 가 아니라
     * <b>부분 문자열</b>이며 대소문자를 가린다. 검색어가 비면 전부 나온다.
     *
     * <p>협력업체 팝업은 같은 자리에서 코드를 훑지 않는다(D-039). 두 화면이 겉모습이
     * 같아 놓치기 쉬운 차이다.
     *
     * @param search 검색어. {@code null} 이면 빈 문자열로 본다
     * @return 이름 순으로 정렬한 제품 목록
     */
    @Transactional(readOnly = true)
    public List<ProductRow> search(String search) {
        String needle = search == null ? "" : search;

        return productMapper.findAllNames().stream()
                .filter(product -> needle.isEmpty()
                        || product.id().contains(needle)
                        || product.name().contains(needle))
                // 레거시도 이름만으로 정렬한다. 이름이 같은 두 제품의 앞뒤는 레거시가
                // HashMap 순서로 정해 재현할 수 없다 — 여기서는 DB 가 주는 순서다.
                .sorted(Comparator.comparing(ProductRow::name))
                .toList();
    }
}
