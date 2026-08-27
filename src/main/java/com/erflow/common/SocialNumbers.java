package com.erflow.common;

/**
 * 화면에 찍는 주민등록번호 모양.
 *
 * <h2>왜 가리는가</h2>
 *
 * <p>레거시 사원 리스트는 주민등록번호를 <b>13자리 그대로</b> 표에 찍었다. 관리자만
 * 들어오는 화면이지만 관리자도 뒷자리를 볼 일이 없다 — 그 표가 쓰는 것은 성별·
 * 내외국인이고, 그 둘은 {@code user_view} 가 이미 계산해서 별도 칸으로 준다.
 *
 * <p>1단계에서는 옮기지 않았다. 화면에 보이는 글자를 바꾸는 일은 이관이 아니어서
 * 안건으로만 남겼다(O-012). 여기서 가린다(D-117).
 *
 * <h2>얼마나 남기는가</h2>
 *
 * <p>뒷자리 일곱 중 <b>첫 한 자리만</b> 남긴다.
 *
 * <pre>
 * 999999-9999999  ->  999999-9******
 * </pre>
 *
 * <p>남는 한 자리는 성별·세기 코드다. 앞 여섯 자리(생년월일)와 이 한 자리가 있으면
 * 표가 보여 주던 성별·내외국인·나이를 그대로 읽을 수 있다 — 가리면서 잃는 정보가
 * 없다. 정말로 감춰야 하는 것은 그 뒤 여섯 자리다.
 *
 * <p>글자 수는 그대로 열넷이다. 표의 칸 너비가 흔들리지 않는다.
 */
public final class SocialNumbers {

    /** 주민등록번호의 숫자 개수. */
    private static final int DIGITS = 13;

    /** 앞자리(생년월일) 길이. */
    private static final int FRONT = 6;

    /** 가리는 뒷자리 개수 — 일곱 중 하나를 남기므로 여섯이다. */
    private static final String HIDDEN = "******";

    private SocialNumbers() {
    }

    /**
     * «999999-9******».
     *
     * <p>비어 있는 값은 비어 있는 그대로 돌려준다. 레거시도 주민등록번호가 없는
     * 사원의 칸을 빈칸으로 두었고, 빈칸을 별표로 채우면 <b>없는 값이 있는 것처럼</b>
     * 보인다.
     *
     * <p>모양이 다른 값은 <b>통째로</b> 가린다. 어디까지가 앞자리인지 모르는 채로
     * 일부만 남기면 그 «일부» 가 뒷자리일 수 있다 — 모르면 더 가리는 쪽이 맞다.
     *
     * @param value 저장된 주민등록번호. {@code null} 이면 {@code null}
     * @return 뒷자리 한 자리만 남긴 글자
     */
    public static String masked(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        StringBuilder digits = new StringBuilder(DIGITS);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            }
        }

        if (digits.length() != DIGITS) {
            return "*".repeat(value.length());
        }
        return digits.substring(0, FRONT) + "-" + digits.charAt(FRONT) + HIDDEN;
    }
}
