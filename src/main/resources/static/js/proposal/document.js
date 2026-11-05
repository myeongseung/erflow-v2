// 결재 문서 보기 (D-160) — 승인·반려 단추가 숨은 result 를 채우고 폼을 보낸다.
//
// 반려는 사유가 있어야 한다. 의견 칸에 required 를 걸고 브라우저의 검사 말풍선으로
// 알린다 — 승인은 비워도 된다.
document.addEventListener('DOMContentLoaded', function() {
  const form = document.getElementById('decisionFrm')
  if (!form) {
    return
  }
  form.querySelectorAll('[data-decide]').forEach(function(button) {
    button.addEventListener('click', function() {
      const reject = button.dataset.decide === 'reject'
      form.elements.comment.required = reject
      if (!form.reportValidity()) {
        return
      }
      if (!window.confirm(reject ? '이 문서를 반려할까요?' : '이 문서를 승인할까요?')) {
        return
      }
      form.elements.result.value = reject ? 'reject' : 'confirm'
      form.submit()
    })
  })
})
