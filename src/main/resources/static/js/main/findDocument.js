$(document).ready(function() {
   $(".a_document_info").on("click", function(e) {
      e.preventDefault();
      
      let _key = $(this).data("key");
      let _value = $(this).data("value");
      window.opener.receiveDocumentInfo(_key, _value);
      window.close();
   });
});