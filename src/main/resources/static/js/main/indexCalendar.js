/*
 * 레거시 대비 바뀐 것은 주소뿐이다.
 *   /ERFlow/calendar/*  ->  /calendar/*
 *
 * CSRF 토큰은 js/common/csrf.js 가 모든 ajax 에 붙여 준다(D-086).
 */
$(function() {
	function formatDateToYYYYMMDDHHMMSS(date) {
		// 날짜 포맷을 YYYY-MM-DD hh:mm:ss로 변경
		var yyyy = date.getFullYear();
		var mm = (date.getMonth() + 1).toString().padStart(2, '0');
		var dd = date.getDate().toString().padStart(2, '0');
		var hh = date.getHours().toString().padStart(2, '0');
		var mi = date.getMinutes().toString().padStart(2, '0');
		var ss = '00';

		return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
	}

	$('.calendar-set').on("click", function() {
		// 시작일 및 종료일 제목 입력값 가져오기
		let userId = $('[name=userId]').val();
		let s_dateValue = $('.calendar-start').val(); //시작일
		let e_dateValue = $('.calendar-end').val(); //종료일
		let subject = $('.calendar-subject').val(); //제목
		let explanation = $(".calendar-content").val(); // 설명
		let type = $(".calendar-select option:selected").val();
		
		let flag = true;
		
		if (!s_dateValue) {
			alert("시작일을 선택해주세요.");
			flag = false;
		}
		
		if (subject == null || subject == "") {
			alert("제목을 선택해주세요.");
			flag = false;
		}

		// Date 객체 생성
		let s_date = new Date(s_dateValue);
		let e_date = new Date(e_dateValue);

		// 포맷팅된 시작일과 종료일
		let formattedStartDate = formatDateToYYYYMMDDHHMMSS(s_date);
		let formattedEndDate = formatDateToYYYYMMDDHHMMSS(e_date);

		if (s_date >= e_date) {
			alert("시작일은 종료일보다 크거나 같게 설정할 수 없습니다.");
			flag = false;
		}
		
		if (flag) {
			// Ajax 호출로 서버에 데이터 전송
			$.ajax({
				url: "/calendar/insert",
				type: 'POST',
				contentType: 'application/json',
				data: JSON.stringify({
					userId: userId,  // 사용자 ID를 적절히 설정하거나 세션에서 가져와야 합니다.
					subject: subject,
					content: explanation,
					start: formattedStartDate,
					end: formattedEndDate,
					type: type
				}),
				success: function(response) {
					if (response.status === 'success') {
						window.location.href = '/ERFlow/index.jsp';
					}
				},
				error: function(xhr, status, error) {
					alert('일정 추가 중 오류가 발생했습니다.');
				}
			});

			$('.modal').modal('hide');
		}
		
	});

	$('.calendar-cancel').on("click", function() {
		 $('.modal').modal('hide');
	});
	
	$('.modal').on('hidden.bs.modal', function() {
		$(this).find('form')[0].reset();
	});
});
