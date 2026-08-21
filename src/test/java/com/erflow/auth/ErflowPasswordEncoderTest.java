package com.erflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 레거시 해시 재현 — 기대값 고정 시험.
 *
 * <p>아래 기대값은 레거시 알고리즘을 별도로 구현해 뽑은 것이며, <b>합성 입력</b>이다.
 * 운영 데이터의 해시를 소스에 박지 않는다 — 공개 저장소이고, 약한 해시라 더욱 그렇다.
 * 실제 사용자 비밀번호와의 호환은 {@link LegacyPasswordCompatibilityTest} 가 DB 를
 * 직접 보고 확인한다.
 *
 * <p>이 시험이 깨지면 기존 사용자가 로그인하지 못한다.
 */
class ErflowPasswordEncoderTest {

    private final ErflowPasswordEncoder encoder = new ErflowPasswordEncoder();

    @Test
    @DisplayName("고정 salt 로 레거시와 같은 값을 만든다")
    void knownAnswer() {
        assertThat(ErflowPasswordEncoder.generate(
                        "erflow", "0123456789abcdef0123456789abcdef01234567"))
                .isEqualTo("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2NzVkN2UyZTA4MTc4"
                        + "MzNkMDJiODhmM2MzM2M1YmRlNTBlZTIyMmQ1ZjU2YmFiZGZiZTY5ZWRkMGE3NzI3ZWU4YWY=");
    }

    @Test
    @DisplayName("한글 비밀번호도 UTF-8 로 같은 값을 만든다")
    void knownAnswerWithHangul() {
        // 레거시는 플랫폼 기본 문자셋에 기댔다. UTF-8 전제를 여기 고정한다.
        assertThat(ErflowPasswordEncoder.generate(
                        "비밀번호", "ffffffffffffffffffffffffffffffffffffffff"))
                .isEqualTo("ZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmQ3ZjBhMDQyODMz"
                        + "Y2YzMWJjMjU3MjBjYWZiNzQ2NDAxYzE4ZWY0ZjI5ZTA2NjIzMWFhMjJkM2YzY2Y5NTA2OTg=");
    }

    @Test
    @DisplayName("저장 형태가 레거시와 같다 — base64(salt 40 + 해시 64)")
    void encodedShapeMatchesLegacy() {
        String decoded = new String(
                Base64.getDecoder().decode(encoder.encode("어떤비밀번호")), StandardCharsets.UTF_8);

        assertThat(decoded).hasSize(104);
        assertThat(decoded.substring(0, 40)).matches("[0-9a-f]{40}");
        assertThat(decoded.substring(40)).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("새로 만든 값도 스스로 검증된다")
    void encodeThenMatch() {
        String encoded = encoder.encode("비밀번호123");
        assertThat(encoder.matches("비밀번호123", encoded)).isTrue();
        assertThat(encoder.matches("비밀번호124", encoded)).isFalse();
    }

    @Test
    @DisplayName("매번 다른 salt 를 쓴다")
    void saltIsRandom() {
        assertThat(encoder.encode("같은비밀번호")).isNotEqualTo(encoder.encode("같은비밀번호"));
    }

    @Test
    @DisplayName("망가진 저장값에 예외를 던지지 않는다")
    void handlesMalformedInput() {
        assertThat(encoder.matches("x", null)).isFalse();
        assertThat(encoder.matches("x", "")).isFalse();
        assertThat(encoder.matches("x", "base64 아님!!")).isFalse();
        assertThat(encoder.matches("x",
                        Base64.getEncoder().encodeToString("짧음".getBytes(StandardCharsets.UTF_8))))
                .as("salt 40글자가 안 되는 값")
                .isFalse();
    }
}
