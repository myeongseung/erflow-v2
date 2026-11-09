// 근태 확인(D-164). 도구줄의 달 고르기는 값이 바뀌면 바로 보낸다 — 따로 단추를
// 누르지 않는다. 검색은 공용 table.js(돋보기 · Enter)가 보내고, 달 넘기기와 쪽 번호는
// 보통의 링크다.
document.addEventListener('DOMContentLoaded', function() {
  const picker = document.querySelector('.month-nav input[type="month"]')
  if (picker) {
    picker.addEventListener('change', function() {
      picker.closest('form').requestSubmit()
    })
  }
})
