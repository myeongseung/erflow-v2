// 내려 열리는 메뉴(상단 바의 사람 항목) — Bootstrap 없이 연다(D-159).
//
// 여는 단추는 data-bs-toggle="dropdown" 을 가진 요소이고, 그 다음 형제
// .dropdown-menu 가 펼쳐지는 판이다. 밖을 누르거나 Esc 를 누르면 닫힌다.
document.addEventListener('DOMContentLoaded', function() {
  const openers = document.querySelectorAll('[data-bs-toggle="dropdown"]')

  function closeAll(except) {
    document.querySelectorAll('.dropdown-menu.show').forEach(function(menu) {
      if (menu !== except) {
        menu.classList.remove('show')
      }
    })
  }

  openers.forEach(function(opener) {
    opener.addEventListener('click', function(event) {
      event.preventDefault()
      const menu = opener.nextElementSibling
      if (!menu || !menu.classList.contains('dropdown-menu')) {
        return
      }
      const willOpen = !menu.classList.contains('show')
      closeAll(menu)
      menu.classList.toggle('show', willOpen)
      opener.setAttribute('aria-expanded', String(willOpen))
    })
  })

  document.addEventListener('click', function(event) {
    if (!event.target.closest('[data-bs-toggle="dropdown"], .dropdown-menu')) {
      closeAll(null)
    }
  })

  document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
      closeAll(null)
    }
  })
})
