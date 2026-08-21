$(document).ready(function() {
	$(".confirm-button").on("click", function() {
		$('[name="proposalFrm"]').find('[name="result"]').val("confirm");
		$("form[name='proposalFrm']").submit();
	});
	$(".reject-button").on("click", function() {
		let _comment = $('[name="proposalFrm"]').find('[name="comment"]').val();
		if (_comment == null || _comment == "") {
			alert("코멘트를 입력해주세요.");
			return;
		} else {
			$('[name="proposalFrm"]').find('[name="result"]').val("reject");
			$("form[name='proposalFrm']").submit();
		}
	});
});