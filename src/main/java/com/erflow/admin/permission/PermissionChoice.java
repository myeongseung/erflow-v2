package com.erflow.admin.permission;

/**
 * 권한 수정 화면의 체크박스 하나.
 *
 * <p>수정 화면은 <b>자기 자신을 뺀</b> 나머지를 늘어놓고, 이미 가진 비트에 체크를 해 둔다.
 *
 * @param classId 부서 또는 직급 번호. 체크박스의 값이다
 * @param name 화면에 보이는 이름
 * @param checked 이미 가진 권한인가
 */
public record PermissionChoice(int classId, String name, boolean checked) {
}
