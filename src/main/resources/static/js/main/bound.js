// 입·출고 화면 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/main/bound.js
//
// 동작은 레거시 그대로다. 팝업·이동 주소만 새 라우트로 바꿨다(D-005).
//   boundRegister.jsp?flag=  -> /bound/register?flag=
//   ../findEachUser.jsp      -> /find/each-user
//   ../findProduct.jsp       -> /find/product
$(document).ready(function() {
	$('#delete-button').on('click', function() {
		if ($('[name=boundId]:checked').length == 0) {
			alert('삭제할 내역을 선택해주세요.');
		} else {
			$('[name=boundFrm]').submit();
		}
	});

	$('#register-button').on('click', function() {
		document.location.href = `/bound/register?flag=${$(this).data('value')}`;
	});

	$("#chkAll").click(function() {
		if ($("#chkAll").is(":checked")) {
			$("input[name=boundId]").prop("checked", true);
		} else {
			$("input[name=boundId]").prop("checked", false)
		};
	});

	$("input[name=boundId]").click(function() {
		const total = $("input[name=boundId]").length;
		const checked = $("input[name=boundId]:checked").length;

		if (total != checked) $("#chkAll").prop("checked", false);
		else $("#chkAll").prop("checked", true);
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
			$("form[name='readFrm']").find('[name=' + key_name + ']').val($("form[name='boundFrm']").find('[name=' + key_name + ']').val());
		});
		$("form[name='readFrm']").eq(0).submit();
	});

	$("#find-each-user").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/each-user', '_blank', features);
	});

	$("#find-product").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/product', '_blank', features);
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

function receiveEachUserInfo(userId) {
	document.boundFrm.userId.value = userId;
}

function receiveProductInfo(productId, productName) {
	document.boundFrm.productId.value = productId;
	document.boundFrm.productName.value = productName;
}
