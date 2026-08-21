// 결재라인 등록/수정 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/proposal/proposalRouteRegister.js
//
// 동작은 레거시 그대로다. 팝업 주소만 새 라우트로 바꿨다(D-005).
//   ../findUser.jsp -> /find/user
//
// updateSelect: 사용자 찾기가 고른 사번을 Base64(URL 인코딩)로 감싸 readFrm 에 넣고
// 화면을 다시 그린다(GET /proposal/route-register). 수정 화면에는 find-user 가 없어
// 이 핸들러가 걸릴 자리가 없다 — 순서 변경(sortable)·행 삭제만 쓴다.
$(function() {
	$("#find-user").on("click", function() {
		const width = 650;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/user', '_blank', features);
	});

	jQuery("table > tbody").sortable({
		start: function(event, ui) {
			ui.item.addClass("selected");
		}
		, stop: function(event, ui) {
			ui.item.removeClass("selected");
		}
	});
	$("#myTable").on("click", ".delete-link", function() {
		$(this).parent().parent().remove();
	});
});

function updateSelect(selectValue) {
	document.readFrm.receiver.value = btoa(encodeURIComponent(selectValue));
	document.readFrm.nickname.value = document.proposalFrm.nickname.value;
	document.readFrm.submit();
}
