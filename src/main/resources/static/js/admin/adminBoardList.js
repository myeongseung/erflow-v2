// 게시판 관리 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/adminBoardList.js
//
// 동작은 레거시 그대로다. 여는 주소만 새 라우트로 바꿨다(D-005).
//   boardRegister.jsp -> /admin/board/register
//   boardUpdate.jsp   -> /admin/board/update
$(function() {
	$('.register-board').on('click', function() { /* 게시판 생성버튼 클릭 */
		window.open('/admin/board/register', '_blank',
			'width=650, height=200, scrollbars=no, resizable=no, toolbar=no, menubar=no, channelmode=no');
	});

	$('.update-board').on('click', function() { /* 게시판 수정버튼 클릭 */
		const boardId = $('input[name=boardId]:checked');
		const length = boardId.length;
		
		if (length != 1) {
			alert('게시판 수정은 하나만 선택한 상태에서 가능합니다.');
		} else {
			window.open(`/admin/board/update?boardId=${boardId.eq(0).val()}`, '_blank',
				'width=650, height=200, scrollbars=no, resizable=no, toolbar=no, menubar=no, channelmode=no');
		}
	});

	$('#chkAllBoard').on('click', function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		$('input[name=boardId]').prop('checked', $('#chkAllBoard').is(':checked'));
	});

	$('input[name=boardId]').on('click', function() {
		const total = $('input[name=boardId]').length;
		const checked = $('input[name=boardId]:checked').length;

		$('#chkAllBoard').prop('checked', total === checked);
	});


	$('#chkAllPost').on('click', function() {   /* messageIndex.jsp 체크박스 전체 클릭*/
		$('input[name=postId]').prop('checked', $('#chkAllPost').is(':checked'));
	});


	$('input[name=postId]').on('click', function() {
		const total = $('input[name=postId]').length;
		const checked = $('input[name=postId]:checked').length;

		$('#chkAllPost').prop('checked', total === checked);
	});
	
	$('[name=board]').on('keydown', function(e) {
		if ($('[name=board]').is(':focus') && e.which === 13) {
			e.preventDefault();
			$('[name=readFrm]').find('[name=board]').val($('[name=boardFrm]').find('[name=board]').val());
			$('[name=readFrm]').submit();
		}
	});

	// 게시판 검색 버튼 클릭 이벤트 핸들러
	$('#boardSearch').on('click', function() {
		$('[name=readFrm]').find('[name=board]').val($('[name=boardFrm]').find('[name=board]').val());
		$('[name=readFrm]').submit();
	});
	
	$('.board-view').on('click', function() {
		$('[name=readFrm]').find('[name=boardId]').val($(this).data('id'));
		$('[name=readFrm]').submit();
	});
	
	$('#keyword').on('keydown', function(e) {
		if ($('#keyword').is(':focus') && e.which === 13) {
			e.preventDefault();
			$('[name=readFrm]').find('[name=keyfield]').val($('[name=postFrm]').find('[name=keyfield]').val());
			$('[name=readFrm]').find('[name=keyword]').val($('[name=postFrm]').find('[name=keyword]').val());
			$('[name=readFrm]').submit();
		}
	});

	// 게시글 검색 버튼 클릭 이벤트 핸들러
	$('#postSearch').on('click', function() {
		$('[name=readFrm]').find('[name=keyfield]').val($('[name=postFrm]').find('[name=keyfield]').val());
		$('[name=readFrm]').find('[name=keyword]').val($('[name=postFrm]').find('[name=keyword]').val());
		$('[name=readFrm]').submit();
	});
})

function blockBoard(base, blk) {
	document.readFrm.nowBoard.value = base * (blk - 1) + 1;
	document.readFrm.submit();
}

function pagingBoard(page) {
	document.readFrm.nowBoard.value = page;
	document.readFrm.submit();
}

function blockPost(base, blk) {
	document.readFrm.nowPage.value = base * (blk - 1) + 1;
	document.readFrm.submit();
}

function pagingPost(page) {
	document.readFrm.nowPage.value = page;
	document.readFrm.submit();
}