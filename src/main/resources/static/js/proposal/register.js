// 결재 올리기 대화상자 (D-161) — 결재 리스트 위에 뜬다. 왼쪽 표에서 문서 하나,
// 오른쪽 표에서 결재라인 하나를 라디오로 고른다.
//
//   tr[data-text] 안의 라디오        줄 아무 데나 눌러도 골라지고, 고른 줄은 is-picked
//   [data-filter="tbody 아이디"]      입력한 글자가 든 줄만 남긴다(줄의 data-text 로 견준다)
//
// 둘 다 골라야 보낸다 — 서버도 거르지만 «등록에 실패했습니다» 보다 먼저 알려 준다.
document.addEventListener('DOMContentLoaded', function() {
  const form = document.getElementById('proposalFrm')
  if (!form) {
    return
  }

  function markPicked(radio) {
    form.querySelectorAll('input[name="' + radio.name + '"]').forEach(function(other) {
      other.closest('tr').classList.toggle('is-picked', other.checked)
    })
  }

  form.querySelectorAll('tr[data-text]').forEach(function(row) {
    const radio = row.querySelector('input[type="radio"]')
    if (!radio) {
      return
    }
    row.addEventListener('click', function() {
      radio.checked = true
      markPicked(radio)
    })
    // 키보드(화살표)로 옮겨도 줄 표시가 따라간다
    radio.addEventListener('change', function() {
      markPicked(radio)
    })
    if (radio.checked) {
      markPicked(radio)
    }
  })

  form.querySelectorAll('[data-filter]').forEach(function(input) {
    const rows = form.querySelectorAll('#' + input.dataset.filter + ' > tr[data-text]')
    input.addEventListener('input', function() {
      const needle = input.value.trim().toLowerCase()
      rows.forEach(function(row) {
        row.hidden = needle !== '' && row.dataset.text.toLowerCase().indexOf(needle) < 0
      })
    })
  })

  form.addEventListener('submit', function(event) {
    const missing = !form.querySelector('input[name="documentId"]:checked')
        || !form.querySelector('input[name="routeId"]:checked')
    document.getElementById('pickError').hidden = !missing
    if (missing) {
      event.preventDefault()
    }
  })
})
