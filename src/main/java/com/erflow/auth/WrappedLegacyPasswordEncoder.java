package com.erflow.auth;

import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 레거시 해시를 bcrypt 로 겹씌운 형식 — 평문을 모르는 계정의 저장 형식을 승격한다.
 *
 * <p>레거시 형식({@link ErflowPasswordEncoder})은 salt 가 저장값 안에 그대로 들어 있고
 * 반복 10회의 SHA-256 이라 요즘 기준으로 약하다. bcrypt 로 바꾸고 싶어도 <b>평문을
 * 모르는 계정은 bcrypt 값을 만들 수 없다</b> — 그래서 저장돼 있던 레거시 해시 자체를
 * bcrypt 에 넣는다. DB 가 유출되어도 공격자는 bcrypt 부터 뚫어야 한다.
 *
 * <pre>
 * 저장 형태   salt(hex 40글자) + bcrypt(레거시 최종 해시 hex 64글자)
 * 검증        입력 평문을 salt 로 레거시 방식대로 늘인 뒤, 그 결과를 bcrypt 로 대조
 * </pre>
 *
 * <p>이 형식은 <b>과도기용</b>이다. 로그인이 성공하면 평문을 아는 순간이므로
 * {@link ErflowUserDetailsService} 가 순수 bcrypt 로 다시 저장한다. 새 비밀번호를
 * 이 형식으로 만드는 일은 없어야 하고, 그래서 {@link #encode} 는 막아 두었다.
 */
public class WrappedLegacyPasswordEncoder implements PasswordEncoder {

    /** {@code DelegatingPasswordEncoder} 저장 접두사({@code {erflow-bcrypt}})에 쓰는 이름. */
    public static final String ID = "erflow-bcrypt";

    private static final Pattern SALT_HEX = Pattern.compile("^[0-9a-f]{40}$");

    /** salt 가 차지하는 글자 수. 레거시 저장값과 같다. */
    private static final int SALT_CHARS = 40;

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    /**
     * 레거시 저장값을 이 형식으로 겹씌운다. 평문 없이 저장값만으로 만들 수 있다.
     *
     * @param legacyStored 레거시 형식의 저장값 — base64(salt 40 + 해시 hex 64)
     * @return salt + bcrypt(해시 hex)
     * @throws IllegalArgumentException 레거시 형식이 아닐 때
     */
    static String wrap(String legacyStored) {
        String decoded;
        try {
            decoded = new String(
                    Base64.getDecoder().decode(legacyStored),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("base64 가 아니라 레거시 저장값이 아니다", e);
        }
        if (decoded.length() != SALT_CHARS + 64 || !SALT_HEX.matcher(
                decoded.substring(0, SALT_CHARS)).matches()) {
            throw new IllegalArgumentException("레거시 저장 형태(salt 40 + 해시 64)가 아니다");
        }
        String salt = decoded.substring(0, SALT_CHARS);
        String innerHash = decoded.substring(SALT_CHARS);
        return salt + new BCryptPasswordEncoder().encode(innerHash);
    }

    /**
     * 새 비밀번호를 이 형식으로 만들지 않는다.
     *
     * <p>이 형식은 «이미 저장된 레거시 해시» 를 승격하는 자리다. 새로 저장할 비밀번호는
     * 평문을 아는 상태이므로 기본 형식(bcrypt)으로 가야 한다.
     *
     * @param rawPassword 평문 비밀번호
     * @return 반환하지 않는다
     */
    @Override
    public String encode(CharSequence rawPassword) {
        throw new UnsupportedOperationException(
                "겹씌움 형식으로 새 비밀번호를 만들지 않는다 — 기본 형식(bcrypt)을 쓴다");
    }

    /**
     * 평문이 저장값과 맞는지 확인한다.
     *
     * <p>저장값 앞의 salt 로 레거시 방식의 최종 해시를 재현하고, 그 해시를 bcrypt 로
     * 대조한다.
     *
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 저장값 (접두사 제외)
     * @return 일치하면 {@code true}
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null
                || encodedPassword.length() <= SALT_CHARS) {
            return false;
        }
        String salt = encodedPassword.substring(0, SALT_CHARS);
        if (!SALT_HEX.matcher(salt).matches()) {
            return false;
        }
        String innerHash = ErflowPasswordEncoder.stretchHex(rawPassword.toString(), salt);
        return bcrypt.matches(innerHash, encodedPassword.substring(SALT_CHARS));
    }
}
