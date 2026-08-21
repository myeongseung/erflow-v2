function block(base, blk) {
	document.readFrm.nowPage.value = base * (blk - 1) + 1;
	document.readFrm.submit();
}

function paging(page) {
	document.readFrm.nowPage.value = page;
	document.readFrm.submit();
}

function search() {
	document.readFrm.keyword.value = document.searchFrm.keyword.value;
	document.readFrm.submit();
}