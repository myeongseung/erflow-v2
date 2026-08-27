$(function() {
	$('#html-selector').on('change', function() {
		const fileReader = new FileReader();
		const files = $('#html-selector').prop('files');
		
		fileReader.onload = function() {
			const data = fileReader.result;
			
			// 편집기는 js/common/editor.js 가 만들어 window.erflowEditor 로
			// 내놓는다(D-120). 전에는 CKEDITOR.instances['editor1'] 이었다.
			if (window.erflowEditor) {
				window.erflowEditor.value = data;
			}
		}
		if (files.length > 0) {
			fileReader.readAsText(files[0]);
		}
	});
})