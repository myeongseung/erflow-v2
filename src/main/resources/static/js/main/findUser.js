$(function() {
	$('#chkAll').on('click', function() {
		$("input[name=userId]").prop("checked", $("#chkAll").is(":checked"));
	});

	$('[name=userId]').on('click', function() {
		const total = $("input[name=userId]").length;
		const checked = $("input[name=userId]:checked").length;

		$("#chkAll").prop("checked", total === checked);
	});

	$('[name=keyword]').on('keydown', function(e) {
		if (e.which === 13) {
			e.preventDefault();
			$('[name=searchFrm]').find('[name=searchFrm]').val();
			$('[name=searchFrm]').submit();
		}
	});
	
	/* 쪽지쓰기 */
	$('#selectFrm').on('click', function(e) {
        e.preventDefault();
        
        let idValue = []; 
        	$('input[name="userId"]:checked').each(function() {
        	idValue.push($(this).val());
    	});
    	 	
    	if (idValue.length > 0) {
			let selectValue = idValue.join(';');	
			
			window.opener.updateSelect(selectValue);
			window.close();
		} else {
            alert('사원을 선택하세요');
        }
    });
});