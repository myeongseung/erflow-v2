// 권한 체크박스의 «전체 선택» — 직급·부서 수정, 게시판 권한, 메뉴 권한 화면이 쓴다.
// 열릴 때 이미 다 켜져 있으면 전체 선택도 켜 둔다(D-153).
$(function() {
	function syncAll() {
		const total = $('[name=permissions]').length;
		const checked = $('[name=permissions]:checked').length;

		$('#chkAll').prop('checked', total > 0 && total === checked);
	}

	$('#chkAll').on('click', function() {
		$('[name=permissions]').prop('checked', $('#chkAll').is(':checked'));
	});

	$('[name=permissions]').on('click', syncAll);
	syncAll();
})
