// 근무 현황 원 그래프.
// 출처: legacy/ERFlow/src/main/webapp/js/admin/adminGraph.js
//
// D-125 에서 라벨과 색을 고쳤다. 이름은 서버가 붙여 보내고(WorkStatusCount)
// 색은 app.css 의 --color-status-* 에서 읽는다 — 달력·근태표와 같은 색이다.
//
// D-069 에서 이름을 조각 밖으로 뺐다. 아래 두 가지가 남아 있었다.
//
//   1. 이름이 조각 안에 그려졌다 — `${라벨} ${인원}명` 을 centroid 에 찍었다.
//      조각이 작으면 글자가 조각보다 길어 밖으로 삐져나가 잘리고, 이웃한 작은
//      조각끼리는 서로 겹쳤다. 조각 크기는 그날 자료가 정하므로 «적당한 자리»
//      라는 것이 없다. 이름을 밖으로 빼서 목록으로 세우면 겹칠 자리가 없다.
//
//   2. <svg> 안에 <svg> 를 또 넣었다 — 심는 자리가 이미 <svg id="graph"> 인데
//      `d3.select("#graph").append("svg")` 를 했다. 심는 자리를 <div> 로 바꿨다.
//
// 가운데를 비운 것(도넛)은 멋이 아니라 자리다. 그 자리에 그날 인원 합계를 적는다.
document.addEventListener('DOMContentLoaded', function() {
	// 상태 코드 -> CSS 사용자 정의 속성. app.css 가 값을 갖는다.
	const STATUS_VARS = [
		'--color-status-absent',   // 0 결근
		'--color-status-working',  // 1 근무 중
		'--color-status-done',     // 2 퇴근
		'--color-status-early',    // 3 조퇴
		'--color-status-late',     // 4 지각
		'--color-status-half',     // 5 반차
		'--color-status-off',      // 6 연차
	]

	// 조각 안에 비율을 적는 최소 크기. 이보다 작으면 숫자도 조각을 넘는다.
	const LABEL_MIN_SHARE = 0.07

	const SIZE = 260
	const RADIUS = SIZE / 2 - 4
	const HOLE = RADIUS * 0.58

	const rootStyle = getComputedStyle(document.documentElement)
	function statusColor(status) {
		const name = STATUS_VARS[status]
		const value = name ? rootStyle.getPropertyValue(name).trim() : ''
		return value || '#dbdbdb'
	}

	const box = d3.select('#graph')

	function draw(rows) {
		const total = rows.reduce(function(sum, row) { return sum + row.value }, 0)
		if (total === 0) {
			box.append('p').attr('class', 'graph-empty').text('오늘 근무 기록이 없습니다')
			return
		}

		const body = box.append('div').attr('class', 'graph-body')

		const svg = body.append('svg')
			.attr('class', 'graph-donut')
			.attr('viewBox', '0 0 ' + SIZE + ' ' + SIZE)
		const g = svg.append('g')
			.attr('transform', 'translate(' + (SIZE / 2) + ',' + (SIZE / 2) + ')')

		const pie = d3.pie().value(function(d) { return d.value }).sort(null)
		const arc = d3.arc().outerRadius(RADIUS).innerRadius(HOLE)

		const slices = g.selectAll('.pie').data(pie(rows)).enter()
			.append('g').attr('class', 'pie')

		slices.append('path')
			.attr('d', arc)
			.attr('fill', function(d) { return statusColor(d.data.status) })
			.attr('stroke', 'white')
			.attr('stroke-width', 1.5)

		// 조각 안에는 비율만 적는다. 이름은 옆의 목록이 갖는다.
		slices.append('text')
			.attr('class', 'graph-share')
			.attr('transform', function(d) { return 'translate(' + arc.centroid(d) + ')' })
			.attr('dy', '0.35em')
			.attr('text-anchor', 'middle')
			.text(function(d) {
				const share = d.data.value / total
				return share < LABEL_MIN_SHARE ? '' : Math.round(share * 100) + '%'
			})

		// 도넛 가운데 — 그날 인원 합계
		const middle = g.append('text').attr('class', 'graph-total').attr('text-anchor', 'middle')
		middle.append('tspan').attr('x', 0).attr('dy', '-0.1em')
			.attr('class', 'graph-total-value').text(total)
		middle.append('tspan').attr('x', 0).attr('dy', '1.4em')
			.attr('class', 'graph-total-unit').text('명')

		const legend = body.append('ul').attr('class', 'graph-legend')
		const item = legend.selectAll('li').data(rows).enter().append('li')
		item.append('span')
			.attr('class', 'graph-swatch')
			.style('background-color', function(d) { return statusColor(d.status) })
		item.append('span').attr('class', 'graph-name').text(function(d) { return d.label })
		item.append('span').attr('class', 'graph-value').text(function(d) { return d.value + '명' })
	}

	$.ajax({
		type: 'get',
		url: '/admin/graph/view',
		dataType: 'json',
		success: function(rows) {
			draw(rows || [])
		},
		error: function(xhr) {
			console.log(xhr);
			box.append('p').attr('class', 'graph-empty').text('근무 현황을 불러오지 못했습니다')
		}
	});
});
