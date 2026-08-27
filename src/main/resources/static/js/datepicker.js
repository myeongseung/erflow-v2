$(function() {
	$("#datepicker").datepicker({
		dateFormat: 'yy-mm-dd' //달력 날짜 형태
		, showOtherMonths: true //빈 공간에 현재월의 앞뒤월의 날짜를 표시
		, showMonthAfterYear: true // 월- 년 순서가아닌 년도 - 월 순서
		, changeYear: true //option값 년 선택 가능
		, changeMonth: true //option값  월 선택 가능                
		/*, showOn: "both"*/ //button:버튼을 표시하고,버튼을 눌러야만 달력 표시 ^ both:버튼을 표시하고,버튼을 누르거나 input을 클릭하면 달력 표시
		// buttonImage·buttonImageOnly·buttonText 를 뺐다.
		//
		// 셋 다 showOn 이 "button" 이나 "both" 일 때만 쓰이는데 그 줄이 주석이라
		// **버튼 자체가 그려지지 않는다** — 죽은 설정이었다. 게다가 buttonImage 가
		// 가리키던 곳이 남의 데모 서버(http://jqueryui.com/resources/demos/...)이고
		// 평문 HTTP 였다(D-119).
		, yearSuffix: "년" //달력의 년도 부분 뒤 텍스트
		, monthNamesShort: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'] //달력의 월 부분 텍스트
		, monthNames: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'] //달력의 월 부분 Tooltip
		, dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'] //달력의 요일 텍스트
		, dayNames: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'] //달력의 요일 Tooltip
		, minDate: "-5Y" //최소 선택일자(-1D:하루전, -1M:한달전, -1Y:일년전)
		, maxDate: "+5y" //최대 선택일자(+1D:하루후, -1M:한달후, -1Y:일년후)  
	});
	$('#datepicker').datepicker('setDate', 'today');

});