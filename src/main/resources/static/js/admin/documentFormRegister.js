$(function() {
	$('#html-selector').on('change', function() {
		const fileReader = new FileReader();
		const files = $('#html-selector').prop('files');
		
		fileReader.onload = function() {
			const data = fileReader.result;
			
			CKEDITOR.instances['editor1'].setData(data);
		}
		if (files.length > 0) {
			fileReader.readAsText(files[0]);
		}
	});
})