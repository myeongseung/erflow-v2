$(function() {
	$('.write-message').on("click", function() { /* 쪽지쓰기 버튼 클릭 */
		window.open('message/register.jsp', '_blank',
			'width=600, height=550, scrollbars=no, resizable=no, toolbar=no, menubar=no, channelmode=no');
	});
});