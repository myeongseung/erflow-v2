// 직급·부서 관리 스크립트 (D-149).
// 출처: legacy/ERFlow/src/main/webapp/js/admin/jobDeptList.js
//
// 검색은 도구줄의 GET 폼이 직접 보내고(Enter), 추가·수정은 대화상자다
// (js/common/dialog.js). 여기 남는 것은 돋보기 클릭과 «전체 선택» 뿐이다.
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.toolbar-search .main-search-icon').forEach(function(icon) {
    icon.addEventListener('click', function() {
      icon.closest('form').requestSubmit()
    })
  })

  // 머리글의 체크박스가 같은 이름의 줄 체크박스를 한꺼번에 켜고 끈다.
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
