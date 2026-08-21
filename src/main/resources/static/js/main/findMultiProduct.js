$(function() {
	$("#chkAll").click(function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		if ($("#chkAll").is(":checked")) {
			$("input[name=productId]").prop("checked", true);
		} else {
			$("input[name=productId]").prop("checked", false)
		};
	});

	$("input[name=productId]").click(function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		const total = $("input[name=productId]").length;
		const checked = $("input[name=productId]:checked").length;

		if (total != checked) $("#chkAll").prop("checked", false);
		else $("#chkAll").prop("checked", true);
	});

	$('#select-button').click(function() {
		var selectedProductsId = [];
		var selectedProductsName = [];
		$('input[name="productId"]:checked').each(function() {
			var productId = $(this).val();
			var productName = $(this).closest('tr').find('.search-people-body-product').text();
			selectedProductsId.push(productId);
			selectedProductsName.push(productName);
		});

	window.opener.receiveMultiProductInfo(selectedProductsId, selectedProductsName);
	window.close();
	});

});