// 게시글 보기 (D-159) — 댓글의 답글·수정 폼을 열고 닫고, 댓글 삭제를 POST 폼으로 보낸다.
//
// 댓글 한 줄(.comment-item)에 접힌 폼(.comment-fold)이 «답글»·«수정» 둘 있다.
// 단추(data-fold)가 가리키는 쪽만 열고 다른 쪽은 닫는다. 같은 단추를 다시 누르면 닫힌다.
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.comment-item [data-fold]').forEach(function(button) {
    button.addEventListener('click', function() {
      const item = button.closest('.comment-item')
      const name = button.dataset.fold
      item.querySelectorAll('.comment-fold').forEach(function(fold) {
        const open = fold.dataset.foldName === name && fold.style.display !== 'flex'
        fold.style.display = open ? 'flex' : 'none'
        if (open) {
          const area = fold.querySelector('textarea')
          if (area) {
            area.focus()
          }
        }
      })
    })
  })

  document.querySelectorAll('[data-delete]').forEach(function(button) {
    button.addEventListener('click', function() {
      if (!window.confirm('이 댓글을 지울까요?')) {
        return
      }
      const form = document.forms.commentDeleteFrm
      form.elements.id.value = button.dataset.id
      form.submit()
    })
  })
})
