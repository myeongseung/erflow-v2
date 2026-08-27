package com.erflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 주민등록번호 가리기(D-117).
 *
 * <p>가리는 규칙은 «뒷자리 한 자리만 남긴다» 하나다. 그 한 자리가 성별·세기 코드라
 * 표가 보여 주던 성별·내외국인·나이를 잃지 않는다.
 *
 * <p>여기 쓰인 번호는 지어낸 것이다. 실제 값은 시험에 쓰지 않는다.
 */
class SocialNumbersTest {

    @Test
    @DisplayName("뒷자리 일곱 중 한 자리만 남는다")
    void keepsOneTailDigit() {
        assertThat(SocialNumbers.masked("990115-1234567")).isEqualTo("990115-1******");
    }

    @Test
    @DisplayName("글자 수가 그대로 열넷이다 — 표의 칸이 흔들리지 않는다")
    void keepsWidth() {
        assertThat(SocialNumbers.masked("990115-1234567")).hasSize(14);
    }

    @Test
    @DisplayName("하이픈이 없어도 같은 모양으로 나온다")
    void normalisesMissingHyphen() {
        assertThat(SocialNumbers.masked("9901151234567")).isEqualTo("990115-1******");
    }

    @Test
    @DisplayName("남는 한 자리는 성별·세기 코드라 성별을 그대로 읽을 수 있다")
    void tailDigitStillTellsGender() {
        assertThat(SocialNumbers.masked("990115-2234567")).isEqualTo("990115-2******");
        assertThat(SocialNumbers.masked("040115-3234567")).isEqualTo("040115-3******");
    }

    @Test
    @DisplayName("없는 값은 없는 그대로 — 별표로 채우면 있는 것처럼 보인다")
    void emptyStaysEmpty() {
        assertThat(SocialNumbers.masked(null)).isNull();
        assertThat(SocialNumbers.masked("")).isEmpty();
        assertThat(SocialNumbers.masked("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("모양이 다른 값은 통째로 가린다 — 어디가 뒷자리인지 모른다")
    void unknownShapeIsFullyHidden() {
        assertThat(SocialNumbers.masked("990115-12345")).isEqualTo("************");
        assertThat(SocialNumbers.masked("9901151234567890")).isEqualTo("****************");
        assertThat(SocialNumbers.masked("모르는값")).isEqualTo("****");
    }

    @Test
    @DisplayName("가린 값에는 뒷자리 여섯이 남지 않는다")
    void tailIsGone() {
        String masked = SocialNumbers.masked("990115-1234567");

        assertThat(masked).doesNotContain("234567").doesNotContain("1234567");
    }
}
