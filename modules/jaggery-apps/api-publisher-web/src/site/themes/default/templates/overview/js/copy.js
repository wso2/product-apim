var copyAPIToNewVersion = function (provider) {
    var apiName = $("#overviewAPIName").val();
    var version = $("#overviewAPIVersion").val();
    var newVersion = $("#copy-api #new-version").val();
    var isDefaultVersion=$('#default_version_checked_cp').val();

    jagg.post("/site/blocks/overview/ajax/overview.jag", { action:"createNewAPI", provider:provider,apiName:apiName, version:version, newVersion:newVersion ,isDefaultVersion:isDefaultVersion},
              function (result) {
                  if (!result.error) {

                      $("#copy-api #new-version").val('');
                      var current = window.location.pathname;
                      if (current.indexOf(".jag") >= 0) {
                          location.href = "index.jag";
                      } else {
                          location.href = 'site/pages/index.jag';
                      }

                  } else {
                      if (result.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:result.message,type:"error"});
                      }
                  }
              }, "json");

};

$(document).ready(
   function() {
       $('#copyApiForm').validate({
           submitHandler: function(form) {
               copyAPIToNewVersion(provider)
           }
       });

      $('.default_version_check_cp').change(function(){
          if($(this).is(":checked")){
              $(default_version_checked_cp).val($(this).val());
          }else{
              $(default_version_checked_cp).val("");
          }
      });

   }
);