/*
 * 글 편집기 — 다섯 화면이 이 파일 하나를 쓴다.
 *
 *   게시글 등록·답변·수정 / 결재 문서 작성 / 문서 양식 작성
 *
 * ## 왜 CKEditor 를 떠났나
 *
 * CKEditor 4 는 **지원이 끝났다**. 게다가 남의 CDN(cdn.ckeditor.com)에서
 * 받아 오고 있었다. 둘 다 사내 시스템에 둘 이유가 없다(D-120).
 *
 * ## 왜 Jodit 인가
 *
 * 고르는 기준은 넷이었다 — MIT · 빌드 없이 <script> 하나 · 살아 있는 유지보수 ·
 * **기존 결재 양식을 망가뜨리지 않을 것**. 마지막이 제일 어려웠다. 요즘 편집기
 * 대부분은 스키마를 갖고 있어서 스키마 밖의 것을 조용히 버리는데, 우리 양식은
 * 표 안에 <input> 체크상자가 박혀 있다.
 *
 * 실제 양식(근태계)을 넣고 되읽어 재 봤다 — input 8→8, td[style] 18→18,
 * colspan·rowspan·인라인 style 전부 그대로였고 보이는 글자도 같았다. 늘어난
 * 것은 빈 셀의 <br> 여덟 개뿐이다(빈 셀을 누를 수 있게 하는 표준 동작이며
 * 정화 목록에 br 이 있다).
 *
 * ## 도구모음은 정화 목록 안에서만 만든다
 *
 * 화면에 나가는 HTML 은 SafeHtml 의 허용목록을 지난다(D-118). 편집기가 그
 * 목록 밖의 것을 만들 수 있으면 **«저장했는데 화면에서 사라지는»** 일이 생긴다.
 * 그래서 단추를 목록에 맞춰 깎았다 — 영상·파일 넣기가 없는 것이 그 때문이다.
 *
 * 그림도 같은 이유로 **주소로만** 넣는다. 파일을 끌어다 놓으면 base64
 * (data: 주소)가 되는데 허용 프로토콜은 http·https·mailto 뿐이라 저장 뒤에
 * 사라진다. 게다가 본문 칸에 수 MB 가 들어앉는다.
 */
(function () {
	'use strict';

	var TARGET_ID = 'editor1';
	var STYLE_HREF = '/js/vendor/jodit-4.13.23.min.css';

	/*
	 * 편집기 CSS 는 편집기와 함께 다닌다.
	 *
	 * 화면이 싣는 스타일시트는 app.css 하나라는 것이 이 저장소의 규칙이다
	 * (D-119). 다섯 화면에만 필요한 161KB 를 100화면이 나눠 지게 할 이유가
	 * 없으므로, 편집기를 붙이는 자리에서 스스로 불러온다.
	 */
	function loadStyles() {
		if (document.querySelector('link[href="' + STYLE_HREF + '"]')) {
			return;
		}
		var link = document.createElement('link');
		link.rel = 'stylesheet';
		link.href = STYLE_HREF;
		document.head.appendChild(link);
	}

	/* 정화 목록(D-118)이 살려 주는 것만 남겼다. */
	var BUTTONS = [
		'undo', 'redo', '|',
		'bold', 'italic', 'underline', 'strikethrough', '|',
		'superscript', 'subscript', 'eraser', '|',
		'font', 'fontsize', 'brush', 'paragraph', '|',
		'ul', 'ol', 'outdent', 'indent', '|',
		'align', '|',
		'table', 'link', 'image', 'hr', '|',
		'source', 'preview', 'fullsize'
	];

	function create(element) {
		return Jodit.make(element, {
			height: 420,
			language: 'ko',
			// 창이 좁아도 단추를 숨기지 않는다. 결재 양식은 표 단추가 있어야 한다.
			toolbarAdaptive: false,
			buttons: BUTTONS,
			buttonsMD: BUTTONS,
			buttonsSM: BUTTONS,
			buttonsXS: BUTTONS,
			// 끌어다 놓은 그림을 base64 로 심지 않는다. 위 주석 참조.
			uploader: { insertImageAsBase64URI: false },
			showCharsCounter: false,
			showWordsCounter: false,
			showXPathInStatusbar: false
		});
	}

	document.addEventListener('DOMContentLoaded', function () {
		var element = document.getElementById(TARGET_ID);
		if (!element || typeof Jodit === 'undefined') {
			return;
		}

		loadStyles();
		var editor = create(element);

		/*
		 * 다른 화면 코드가 편집기를 만질 수 있게 내놓는다. 문서 양식 작성
		 * 화면이 HTML 파일을 읽어 편집기에 부어 넣는 데 쓴다.
		 */
		window.erflowEditor = editor;

		/*
		 * 폼을 보낼 때 원본 textarea 로 값을 옮긴다.
		 *
		 * 편집기가 알아서 맞춰 주기는 하지만, 서버로 가는 값이 편집기 상태와
		 * 어긋나면 «썼는데 빈 채로 저장되는» 조용한 손실이 된다 — 보내기 직전에
		 * 한 번 더 못 박는다.
		 */
		var form = element.form || element.closest('form');
		if (form) {
			form.addEventListener('submit', function () {
				element.value = editor.value;
			});
		}
	});
})();
