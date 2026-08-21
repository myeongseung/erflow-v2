$(function() {
	$('.post-reader').on('click', function() {
		$('form[name=showFrm]').find('[name=id]').val( $(this).data('value') );
		$('form[name=showFrm]').submit();
	});
	
	$('.comment-reader').on('click', function() {
		$('form[name=showFrm]').find('[name=id]').val( $(this).data('value') );
		$('form[name=showFrm]').submit();
	});
	
	$('.main-search-text[name=keyword]').on('keydown', function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$('.main-search-icon').trigger('click');
		}
	});
	
	// Search
	$(".main-search-icon").on("click", function() {
		let _data = ["keyfield", "keyword"];
		
		_data.forEach(function(_key) {
			$('form[name=readFrm]').find(`[name=${_key}]`).val($('form[name=postFrm]').find(`[name=${_key}]`).val());
		});
		$("form[name='readFrm']").eq(0).submit();
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