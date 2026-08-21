// 공정 목록 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/process/processList.js
//
// 동작은 레거시 그대로다. 이동 주소만 새 라우트로 바꿨다(D-005).
//   processRegister.jsp -> /process/register
$(function() {
	$(".main-search-text[name='keyword']").on("keydown", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$(".main-search-icon i").trigger("click");
		}
	});

	$(".main-search-icon i").on("click", function() {
		let _data = ["keyfield", "keyword"];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='processFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").submit();
	});
	
	$("#delete-button").on("click", function() {
		if ($('[name=processId]:checked').length == 0) {
			alert('삭제할 공정을 선택해주세요.');
		} else {
			$('[name=processFrm]').submit();
		}
	});
	
	$("#register-button").on("click", function() {
		document.location.href = "/process/register";
	});
	
	$('#chkAll').on('click', function() {
		$('input[name=processId]').prop('checked', $('#chkAll').is(':checked'));
	});

	// 레거시는 이름 링크에 onclick 으로 창 크기를 적어 두었다. Thymeleaf 가 그 자리에
	// 값을 끼워 넣지 못해(D-035) 여기서 건다. 크기와 창 이름은 레거시 그대로다.
	$('.process-name').on('click', function(e) {
		e.preventDefault();
		window.open(this.href, 'myWindow', 'width=600, height=400');
	});

	$('[name=processId]').on('click', function() {
		const total = $('input[name=processId]').length;
		const checked = $('input[name=processId]:checked').length;

		$("#chkAll").prop("checked", total === checked);
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
