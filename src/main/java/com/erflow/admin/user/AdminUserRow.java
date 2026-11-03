package com.erflow.admin.user;

import com.erflow.common.LegacyDates;
import com.erflow.common.SocialNumbers;

/**
 * 사원 리스트 한 줄. {@code user_view} 의 한 행.
 *
 * <p>주소 세 칸은 줄마다 붙는 주소 대화상자가 쓴다(D-157) — 따로 묻지 않는다.
 *
 * @param id 사번
 * @param name 이름
 * @param socialNumber 주민등록번호. 화면에는 {@link #socialNumberMasked()} 로만 낸다
 * @param gender 성별
 * @param region 내·외국인
 * @param deptName 부서명
 * @param jobName 직급명
 * @param extensionPhone 내선 번호
 * @param mobilePhone 휴대 전화
 * @param email 이메일
 * @param hiredAt 입사일 원문
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세 주소
 */
public record AdminUserRow(
        String id,
        String name,
        String socialNumber,
        String gender,
        String region,
        String deptName,
        String jobName,
        String extensionPhone,
        String mobilePhone,
        String email,
        String hiredAt,
        String postalCode,
        String address1,
        String address2) {

    /**
     * @return 화면에 찍는 입사일
     */
    public String hiredAtLabel() {
        return LegacyDates.korean(hiredAt);
    }

    /**
     * @return 뒷자리 한 자리만 남긴 주민등록번호(D-117)
     */
    public String socialNumberMasked() {
        return SocialNumbers.masked(socialNumber);
    }
}
