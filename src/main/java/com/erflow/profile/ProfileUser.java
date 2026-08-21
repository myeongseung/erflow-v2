package com.erflow.profile;

/**
 * 프로필 화면이 보여 주는 사용자. {@code user_tbl} 한 행에서 화면이 쓰는 칸만.
 *
 * <p>주민번호는 담지 않는다. 레거시도 로그인할 때 세션에 넣기 전에 지웠고
 * ({@code LoginServlet} 의 {@code user.setSocialNumber(null)}), 그래서 프로필 수정
 * 화면의 «주민 번호» 칸은 <b>언제나 비어 있다</b>. 화면에 안 나오는 값을 굳이 읽어
 * 올 이유가 없다.
 *
 * @param id 사번
 * @param name 이름
 * @param email 이메일
 * @param extensionPhone 내선 전화
 * @param mobilePhone 개인 전화
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세 주소
 */
public record ProfileUser(
        String id,
        String name,
        String email,
        String extensionPhone,
        String mobilePhone,
        String postalCode,
        String address1,
        String address2) {

    /**
     * @return 이메일. 없으면 빈 글자
     */
    public String emailText() {
        return orEmpty(email);
    }

    /**
     * @return 내선 전화. 없으면 빈 글자
     */
    public String extensionPhoneText() {
        return orEmpty(extensionPhone);
    }

    /**
     * @return 개인 전화. 없으면 빈 글자
     */
    public String mobilePhoneText() {
        return orEmpty(mobilePhone);
    }

    /**
     * @return 우편번호. 없으면 빈 글자
     */
    public String postalCodeText() {
        return orEmpty(postalCode);
    }

    /**
     * @return 도로명 주소. 없으면 빈 글자
     */
    public String address1Text() {
        return orEmpty(address1);
    }

    /**
     * @return 상세 주소. 없으면 빈 글자
     */
    public String address2Text() {
        return orEmpty(address2);
    }

    /**
     * 프로필 카드에 한 줄로 찍는 주소.
     *
     * <p>레거시가 <b>사이에 공백 하나를 반드시 넣는다.</b> 주소가 둘 다 비어 있으면
     * 공백 한 글자만 남는다 — 그대로 옮긴다.
     *
     * @return "도로명 상세"
     */
    public String addressText() {
        return orEmpty(address1) + " " + orEmpty(address2);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
