// 제품 목록 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/main/product.js
//
// 동작은 레거시 그대로다. 이동 주소만 새 라우트로 바꿨다(D-005).
//   productRegister.jsp?flag= -> /product/register?flag=
$(document).ready(function() {
	$("input.main-search-text[name='keyword']").on("keydown", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$("div.main-search-icon i").trigger("click");
		}
	});
	
	$("div.main-search-icon i").on("click", function() {
		let _data = [ "keyfield", "keyword" ];
		_data.forEach(function(key_name) {
			$("form[name='readFrm']").find('[name=' + key_name + ']').val( $("form[name='productFrm']").find('[name='+ key_name + ']').val() );
		});
		$('[name=readFrm]').submit();
	});
	
	$('#chkAll').on('click', function() {
		$('input[name=productId]').prop('checked', $('#chkAll').is(':checked'));
	});

	$('[name=productId]').on('click', function() {
		const total = $('input[name=productId]').length;
		const checked = $('input[name=productId]:checked').length;

		$("#chkAll").prop("checked", total === checked);
	});
	
	$('#delete-button').on('click', function() {
		if ($('[name=productId]:checked').length == 0) {
			alert('삭제할 재고 목록을 선택해주세요.');
		} else {
			$('[name=productFrm]').submit();
		}
	});
	
	$('#register-button').on('click', function() {
		document.location.href = `/product/register?flag=${$('#register-button').data('value')}`;
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