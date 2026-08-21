package com.erflow.admin.user;

import com.erflow.common.LegacyDates;

/**
 * 사원 리스트 한 줄. {@code user_view} 한 행 중 표가 찍는 칸.
 *
 * <p>성별·내외국인·나이는 표에 있는 값이지만 컬럼이 아니다 — 뷰가 주민등록번호에서
 * 계산해 준다. 레거시도 그 계산을 SQL 쪽에 두었다.
 *
 * @param id 사번
 * @param name 이름
 * @param socialNumber 주민등록번호
 * @param gender 성별
 * @param region 내·외국인
 * @param deptName 부서
 * @param jobName 직급
 * @param extensionPhone 내선 번호
 * @param mobilePhone 개인 번호
 * @param email 이메일
 * @param hiredAt 입사일
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
        String hiredAt) {

    /**
     * 화면에 찍히는 입사일.
     *
     * <p>레거시는 «2004년 10월 01일» 로 보여준다. JSP 는 값을 그대로 찍고
     * {@code ResultSetExtractHelper} 가 미리 바꿔 둔다(D-088).
     *
     * @return 한글 날짜
     */
    public String hiredAtLabel() {
        return LegacyDates.korean(hiredAt);
    }
}
