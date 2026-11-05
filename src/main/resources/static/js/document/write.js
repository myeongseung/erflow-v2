// 기안 작성·수정 (D-160) — 양식을 고르면 같은 화면을 그 양식으로 다시 연다.
//
// 양식 콤보(data-reload)에 돌아갈 주소가 적혀 있다. 거기에 template 만 더 단다.
// 비어 있는 본문에만 양식이 들어가므로(D-110) 쓰던 저장본은 사라지지 않는다.
document.addEventListener('DOMContentLoaded', function() {
  const select = document.getElementById('templateId')
  if (!select || !select.dataset.reload) {
    return
  }
  select.addEventListener('change', function() {
    const base = select.dataset.reload
    const joiner = base.indexOf('?') >= 0 ? '&' : '?'
    window.location.href = base + joiner + 'template=' + encodeURIComponent(select.value)
  })
})
