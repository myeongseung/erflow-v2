$(function() {
	$('.main-search-icon i').on('click', function() {
		const _data = [ "keyfield", "keyword" ];
		
		_data.forEach(function(_key) {
			$('form[name=readFrm]').find(`[name=${_key}]`).val(
				$('form[name=searchFrm]').find(`[name=${_key}]`).val()
			);
		});
		$('form[name=readFrm]').eq(0).submit();
	});
})

function block(base, blk) {
	document.readFrm.nowPage.value = base * (blk - 1) + 1;
	document.readFrm.submit();
}

function paging(page) {
	document.readFrm.nowPage.value = page;
	document.readFrm.submit();
}

function search() {
	document.readFrm.keyfield.value = document.searchFrm.keyfield.value;
	document.readFrm.keyword.value = document.searchFrm.keyword.value;
	document.readFrm.submit();
}