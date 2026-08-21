$(document).ready(function() {
	$(".a_company_info").on("click", function(e) {
		e.preventDefault();
		
		let _key = $(this).data("key"); 
		let _value = $(this).data("value");
		
		window.opener.receiveCompanyInfo(_key, _value);
		window.close();
	});
});