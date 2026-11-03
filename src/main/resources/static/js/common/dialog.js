// 대화상자(D-148). 만들기·고치기 폼은 새 창이 아니라 목록 위의 <dialog> 에 뜬다.
//
//   data-dialog-open="아이디"   누르면 그 <dialog> 를 모달로 연다
//   data-dialog-close           누르면 자기를 감싼 <dialog> 를 닫는다
//   <dialog data-open-on-load>  화면이 열리면 바로 뜬다 (옛 주소로 들어온 경우)
//
// Esc 로 닫히는 것은 브라우저가 해 준다. 바깥을 눌러서는 닫히지 않는다 — 폼을
// 채우다 잃는 일이 없게.
document.addEventListener('DOMContentLoaded', function() {
	document.querySelectorAll('[data-dialog-open]').forEach(function(opener) {
		opener.addEventListener('click', function() {
			const dialog = document.getElementById(opener.dataset.dialogOpen)
			if (dialog) {
				dialog.showModal()
			}
		})
	})

	document.querySelectorAll('[data-dialog-close]').forEach(function(closer) {
		closer.addEventListener('click', function() {
			closer.closest('dialog').close()
		})
	})

	document.querySelectorAll('dialog[data-open-on-load]').forEach(function(dialog) {
		dialog.showModal()
	})
})
