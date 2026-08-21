package com.erflow.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 레거시 비밀번호 해시를 그대로 재현하는 인코더.
 *
 * <p>출처: {@code service/implementation/PasswordHashServiceImpl}.
 *
 * <pre>
 * salt        20바이트 난수를 hex 로 편 40글자
 * stretch     SHA-256 을 10번 되풀이. 입력은 (비밀번호 + salt)
 * 저장 형태    base64(salt + 최종해시hex)  ->  디코드하면 40 + 64 = 104 글자
 * </pre>
 *
 * <h2>왜 그대로 옮기는가</h2>
 *
 * <p>기존 사용자 55명의 비밀번호가 이 형식으로 저장돼 있다. BCrypt 같은 것으로 바꾸면
 * <b>아무도 로그인하지 못한다.</b> 검증은 1:1 로 옮길 수밖에 없다.
 *
 * <p>이 방식은 요즘 기준으로 약하다 — 반복 10회는 너무 적고, salt 가 결과 안에 그대로
 * 들어 있다. 다만 그것은 이관이 아니라 개선이고, 로그인 시 재해시하는 방식으로
 * 점진 교체하는 것이 맞다. 별도 안건으로 둔다.
 *
 * <p>레거시는 {@code String.getBytes()} 를 써서 플랫폼 기본 문자셋에 기댔다. 여기서는
 * UTF-8 로 고정한다. salt 와 해시는 ASCII 라 영향이 없고, 비밀번호에 한글이 들어간
 * 경우에만 차이가 생길 수 있다. 레거시 서버가 UTF-8 이었다는 전제다.
 */
public class ErflowPasswordEncoder implements PasswordEncoder {

    /** 키 스트레칭 횟수. 레거시 {@code KEY_STRETCH_COUNT}. */
    private static final int STRETCH_COUNT = 10;

    /** salt 바이트 길이. hex 로 펴면 40글자가 된다. 레거시 {@code SALT_LENGTH}. */
    private static final int SALT_BYTES = 20;

    /** 저장값에서 salt 가 차지하는 글자 수. */
    private static final int SALT_CHARS = SALT_BYTES * 2;

    private static final Pattern BASE64 =
            Pattern.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");

    private final SecureRandom random = new SecureRandom();

    /**
     * 새 비밀번호를 저장 형태로 만든다.
     *
     * @param rawPassword 평문 비밀번호
     * @return base64 로 인코딩된 저장값
     */
    @Override
    public String encode(CharSequence rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return generate(rawPassword.toString(), HexFormat.of().formatHex(salt));
    }

    /**
     * 평문이 저장값과 맞는지 확인한다.
     *
     * <p>저장값에서 salt 를 꺼내 같은 방식으로 다시 만들어 비교한다.
     *
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 저장값
     * @return 일치하면 {@code true}
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null
                || !BASE64.matcher(encodedPassword).matches()) {
            return false;
        }
        String decoded;
        try {
            decoded = new String(
                    Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException expected) {
            return false;
        }
        if (decoded.length() < SALT_CHARS) {
            return false;
        }
        String salt = decoded.substring(0, SALT_CHARS);
        return MessageDigest.isEqual(
                encodedPassword.getBytes(StandardCharsets.UTF_8),
                generate(rawPassword.toString(), salt).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * salt 를 지정해 저장값을 만든다.
     *
     * <p>레거시 {@code generatePassword(pwd, salt)} 에 대응한다. 공개 API 로 두지 않는
     * 이유는 salt 를 바깥에서 정할 일이 없기 때문이고, 그럼에도 감춰 두지 않는 이유는
     * 고정 salt 로 기대값을 박아 두는 시험이 필요하기 때문이다.
     *
     * @param rawPassword 평문 비밀번호
     * @param salt hex 40글자
     * @return base64 저장값
     */
    static String generate(String rawPassword, String salt) {
        String value = rawPassword + salt;
        for (int i = 0; i < STRETCH_COUNT; i++) {
            value = sha256Hex(value);
        }
        return Base64.getEncoder()
                .encodeToString((salt + value).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);
        }
    }
}
