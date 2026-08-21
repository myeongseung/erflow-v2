$(document).ready(function() {
	$("a").on("click", function(e) {
		e.preventDefault();
		
		let _key = $(this).data("key"); 
		let _value = $(this).data("value");
		
		window.opener.receiveProductInfo(_key, _value);
		window.close();
	});
});