package com.erflow.company;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 코드표.
 *
 * <p>기대값은 레거시 {@code FieldCodeRepository} / {@code BankCodeRepository} 의 실제
 * 항목이다. 자원 파일이 원본에서 제대로 뽑혔는지, 조회 방식이 같은지를 본다.
 */
class CompanyCodesTest {

    private final CompanyCodes codes = new CompanyCodes();

    @Test
    @DisplayName("레거시가 갖고 있던 만큼 담고 있다")
    void sizesMatchLegacy() {
        assertThat(codes.fieldCount()).as("업종코드").isEqualTo(156);
        assertThat(codes.bankCount()).as("은행코드").isEqualTo(73);
    }

    @Test
    @DisplayName("코드로 이름을 푼다")
    void resolvesNames() {
        assertThat(codes.fieldName("032902")).isEqualTo("특수 목적용 기계 제조업");
        assertThat(codes.bankName("001")).isEqualTo("한국은행");
    }

    @Test
    @DisplayName("없는 코드는 null 이다")
    void unknownCodeIsNull() {
        // 레거시는 Optional.ofNullable(...).orElse("") 로 감쌌다. 감싸는 일은 화면 몫이다.
        assertThat(codes.fieldName("없는코드")).isNull();
        assertThat(codes.fieldName(null)).isNull();
        assertThat(codes.bankName("999999")).isNull();
    }

    @Test
    @DisplayName("업종명 부분 일치로 코드를 모은다")
    void matchesByPartialName() {
        // 레거시 getFieldCodes(name) 은 contains 였다.
        assertThat(codes.fieldsMatching("제조업")).hasSizeGreaterThan(10);
        assertThat(codes.fieldsMatching("특수 목적용 기계 제조업")).containsExactly("032902");
    }

    @Test
    @DisplayName("걸리는 업종이 없으면 빈 목록이다")
    void noMatchIsEmpty() {
        // 레거시는 여기서 빈 IN () 을 만들다 예외로 죽었다 (D-017).
        assertThat(codes.fieldsMatching("존재하지않는업종")).isEmpty();
        assertThat(codes.fieldsMatching("")).isEmpty();
        assertThat(codes.fieldsMatching(null)).isEmpty();
    }
}
