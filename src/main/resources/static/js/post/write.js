// 글쓰기·글 수정 (D-159) — 고른 첨부 파일 이름을 입력 아래에 적어 준다.
document.addEventListener('DOMContentLoaded', function() {
  const input = document.querySelector('input[type="file"][name="filename"]')
  const list = document.getElementById('fileList')
  if (!input || !list) {
    return
  }
  input.addEventListener('change', function() {
    list.textContent = ''
    Array.prototype.forEach.call(input.files, function(file) {
      const item = document.createElement('li')
      item.textContent = file.name
      list.appendChild(item)
    })
  })
})
