$(function() {
	$('.board-select').on('click', function() {
		window.location.href = `postList.jsp?boardId=${$(this).data('id')}`;
		return false;
	});
	
	$('.main-search-text[name=keyword]').on('keydown', function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$('.main-search-icon').trigger('click');
		}
	});
	
	// Search
	$(".main-search-icon").on("click", function() {
		$('form[name=readFrm]').find('[name=keyword]').val($('form[name=boardFrm]').find('[name=keyword]').val());
		$("form[name=readFrm]").eq(0).submit();
	});
});