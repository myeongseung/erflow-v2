package com.erflow.company;

import com.erflow.common.CodeTable;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 협력업체 화면이 쓰는 코드표.
 *
 * <p>레거시 {@code FieldCodeRepository} 와 {@code BankCodeRepository} 에 대응한다.
 * 둘 다 소스에 박혀 있던 것을 자원 파일로 옮겼다({@link CodeTable} 참조).
 */
@Component
public class CompanyCodes {

    private final CodeTable fields = CodeTable.load("data/field-codes.properties");
    private final CodeTable banks = CodeTable.load("data/bank-codes.properties");

    /**
     * 업종코드의 이름.
     *
     * @param code 업종코드
     * @return 업종명. 없으면 {@code null}
     */
    public String fieldName(String code) {
        return fields.name(code);
    }

    /**
     * 은행코드의 이름.
     *
     * @param code 은행코드
     * @return 은행명. 없으면 {@code null}
     */
    public String bankName(String code) {
        return banks.name(code);
    }

    /**
     * 업종명에 검색어가 들어간 업종코드를 모은다.
     *
     * @param keyword 검색어
     * @return 걸리는 업종코드 목록
     */
    public List<String> fieldsMatching(String keyword) {
        return fields.matching(keyword);
    }

    /**
     * @return 업종코드 수
     */
    public int fieldCount() {
        return fields.size();
    }

    /**
     * @return 은행코드 수
     */
    /**
     * 업종 코드표 자체. 찾기 팝업이 코드와 이름을 함께 훑어야 한다.
     *
     * @return 업종 코드표
     */
    public CodeTable fieldTable() {
        return fields;
    }

    /**
     * 은행 코드표 자체.
     *
     * @return 은행 코드표
     */
    public CodeTable bankTable() {
        return banks;
    }

    public int bankCount() {
        return banks.size();
    }
}
