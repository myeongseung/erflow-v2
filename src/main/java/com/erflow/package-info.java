/**
 * ERFlow — JSP/Servlet(Model 1) 레거시의 Spring Boot 이관.
 *
 * <h2>패키지 구성</h2>
 *
 * <p>도메인별로 나눈다. 계층별({@code controller/}, {@code service/} 를 최상위에 두는 방식)이
 * 아니다. 이관이 도메인 슬라이스 단위로 진행되므로, 한 도메인이 한 패키지에 모여 있어야
 * 슬라이스가 통째로 옮겨졌는지 눈으로 확인된다.
 *
 * <pre>
 * com.erflow
 *   ErflowApplication
 *   layout/     헤더·사이드메뉴·권한 (전 화면 공통)
 *   unit/       생산 설비 관리   &lt;- slice 1
 *     UnitController  UnitService  UnitMapper  dto/
 *   company/    협력업체 관리     &lt;- slice 2 (대조군)
 * </pre>
 *
 * <h2>계층 규칙</h2>
 *
 * <p>의존 방향은 한쪽이다. {@code Controller -> Service -> Mapper}. 역방향과 계층 건너뛰기
 * 모두 금지다. Controller 가 Mapper 를 직접 부르면 트랜잭션 경계가 사라진다.
 *
 * <ul>
 *   <li><b>Controller</b> — HTTP 와 화면. 비즈니스 판단을 하지 않는다.
 *   <li><b>Service</b> — 트랜잭션 경계. 레거시가 JSP 스크립틀릿과 {@code ServiceImpl} 에
 *       흩어놨던 로직이 여기로 모인다.
 *   <li><b>Mapper</b> — SQL. 애너테이션 SQL 을 쓰지 않고 XML 매퍼로만 작성한다.
 *       이관한 쿼리가 레거시의 어느 SQL 에서 왔는지 한곳에서 대조할 수 있어야 한다.
 * </ul>
 *
 * <h2>이관 원칙</h2>
 *
 * <p>{@code legacy/ERFlow} 가 정답이다. 화면에 보이는 것을 더하거나 빼지 않는다.
 * 레거시에 없던 요소를 추가하면 {@code check_no_invention} 게이트가 막는다.
 * 동작을 바꿔야 한다면 {@code migration/design/00-decisions.md} 에 근거를 남긴다.
 */
package com.erflow;
