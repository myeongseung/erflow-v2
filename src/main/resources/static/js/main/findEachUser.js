$(function() {
	$('[name=keyword]').on('keydown', function(e) {
		if (e.which === 13) {
			e.preventDefault();
			$('[name=searchFrm]').find('[name=searchFrm]').val();
			$('[name=searchFrm]').submit();
		}
	});

	$('#select-button').on('click', function(e) {
		e.preventDefault();
		$('input[name="userId"]:checked').each(function() {
			var userId = $(this).val();
			var userName = $(this).closest('tr').find('.search-people-body-receiver').text().trim();
			window.opener.receiveEachUserInfo(userId, userName);
			window.close();
		});

	});


});