package com.erflow.admin.permission;

/**
 * 부서 또는 직급 한 줄. {@code permission_dept_view} / {@code permission_job_view} 의 한 행.
 *
 * <p>레거시 {@code ViewPermissionBean} 에 대응한다. 번호가 둘인 것에 주의한다 —
 * {@code id} 는 <b>권한 행</b> 번호이고 {@code classId} 가 부서·직급 번호다. 목록 화면은
 * 앞의 것을 «번호» 로 찍고 수정 링크에는 뒤의 것을 싣는다. 레거시 그대로다.
 *
 * @param id 권한 행 번호
 * @param classId 부서 또는 직급 번호
 * @param name 부서명 또는 직급명
 * @param level 자기 비트 하나
 * @param permission 가진 비트들(자기 비트 + 겸하는 것들)
 */
public record PermissionRow(int id, int classId, String name, long level, long permission) {
}
