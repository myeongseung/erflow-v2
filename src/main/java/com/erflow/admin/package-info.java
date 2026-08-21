/**
 * 관리자 화면.
 *
 * <h2>여기만 다른 것 — 권한</h2>
 *
 * <p>다른 화면은 {@code screen} 테이블이 경로를 프로그램에 잇고 비트마스크로 판정한다
 * ({@code auth.ScreenAuthorizationManager}). <b>관리자 화면은 그 표에 없다.</b> 레거시가
 * 화면마다 {@code permissionCon.isAdmin(session)} 만 물었기 때문이다. 그래서 이 아래는
 * 경로 규칙으로 막는다 — {@code SecurityConfig} 의 {@code /admin/**} 규칙이다(D-053).
 *
 * <h2>패키지를 한 겹 더 나눈 이유</h2>
 *
 * <p>관리자 화면이 26개다. 한 패키지에 담으면 무엇이 어느 화면의 것인지 알 수 없다.
 * 레거시 폴더와 라우트가 이미 넷으로 갈라져 있으므로 그것을 따른다.
 *
 * <pre>
 * com.erflow.admin
 *   user/        사원 관리      /admin/user/*
 *   permission/  권한 관리      /admin/permission/*
 *   document/    문서 양식      /admin/document/*
 *   board/       게시판 관리    /admin/board/*
 * </pre>
 *
 * <p>사이드메뉴가 가리키는 네 갈래가 모두 실제 화면이다. 이관 전 발판
 * ({@code AdminScaffoldController})은 지웠다. 남은 것은 관리자 홈({@code /admin})
 * 하나이며 그것은 {@code common.ErrorPageController} 가 들고 있다.
 */
package com.erflow.admin;
