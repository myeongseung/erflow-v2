$(function() {
	// 글 삭제
	$('#deleteLink').on('click', function(e) {
		e.preventDefault();
		$('#deleteForm').submit();
	});

	// 글 답변
	$('#replyLink').on('click', function(e) {
		e.preventDefault();
		$('#replyForm').submit();
	});

	// 글 수정
	$('#updateLink').on('click', function(e) {
		e.preventDefault();
		$('#updateForm').submit();
	});
	
	// 댓글의 답글 작성폼 보이기
	$('.reply-expand-button').on('click', function() {
		$(`.edit-reply[data-id=${ $(this).data('id') }]`).find('.collapse:visible').hide();
		$(`.post-reply[data-id=${ $(this).data('id') }]`).find('.collapse').slideToggle();
	});
	
	// 댓글 수정폼 보이기
	$('.edit-expand-button').on('click', function() {
		$(`.post-reply[data-id=${ $(this).data('id') }]`).find('.collapse:visible').hide();
		$(`.edit-reply[data-id=${ $(this).data('id') }]`).find('.collapse').slideToggle();
	});
});