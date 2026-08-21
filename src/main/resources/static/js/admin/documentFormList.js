// 문서 양식 리스트 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/documentFormList.js
//
// 동작은 레거시 그대로다. 이동 주소만 새 라우트로 바꿨다(D-005).
//   documentFormRegister.jsp -> /admin/document/form-register
$(function () {
	$("#chkAll").click(function () {
		if ($("#chkAll").is(":checked")) {
			$("input[name=templateId]").prop("checked", true);
		} else {
			$("input[name=templateId]").prop("checked", false)
		};
	});

	$("input[name=templateId]").on('click', function() {
		const total = $("input[name=templateId]").length;
		const checked = $("input[name=templateId]:checked").length;
		
		if (total != checked) $("#chkAll").prop("checked", false);
		else $("#chkAll").prop("checked", true); 
	});
	
	$('#btn-register').on('click', function() {
		window.location.href = '/admin/document/form-register';
	});
	
	$('.close-button').on("click", function() {
		 $('.modal').modal('hide');
	});
});