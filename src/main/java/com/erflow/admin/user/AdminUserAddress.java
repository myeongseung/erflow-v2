package com.erflow.admin.user;

/**
 * 주소 팝업이 보여주는 것. 사원 리스트의 «보기» 가 새 창으로 연다.
 *
 * @param name 이름
 * @param jobName 직급
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세 주소
 */
public record AdminUserAddress(
        String name, String jobName, String postalCode, String address1, String address2) {
}
