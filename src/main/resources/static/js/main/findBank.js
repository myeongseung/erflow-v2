// 레거시는 팝업 마크업의 onclick 이 이 함수를 직접 불렀다.
// Thymeleaf 가 문자열을 JS 호출에 끼워 넣는 것을 막으므로(이름에 따옴표가 들어가면
// 코드가 되는 자리다) 업종 찾기와 같은 data 속성 방식으로 맞췄다. D-035 참조.
$(document).ready(function() {
	$(".a-bank-info").on("click", function(e) {
		e.preventDefault();

		let _key = $(this).data("key");
		let _value = $(this).data("value");

		window.opener.receiveBankInfo(_key, _value);
		window.close(); // 팝업 창을 닫습니다.
	});
});
