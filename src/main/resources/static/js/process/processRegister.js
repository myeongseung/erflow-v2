// 공정 등록 스크립트.
// 출처: legacy/ERFlow/src/main/webapp/js/process/processRegister.js
//
// 동작은 레거시 그대로다. 주소만 새 라우트로 바꿨다(D-005).
//   /ERFlow/process/processRegister    -> /process/processRegister
//   /ERFlow/process/processList.jsp    -> /process/list
document.addEventListener('DOMContentLoaded', function() {
	document.getElementById('process-register-button').addEventListener('click', function() {
		let processId = document.querySelector('input[name="processId"]').value.trim();
		let processName = document.querySelector('input[name="processName"]').value.trim();

		if (processId === '' || processName === '') {
			alert('공정ID와 공정명을 입력해주세요.');
			return false;
		}

		let table = $('#myTable');
		let priority = table.find('tr').length;  // 테이블 행의 개수를 기반으로 우선순위 설정
		const row = $('<tr></tr>');
		row.append('<td>' + processId + '</td>')
			.append('<td>' + processName + '</td>')
			.append('<td>' + priority + '</td>')
			.append('<td><button type="button" class="btn btn-danger btn-sm">삭제</button></td>');

		table.append(row);

		document.querySelector('input[name="processId"]').value = '';
		document.querySelector('input[name="processName"]').value = '';
	});

	// '삭제' 버튼을 눌렀을 때 행을 삭제하는 함수
	$(document).on('click', '.btn-danger', function() {
		$(this).closest('tr').remove();

		$('#myTable').find('tr').each(function(index, element) {
			if (index != 0) {
				$(element).find('td').eq(2).text(index);
			}
		});
	});

	window.removeRow = function(button) {
		button.closest('tr').remove();
	}

	// 엔터키를 누를 때 동작 방지
	$('.processId').on('keydown', function(e) {
		if (e.keyCode === 13) {
			e.preventDefault();
			$("#process-register-button").trigger("click");
		}
	});

	$('.processName').on('keydown', function(e) {
		if (e.keyCode === 13) {
			e.preventDefault();
			$("#process-register-button").trigger("click");
		}
	});
	$("#registerBtn").on("click", function() {

		let tableData = [];
		let flag = true;


		$("#myTable tbody tr").each(function() {
			var processId = $(this).find("td").eq(0).text();
			var processName = $(this).find("td").eq(1).text();
			var rowData = {
				processId: processId,
				processName: processName
			};
			tableData.push(rowData);
		});

		if (tableData.length == 0) {
			flag = false;
			alert("공정을 등록해주세요.");
			return
		}

		if (flag) {
			$.ajax({
				url: "/process/processRegister", // 서버 측 코드를 처리할 URL
				type: 'POST',
				contentType: 'application/json',
				data: JSON.stringify(tableData), // tableData 배열만 전송
				success: function(response) {
					if (response.status === 'success') {
						window.location.href = '/process/list';
					}
				},
				error: function(xhr, status, error) {
					alert('데이터 전송 중 오류가 발생했습니다.');
				}
			});
		}
	});
});


