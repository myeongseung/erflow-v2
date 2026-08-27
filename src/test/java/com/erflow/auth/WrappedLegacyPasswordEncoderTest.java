package com.erflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 겹씌움 형식 — 평문 없이 레거시 저장값만으로 bcrypt 로 승격하고, 로그인 때 평문으로
 * 검증된다는 것을 못 박는다 (D-128).
 */
class WrappedLegacyPasswordEncoderTest {

    private static final String SALT = "0123456789abcdef0123456789abcdef01234567";

    private final WrappedLegacyPasswordEncoder encoder = new WrappedLegacyPasswordEncoder();

    @Test
    @DisplayName("레거시 저장값을 겹씌우면 원래 평문으로 검증된다")
    void wrapThenMatch() {
        String legacyStored = ErflowPasswordEncoder.generate("비밀번호123", SALT);
        String wrapped = WrappedLegacyPasswordEncoder.wrap(legacyStored);

        assertThat(encoder.matches("비밀번호123", wrapped)).isTrue();
        assertThat(encoder.matches("비밀번호124", wrapped)).isFalse();
    }

    @Test
    @DisplayName("겹씌운 값의 모양 — salt 40글자 뒤에 bcrypt")
    void wrappedShape() {
        String wrapped = WrappedLegacyPasswordEncoder.wrap(
                ErflowPasswordEncoder.generate("pw", SALT));

        assertThat(wrapped.substring(0, 40)).isEqualTo(SALT);
        assertThat(wrapped.substring(40)).startsWith("$2");
    }

    @Test
    @DisplayName("레거시 형식이 아닌 값은 겹씌우지 못한다")
    void wrapRejectsMalformed() {
        assertThatThrownBy(() -> WrappedLegacyPasswordEncoder.wrap("base64 아님!!"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WrappedLegacyPasswordEncoder.wrap(
                        Base64.getEncoder().encodeToString(
                                "짧음".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("새 비밀번호를 이 형식으로 만들 수 없다 — 새 저장은 bcrypt 로 간다")
    void encodeIsBlocked() {
        assertThatThrownBy(() -> encoder.encode("새비밀번호"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("망가진 저장값에 예외를 던지지 않는다")
    void handlesMalformedStoredValue() {
        assertThat(encoder.matches("x", null)).isFalse();
        assertThat(encoder.matches("x", "")).isFalse();
        assertThat(encoder.matches("x", "짧은값")).isFalse();
        assertThat(encoder.matches("x", "z".repeat(40) + "$2a$10$abc")).isFalse();
        assertThat(encoder.matches(null, SALT + "$2a$10$abc")).isFalse();
    }
}
