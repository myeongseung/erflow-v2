$(function() {
	$('#chkAll').on('click', function() {
		$('[name=permissions]').prop('checked', $('#chkAll').is(':checked'));
	});
	
	$('[name=permissions]').on('click', function() {
		const total = $('[name=permissions]').length;
		const checked = $('[name=permissions]:checked').length;
		
		$('#chkAll').prop('checked', total === checked);
	});
})