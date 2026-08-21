/*
 * jQuery 로 나가는 모든 요청에 CSRF 토큰을 실어 준다.
 *
 * 왜 필요한가
 *   레거시에는 CSRF 방어가 없었다. 이관하면서 켰고(D-013), form 은 Thymeleaf 가
 *   hidden 입력을 알아서 넣어 준다. 그러나 **JSON 을 보내는 ajax 는 form 이 아니다** —
 *   토큰이 붙지 않아 서버가 403 으로 되돌린다. 화면은 아무 말 없이 «오류가
 *   발생했습니다» 만 띄운다.
 *
 *   달력(일정 추가·수정·삭제)과 공정 등록이 그 경로다.
 *
 * 토큰은 페이지의 <meta> 에서 읽는다. 화면에 보이는 요소가 아니므로 레거시 대비
 * 정합성에는 영향이 없다.
 *
 * GET/HEAD 에는 붙이지 않는다. 서버도 그쪽은 검사하지 않는다.
 */
(function () {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');

    if (!token || !header || typeof jQuery === 'undefined') {
        return;
    }
    jQuery.ajaxSetup({
        beforeSend: function (xhr, settings) {
            if (!/^(GET|HEAD|OPTIONS|TRACE)$/i.test(settings.type || 'GET')) {
                xhr.setRequestHeader(header.content, token.content);
            }
        }
    });
})();
