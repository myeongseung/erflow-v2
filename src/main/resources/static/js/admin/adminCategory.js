$(function() {
	const keys = [ '퇴근', '출근', '지각', '조퇴', '결근', '연차' ];
	const values = [ '#ADFF2F', '#ADFF2F', '#FFFF00', '#87CEEB', '#E34F5E', '#DBDBDB' ];
	let span = $('.attendance-management-content .input-group-text').get();
	let div = $('.attendance-management-content .category').get();
	
	for (let i = 0; i < div.length; ++i) {
		const curDiv = div[i];
		const curSpan = span[i];
		const text = $(curDiv).text();
		let index = 0;
		
		for (let j = 0; j < keys.length; ++j) {
			if (text === keys[j]) {
				index = j;
				j = keys.length;
			}
		}
		$(curSpan).css('background-color', values[index]);
	}
})