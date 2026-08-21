// 수·발주 화면 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/main/task.js
//
// 동작은 레거시 그대로다. 팝업·이동 주소만 새 라우트로 바꿨다(D-005).
//   ../findEachUser.jsp   -> /find/each-user
//   ../findCompany.jsp    -> /find/company
//   ../findDocument.jsp   -> /find/document
//   ../findMultiProduct.jsp -> /find/multi-product
//   taskRegister.jsp?flag=  -> /task/register?flag=
//   createModal.jsp?taskId= -> /task/history-modal?taskId=
$(function() {
	$('#delete-button').on('click', function() {
		if ($('[name=taskId]:checked').length == 0) {
			alert('삭제할 업무를 선택하세요.');
		} else {
			$('[name=taskFrm]').submit();
		}
	});

	$('#register-button').on("click", function() {
		document.location.href = `/task/register?flag=${$(this).data('value')}`;
	});

	$("input.main-search-text[name='keyword']").on("keydown", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$("div.main-search-icon i").trigger("click");
		}
	});

	$("div.main-search-icon i").on("click", function() {
		let _data = ["keyfield", "keyword"];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='taskFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").submit();
	});

	$("#find-user").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/each-user', '_blank', features);
	});

	$("#find-company").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/company', '_blank', features);
	});

	$(".openModalBtn").on("click", function() {
		const selectedTaskId = $(this).val();
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;

		const url = `/task/history-modal?taskId=${selectedTaskId}`;

		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open(url, '_blank', features);
	});

	$("#find-document").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/document', '_blank', features);
	});

	$("#find-multi-product").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/multi-product', '_blank', features);
	});

	$('input[name="count"]').on('input', function() {
		if ($(this).val().trim() === '') {
			$(this).val('0');
		}
	});

	$("#chkAll").click(function() {
		if ($("#chkAll").is(":checked")) {
			$("input[name=taskId]").prop("checked", true);
		} else {
			$("input[name=taskId]").prop("checked", false)
		};
	});

	$("input[name=taskId]").click(function() {
		const total = $("input[name=taskId]").length;
		const checked = $("input[name=taskId]:checked").length;

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

function updateSelect(selectValue) {
	document.taskFrm.userId.value = selectValue;
}

function receiveEachUserInfo(userId, userName) {
	document.taskFrm.userId.value = userId;
	document.taskFrm.userName.value = userName;
}

function receiveCompanyInfo(companyId, companyName) {
	document.taskFrm.companyId.value = companyId;
	document.taskFrm.companyName.value = companyName;
}

function receiveDocumentInfo(documentId) {
	document.taskFrm.documentId.value = documentId;
}

function receiveMultiProductInfo(productId, productName) {
	$("#myTable tbody").empty();
	for (var i = 0; i < productId.length; i++) {
		$("#myTable").append("<tr><td><input type='hidden' name='productId' value='" + productId[i] + "'>" + productId[i] + "</td><td>" + productName[i] + "</td><td><input type='number' name='count' value='1' min='0'></td></tr>");
	}
}
