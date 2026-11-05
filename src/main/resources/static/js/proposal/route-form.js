// 결재라인 만들기·고치기 (D-160) — 결재자 표를 화면에서 만든다.
//
//   «직원 추가» 대화상자: 부서·직급·이름으로 거르고(모두 화면에서), 체크한 사람을
//   표에 넣는다. 이미 든 사람은 건너뛴다.
//   표의 줄: 위로 · 아래로 · 빼기. 순서 칸은 고칠 때마다 다시 매긴다.
//   보낼 때 결재자가 없으면 막는다.
document.addEventListener('DOMContentLoaded', function() {
  const form = document.getElementById('routeFrm')
  const rows = document.getElementById('routeRows')
  if (!form || !rows) {
    return
  }

  function renumber() {
    const items = rows.querySelectorAll('tr')
    items.forEach(function(row, index) {
      row.querySelector('.order-cell').textContent = index + 1
      row.querySelector('[data-move="up"]').disabled = index === 0
      row.querySelector('[data-move="down"]').disabled = index === items.length - 1
    })
    document.getElementById('routeEmpty').hidden = items.length > 0
  }

  function bindRow(row) {
    row.querySelector('[data-move="up"]').addEventListener('click', function() {
      if (row.previousElementSibling) {
        rows.insertBefore(row, row.previousElementSibling)
        renumber()
      }
    })
    row.querySelector('[data-move="down"]').addEventListener('click', function() {
      if (row.nextElementSibling) {
        rows.insertBefore(row.nextElementSibling, row)
        renumber()
      }
    })
    row.querySelector('[data-remove]').addEventListener('click', function() {
      row.remove()
      renumber()
    })
  }

  function makeRow(id, name, dept, job) {
    const row = document.createElement('tr')
    row.dataset.id = id
    const cells = [
      '<td class="num-col order-cell"></td>',
      '<td><b></b><input type="hidden" name="routeId"></td>',
      '<td class="id-cell"></td>',
      '<td class="dept-cell"></td>',
      '<td class="job-cell"></td>',
      '<td class="nowrap-cell row-actions">'
        + '<button type="button" class="link-btn" data-move="up">위로</button> '
        + '<button type="button" class="link-btn" data-move="down">아래로</button> '
        + '<button type="button" class="link-btn" data-remove>빼기</button></td>'
    ]
    row.innerHTML = cells.join('')
    row.querySelector('b').textContent = name
    row.querySelector('input').value = id
    row.querySelector('.id-cell').textContent = id
    row.querySelector('.dept-cell').textContent = dept
    row.querySelector('.job-cell').textContent = job
    bindRow(row)
    return row
  }

  rows.querySelectorAll('tr').forEach(bindRow)
  renumber()

  // ── 직원 고르기 대화상자
  const pickRows = Array.prototype.slice.call(document.querySelectorAll('#pickRows > tr'))
  const deptFilter = document.querySelector('[data-filter-dept]')
  const jobFilter = document.querySelector('[data-filter-job]')
  const nameFilter = document.querySelector('[data-filter-name]')

  function applyFilter() {
    const dept = deptFilter.value
    const job = jobFilter.value
    const needle = nameFilter.value.trim()
    pickRows.forEach(function(row) {
      row.hidden = (dept !== '' && row.dataset.dept !== dept)
        || (job !== '' && row.dataset.job !== job)
        || (needle !== '' && row.dataset.name.indexOf(needle) < 0)
    })
  }
  ;[deptFilter, jobFilter].forEach(function(select) { select.addEventListener('change', applyFilter) })
  nameFilter.addEventListener('input', applyFilter)

  document.querySelector('[data-check-visible]').addEventListener('change', function(event) {
    pickRows.forEach(function(row) {
      if (!row.hidden) {
        row.querySelector('input[type="checkbox"]').checked = event.target.checked
      }
    })
  })

  document.querySelector('[data-add-users]').addEventListener('click', function(event) {
    pickRows.forEach(function(row) {
      const box = row.querySelector('input[type="checkbox"]')
      if (!box.checked) {
        return
      }
      box.checked = false
      if (!rows.querySelector('tr[data-id="' + row.dataset.id + '"]')) {
        rows.appendChild(makeRow(row.dataset.id, row.dataset.name, row.dataset.dept, row.dataset.job))
      }
    })
    document.querySelector('[data-check-visible]').checked = false
    renumber()
    event.target.closest('dialog').close()
  })

  form.addEventListener('submit', function(event) {
    const empty = rows.querySelectorAll('tr').length === 0
    document.getElementById('routeError').hidden = !empty
    if (empty) {
      event.preventDefault()
    }
  })
})
