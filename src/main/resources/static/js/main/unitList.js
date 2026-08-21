$(function() {
	$('#delete-button').on('click', function() {
		if ($('[name=unitId]:checked').length == 0) {
			alert('삭제할 설비를 선택하세요.');
		} else {
			$('[name=unitFrm]').submit();
		}
	});
	
	$('#register-button').on("click", function() { 
	    const width = 400;
	    const height = 450;
	    const left = (screen.width - width) / 2;
	    const top = (screen.height - height) / 2;
	    const features = `width=${width},height=${height},left=${left},top=${top}`;
	
	    window.open('/unit/register','_blank', features);
	});
	
	$('#update-button').on("click", function() { 
	    const width = 400;
	    const height = 450;
	    const left = (screen.width - width) / 2;
	    const top = (screen.height - height) / 2;
	    const features = `width=${width},height=${height},left=${left},top=${top}`;
	
	    window.open(`/unit/update?id=${$(this).data('id')}`,'_blank', features);
	});
	
	$(".main-search-text[name='keyword']").on("keydown", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$("#searchIcon").trigger("click");
		}
	});

	$("#searchIcon").on("click", function() {
		let _data = ["keyfield", "keyword"];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='unitFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").eq(0).submit();
	});

	$("#chkAll").click(function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		if ($("#chkAll").is(":checked")) {
			$("input[name=unitId]").prop("checked", true);
		} else {
			$("input[name=unitId]").prop("checked", false)
		};

	});

	$("input[name=unitId]").click(function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		const total = $("input[name=unitId]").length;
		const checked = $("input[name=unitId]:checked").length;

		if (total != checked) $("#chkAll").prop("checked", false);
		else $("#chkAll").prop("checked", true);
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