$(function() {
	$('[name=templateId]').on('change', function() {
		$('[name=readFrm]').find('[name=template]').val($(this).val());
		$('[name=readFrm]').submit();
	});
})