// 결재 등록 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/proposal/proposalRegister.js
//
// 동작은 레거시 그대로다. 팝업 주소만 새 라우트로 바꿨다(D-005).
//   ../findDocument.jsp      -> /find/document
//   ../findProposalRoute.jsp -> /find/proposal-route
$(function() {
	$("#find-document").on("click", function() {
		const width = 584;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/document', '_blank', features);
	});

	$("#find-route").on("click", function() {
		const width = 650;
		const height = 400;
		const left = (screen.width - width) / 2;
		const top = (screen.height - height) / 2;
		const features = `width=${width},height=${height},left=${left},top=${top}`;

		window.open('/find/proposal-route', '_blank', features);
	});

	jQuery("table > tbody").sortable({
		start: function(event, ui) {
			ui.item.addClass("selected");
		}
		, stop: function(event, ui) {
			ui.item.removeClass("selected");
		}
	});
	$("#myTable").on("click", ".delete-link", function() {
		$(this).parent().parent().remove();
	});
});

function receiveDocumentInfo(documentId, documentName) {
	document.proposalFrm.documentId.value = documentId;
	document.proposalFrm.documentName.value = documentName;
}

function receiveRouteInfo(routeId, route, nickname) {
	document.proposalFrm.routeId.value = routeId;
	document.proposalFrm.nickname.value = nickname;
	document.proposalFrm.route.value = route;
}
