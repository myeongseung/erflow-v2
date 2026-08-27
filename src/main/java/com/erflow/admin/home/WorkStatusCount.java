package com.erflow.admin.home;

/**
 * 근무 현황 그래프 한 조각.
 *
 * <p>레거시는 {@code {"0": 3, "1": 12}} 처럼 <b>코드→인원</b> 만 내보냈고, 라벨은 화면
 * 스크립트가 자기 배열에 박아 두었다. 그래서 서버와 스크립트가 따로 놀았고 실제로
 * 어긋나 있었다 — 3·4 가 뒤바뀌어 조퇴와 지각이 서로의 이름을 달고 나왔다(D-125).
 *
 * <p>라벨을 서버가 붙여 보낸다. {@link com.erflow.common.WorkStatus} 하나가 정답이고
 * 스크립트는 받아 쓰기만 한다. 어긋날 자리가 없어진다.
 *
 * <p>{@code status} 를 함께 보내는 이유는 색 때문이다. 스크립트는 이 코드로 CSS
 * 사용자 정의 속성({@code --color-status-*})을 찾아 조각을 칠한다 — 색을 정하는 곳은
 * 디자인 체계 한 곳뿐이다.
 *
 * @param status 상태 코드
 * @param label  상태 라벨
 * @param value  인원
 */
public record WorkStatusCount(int status, String label, int value) {
}
