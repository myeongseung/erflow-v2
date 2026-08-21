package com.erflow.product;

import java.util.Map;

/**
 * 제품 분류. 화면 셋이 이 값으로 갈린다.
 *
 * <p>출처: {@code ProductServiceImpl.getProducts} 의 {@code typekeys}/{@code typevalues},
 * {@code productRegister.jsp} 의 {@code switch (flag)}.
 *
 * <pre>
 * flag         코드   화면 글자   목록 화면
 * ingredient   0      원재료     /product/ingredient-product
 * processed    1      가공품     /product/processed-product
 * producted    2      완제품     /product/producted-product
 * </pre>
 *
 * <p>«완제품» 만 안내줄이 다르다 — 등록 화면이 그것만 «영업» 으로 적고 나머지는
 * «구매» 다. 목록 화면도 완제품만 «사용자 &gt; 제품 &gt; 완제품» 이다.
 */
public enum ProductType {

    /** 원재료. */
    INGREDIENT("ingredient", 0, "원재료"),

    /** 가공품. */
    PROCESSED("processed", 1, "가공품"),

    /** 완제품. 등록 화면 안내줄이 «영업» 으로 갈린다. */
    PRODUCTED("producted", 2, "완제품");

    private static final Map<String, ProductType> BY_FLAG = Map.of(
            "ingredient", INGREDIENT, "processed", PROCESSED, "producted", PRODUCTED);

    private final String flag;
    private final int code;
    private final String label;

    ProductType(String flag, int code, String label) {
        this.flag = flag;
        this.code = code;
        this.label = label;
    }

    /**
     * 화면이 넘기는 글자로 분류를 찾는다.
     *
     * @param flag {@code ingredient} / {@code processed} / {@code producted}
     * @return 분류. 아는 값이 아니면 {@code null}
     */
    public static ProductType of(String flag) {
        return flag == null ? null : BY_FLAG.get(flag.trim());
    }

    /**
     * @return 화면이 쓰는 글자
     */
    public String flag() {
        return flag;
    }

    /**
     * @return DB 에 담기는 코드
     */
    public int code() {
        return code;
    }

    /**
     * @return 화면에 보이는 이름
     */
    public String label() {
        return label;
    }
}
