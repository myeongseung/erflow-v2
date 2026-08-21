// 결재 리스트 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/proposal/proposal.js
//
// 동작은 레거시 그대로다. 이동 주소만 새 라우트로 바꿨다(D-005).
//   proposalRegister.jsp -> /proposal/register
//
// block()/paging() 은 정의하지 않는다 — 레거시도 이 화면에 두 함수를 두지 않아
// 페이징이 동작하지 않았다(message 와 같은 D-045). 그대로 옮긴다.
$(function() {
	$('#register-button').on('click', function() {
		document.location.href = '/proposal/register'
	});

	$(".search-option select[name='keyfield']").on("change", function() {
		$(".main-search-icon i").trigger("click");
	});

	$(".main-search-icon i").on("click", function() {
		let _data = ["keyfield"];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='proposalFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").submit();
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
