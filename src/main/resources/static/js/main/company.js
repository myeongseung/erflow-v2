$(document).ready(function() {
	$(".main-search-text[name='keyword']").on("keydown", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$("#searchIcon").trigger("click");
		}
	});

	$("#searchIcon").on("click", function() {
		let _data = ["keyfield", "keyword"];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='companyFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").eq(0).submit();
	});

	$("#chkAll").click(function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		if ($("#chkAll").is(":checked")) {
			$("input[name=companyId]").prop("checked", true);
		} else {
			$("input[name=companyId]").prop("checked", false)
		};
	});

	$("input[name=companyId]").click(function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		const total = $("input[name=companyId]").length;
		const checked = $("input[name=companyId]:checked").length;

		if (total != checked) $("#chkAll").prop("checked", false);
		else $("#chkAll").prop("checked", true);
	});
	
	$('#delete-button').on('click', function() {
		if ($('[name=companyId]:checked').length == 0) {
			alert('삭제할 협력업체 목록을 선택해주세요.');
		} else {
			$('[name=companyFrm]').submit();
		}
	});
	
	$('#register-button').on('click', function() {
		document.location.href = `companyRegister.jsp?flag=${$(this).data('id')}`;
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

function search() {
	document.readFrm.keyfield.value = document.companyFrm.searchkeyfield.value;
	document.readFrm.keyword.value = document.companyFrm.searchkeyword.value;
	document.readFrm.submit();
}