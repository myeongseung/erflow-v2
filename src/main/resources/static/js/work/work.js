$(function() {
	// 날짜 검색 기능
	$('.date-search').on('click', function() {
		$('[name=readFrm]').find('input[name=date]').val($('input[type=month]').val());
		$('[name=readFrm]').submit();
	});
	
	// 검색 엔터키 적
	$('.main-search-text[name=keyword]').on('keydown', function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$('.main-search-icon i').trigger('click');
		}
	});
	
	// 검색 기능
	$('.main-search-icon i').on('click', function() {
		const _data = [ 'keyfield', 'keyword' ];
		
		_data.forEach(function(_key) {
			$('[name=readFrm]').find(`[name=${_key}]`).val(
				$('[name=workFrm]').find(`[name=${_key}]`).val()
			);
		});
		$('[name=readFrm]').submit();
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