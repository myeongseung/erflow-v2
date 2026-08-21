$(function() {
	$('.btn-secondary').on('click', function() {
		window.location.href = `postList.jsp?boardId=${$(this).data('id')}`;
	});
	
	$('input[type=file]').on('change', function(event) {
		const fileList = $('#fileList');
	    const files = event.target.files; // 사용자가 선택한 파일들을 가져옴
	    
		fileList.html('');
	    
	    for (let i = 0; i < files.length; ++i) {
			const file = files[i];
			fileList.append('<div>' + file.name +  '</div>');
		}
	    if (files.length > 0) {
			$('#listbox').slideDown();
		} else {
			$('#listbox').slideUp();
		}
	});
})