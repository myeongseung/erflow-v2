package com.erflow.auth;

/**
 * 화면 하나가 요구하는 권한.
 *
 * <p>{@code screen} 테이블이 경로를 프로그램에 잇고, 프로그램이 요구 비트마스크를 갖는다.
 *
 * <p>같은 경로가 요청 파라미터에 따라 서로 다른 권한을 갖는 경우가 있다. 레거시가 화면
 * 안에서 {@code switch (paramFlag)} 로 {@code PROGRAM_CODE} 를 골랐기 때문이다
 * (협력업체 관리는 {@code flag=1} 이면 구매, {@code flag=0} 이면 영업). 그런 화면은
 * 경로가 같은 행이 여러 개이고 {@code paramName}/{@code paramValue} 로 갈린다.
 *
 * @param route 요청 경로
 * @param paramName 권한이 갈리는 파라미터명. 갈리지 않으면 {@code null}
 * @param paramValue 그 파라미터의 값
 * @param programName 권한 프로그램명. 화면 라벨과 다를 수 있다
 * @param deptLevel 요구 부서 권한
 * @param jobLevel 요구 직급 권한
 */
public record ScreenAccess(
        String route,
        String paramName,
        String paramValue,
        String programName,
        long deptLevel,
        long jobLevel) {

    /**
     * 이 규칙이 요청에 걸리는지 본다.
     *
     * @param actual 요청에 실린 파라미터 값. 없으면 {@code null}
     * @return 파라미터를 따지지 않거나 값이 맞으면 {@code true}
     */
    public boolean matches(String actual) {
        return paramName == null || paramValue.equals(actual);
    }
}
