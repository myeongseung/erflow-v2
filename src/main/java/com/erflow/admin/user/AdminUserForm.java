package com.erflow.admin.user;

import com.erflow.common.SocialNumbers;

/**
 * 사원 수정 화면이 채워 넣는 값. {@code user_tbl} 한 행이다.
 *
 * <p>목록·주소 팝업이 읽는 {@code user_view} 가 아니라 원본 표다. 화면이 직급·부서를
 * <b>이름이 아니라 번호</b>로 고르기 때문이다 — 뷰에는 이름만 있다.
 *
 * @param id 사번
 * @param name 이름
 * @param socialNumber 주민등록번호. 화면에는 가려서 찍는다 — {@link #socialNumberMasked()}
 * @param email 이메일
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세 주소
 * @param jobId 직급 번호
 * @param deptId 부서 번호
 * @param extensionPhone 내선 번호
 * @param mobilePhone 휴대 전화
 */
public record AdminUserForm(
        String id,
        String name,
        String socialNumber,
        String email,
        String postalCode,
        String address1,
        String address2,
        int jobId,
        int deptId,
        String extensionPhone,
        String mobilePhone) {

    /**
     * 수정 화면이 찍는 주민등록번호. 뒷자리 한 자리만 남는다(D-117).
     *
     * <p>이 칸은 {@code readonly} 이고 {@code updateUser} 문장에 {@code social_number}
     * 가 없다 — 가린 값이 폼과 함께 되돌아와도 저장되는 곳이 없다.
     *
     * @return 가려진 주민등록번호
     */
    public String socialNumberMasked() {
        return SocialNumbers.masked(socialNumber);
    }
}
