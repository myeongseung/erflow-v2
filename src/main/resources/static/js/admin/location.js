// 다음 우편번호 서비스로 주소를 채운다.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/location.js
//
// 찾은 값을 넣는 칸은 **누른 단추가 속한 .address-group 안에서** 이름으로 고른다
// (D-149) — 한 화면에 주소 묶음이 둘일 수 있다. 우편번호(postalCode) ·
// 도로명(address1) · 상세(address2). 참고항목은 보내지 않는 칸이라 이름이 없고,
// 묶음 안에서 이름 없는 입력이 그것이다.
function fillAddress(group, data) {
	var addr = '';
	var extraAddr = '';

	// 사용자가 고른 주소 종류에 따라 도로명(R) 또는 지번(J) 주소를 쓴다.
	if (data.userSelectedType === 'R') {
		addr = data.roadAddress;
	} else {
		addr = data.jibunAddress;
	}

	// 도로명 주소일 때만 참고항목(법정동·건물명)을 괄호로 붙인다.
	if (data.userSelectedType === 'R') {
		if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)) {
			extraAddr += data.bname;
		}
		if (data.buildingName !== '' && data.apartment === 'Y') {
			extraAddr += (extraAddr !== '' ? ', ' + data.buildingName : data.buildingName);
		}
		if (extraAddr !== '') {
			extraAddr = ' (' + extraAddr + ')';
		}
	}

	group.querySelector('[name="postalCode"]').value = data.zonecode;
	group.querySelector('[name="address1"]').value = addr;
	group.querySelector('input:not([name])').value = extraAddr;
	group.querySelector('[name="address2"]').focus();
}

$(document).ready(function() {
	$(".btn_execDaumPostcode").on("click", function() {
		var group = this.closest('.address-group');
		new daum.Postcode({
			oncomplete: function(data) {
				fillAddress(group, data);
			}
		}).open();
	});
});
