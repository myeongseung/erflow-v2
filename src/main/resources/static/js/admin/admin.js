// 관리자 공통 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/admin.js
//
// 동작은 레거시 그대로다. 이동 주소만 새 라우트로 바꿨다(D-005).
//   /ERFlow/admin/admin.jsp -> /admin
document.addEventListener('DOMContentLoaded', function() {
    var mainMenus = document.querySelectorAll('.sidebar-mainmenu');

    mainMenus.forEach(function(menu) {
        menu.addEventListener('click', function() {
            if (this.classList.contains('active')) {
                this.classList.remove('active');
            } else {
                // 현재 메뉴에 active 클래스 추가
                this.classList.add('active');
            }
        });
    });
});

$(function() {
	$('.logo-font2').on('click', function() {
		document.location.href = '/admin';
	})
})
