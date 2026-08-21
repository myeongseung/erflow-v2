$(document).ready(function() {
	$(".a-work-info").on("click", function(e) {
		e.preventDefault();
		
		let _key = $(this).data("key"); 
		let _value = $(this).data("value");
		
		window.opener.receiveWorkInfo(_key, _value);
		window.close();
	});
});