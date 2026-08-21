$(document).ready(function() {
   $(".a_route_info").on("click", function(e) {
      e.preventDefault();
      
      let _key = $(this).data("key"); 
      let _value = $(this).data("value");
      let _name = $(this).data("name");
      
      window.opener.receiveRouteInfo(_key, _value, _name);
      window.close();
   });
});