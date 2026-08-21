// 사원 주소 팝업.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/userListAddress.js
//
// 동작은 레거시 그대로다. 두 가지만 바꿨다.
//   userAddress.jsp?id=  -> /admin/user/address?id=   (D-005)
//   onclick="openNewWindow('사번')" -> data-id 로 넘겨 여기서 건다 (D-035)
function openNewWindow(id) {
    const width = 350;
    const height = 300;
    const left = (screen.width - width) / 2;
    const top = (screen.height - height) / 2;
    const features = `width=${width},height=${height},left=${left},top=${top}`;

    window.open(`/admin/user/address?id=${id}`, '_blank', features);
}

$(function() {
	$('.a-user-address').on('click', function() {
		openNewWindow($(this).data('id'));
	});
});
