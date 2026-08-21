// 결재라인 관리 목록 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/proposal/proposalRoute.js
//
// 동작은 레거시 그대로다. 이동 주소만 새 라우트로 바꿨다(D-005).
//   proposalRouteRegister.jsp -> /proposal/route-register
$(function() {
	$('#delete-button').on('click', function() {
		if ($('[name=proposalId]:checked').length == 0) {
			alert('삭제할 결제라인을 선택해주세요.');
		} else {
			$('[name=proposalFrm]').submit();
		}
	});

	$('#register-button').on('click', function() {
		document.location.href = '/proposal/route-register'
	});

	$(".main-search-text[name='keyword']").on("keydown", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$(".main-search-icon i").trigger("click");
		}
	});

	$(".main-search-icon i").on("click", function() {
		let _data = ["keyfield", "keyword"];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='proposalFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").eq(0).submit();
	});

	$("#chkAll").click(function() {
		$('[name=proposalId]').prop('checked', $('#chkAll').is(':checked'));
	});

	$('[name=proposalId]').click(function() {
		const total = $('[name=proposalId]').length;
		const checked = $('[name=proposalId]:checked').length;

		$('#chkAll').prop('checked', total === checked);
	});
});

function block(base, blk) {
	document.readFrm.nowPage.value = base * (blk - 1) + 1;
	document.readFrm.submit();
}

function paging(page) {
	document.readFrm.nowPage.value = page;
	document.readFrm.submit();
}
