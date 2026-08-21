// 근무 현황 원 그래프.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/adminGraph.js
//
// 동작은 레거시 그대로다. 값을 받아 오는 주소만 바꿨다.
//   /ERFlow/admin/graph/view -> /admin/graph/view
document.addEventListener('DOMContentLoaded', function() {
	$.ajax({
		type: 'get',
		url: '/admin/graph/view',
		dataType: 'json',
		success: function(rows) {
			const keys = ['결근','출근', '퇴근', '지각', '조퇴', '반차', '연차']
			let dataset = []
			let names = []
			let values = []			
			
			$.each(rows, function(key, value) {
				names.push(`${keys[key]} ${value}명`)
				values.push(value)
			});
			let width = 350; // 그래프 넓이
			let height = 350; // 그래프 높이
			// 이 밑은 그래프 그리는 js 입니다.
			// 1. 데이터 준비
			
			for (let i = 0; i < keys.length; ++i) {
				dataset.push({ "name": names[i], "value": values[i] });
			}
			let radius = Math.min(width, height) / 2 - 10; // 차트의 반경을 설정

			// 2. SVG 영역 설정
			// id가 'graph'인 태그에 svg(벡터그림)을 추가한다.
			let svg = d3.select("#graph").append("svg").attr("width", width).attr("height", height);

			// 축의 위치를 조정한다.
			let g = svg.append("g").attr("transform", "translate(" + (width / 2) + "," + (height / 2) + ")");

			// 3. 컬러 설정
			// scaleOrdinal() 은 range 로 설정된 배열을 반복적으로 호출하는 함수를 설정
			let color = d3.scaleOrdinal()
				.range(["#ADFF2F", "#FFFF00", "#87CEEB", "#E34F5E", "#DBDBDB"]); // 출근, 지각, 조퇴, 결근, 연차

			// 4. pie 차트 dateset에 대한 함수 설정
			let pie = d3.pie()
				.value(function(d) { return d.value; })
				.sort(null);

			// 5. pie 차트 SVG 요소 설정
			let pieGroup = g.selectAll(".pie")
				.data(pie(dataset))
				.enter()
				.append("g")
				.attr("class", "pie");

			arc = d3.arc()
				.outerRadius(radius)
				.innerRadius(0);

			pieGroup.append("path")
				.attr("d", arc)
				.attr("fill", function(d) { return color(d.index) })
				.attr("opacity", 0.75)
				.attr("stroke", "white");

			// 6. pie 차트 텍스트 SVG 요소 설정
			var text = d3.arc()
				.outerRadius(radius - 50)
				.innerRadius(radius - 50);

			pieGroup.append("text")
				.attr("fill", "black")
				.attr("transform", function(d) { return "translate(" + text.centroid(d) + ")"; })
				.attr("dy", "5px")
				.attr("font-size", "15px")
				.attr("text-anchor", "middle")
				.text(function(d) { return d.data.name; });
		},
		error: function(xhr) {
			console.log(xhr);
		}
	});
});