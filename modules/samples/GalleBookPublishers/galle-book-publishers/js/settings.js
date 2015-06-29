$('#saveButton').on('click', function () {

  var status = $("#settings-form").validationEngine('validate');

  if(status){
      var gatewayUrl = $('#gatewayUrl').val();
      var consumerKey = $('#consumerKey').val();
      var consumerSecret = $('#consumerSecret').val();

      $.ajax({
        method: "POST",
        url: "settings",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        data: JSON.stringify({"gatewayUrl":gatewayUrl, "consumerKey":consumerKey, "consumerSecret":consumerSecret})
      })
      .done(function( msg ) {
        alert("Saved successfully ! ");
      });
  }
});
