// 목록 틀(D-148) 공용 스크립트. 직급·부서 관리와 게시판 관리가 쓴다(D-153).
//
// - 도구줄의 돋보기를 누르면 그 검색 폼을 보낸다. Enter 는 폼이 알아서 보낸다.
// - 머리글 체크박스(data-check-all="이름")가 같은 이름의 줄 체크박스를 한꺼번에
//   켜고 끈다. 줄 하나를 끄면 머리글도 꺼진다.
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.toolbar-search .main-search-icon').forEach(function(icon) {
    icon.addEventListener('click', function() {
      icon.closest('form').requestSubmit()
    })
  })

  document.querySelectorAll('[data-check-all]').forEach(function(all) {
    const rows = document.querySelectorAll('input[name="' + all.dataset.checkAll + '"]')
    all.addEventListener('change', function() {
      rows.forEach(function(row) { row.checked = all.checked })
    })
    rows.forEach(function(row) {
      row.addEventListener('change', function() {
        all.checked = Array.prototype.every.call(rows, function(each) { return each.checked })
      })
    })
  })
})
