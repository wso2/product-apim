var login = function () {
    var name = $("#username").val();
    var pass = $("#pass").val();
    var tenantDomain = $("#tenant").val();
    jagg.post("/site/blocks/user/login/ajax/login.jag", { action:"login", username:name, password:pass,tenant:tenantDomain },
              function (result) {
                  if (!result.error) {
                      var current = window.location.pathname;
                      var currentHref=window.location.search;
                      var queryParam;
                      if(currentHref.indexOf("tenant")>-1){queryParam=currentHref;}
                      else{queryParam='';}
                      if (current.indexOf(".jag") >= 0) {
                          location.href = "index.jag"+queryParam;
                      } else {
                          location.href = 'site/pages/index.jag'+queryParam;
                      }

                  } else {
                      $('#loginError').show('fast');
                      $('#loginErrorSpan').html('<strong>'+i18n.t("errorMsgs.login")+'</strong><br />' + result.message);
                  }
              }, "json");


};

$(document).ready(
        function() {
            $('#username').focus();
            $('#username').keydown(function(event) {
                if (event.which == 13) {
                    event.preventDefault();
                    login();
                }
            });
            $('#pass').keydown(function(event) {
                if (event.which == 13) {
                    event.preventDefault();
                    login();
                }
            });
        }
        );


