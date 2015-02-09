var removeAPI = function(name, version, provider, buttonElement) {
    var apiThumbnail = $(buttonElement).closest(".thumbnail");
    jagg.message({
        content:"Are you sure you want to delete the API - " + name + " - " + version ,
        type:"confirm",
        title:"Confirm Delete",
        anotherDialog:true,
        okCallback:function(){
            $('#messageModal').modal({backdrop: 'static', keyboard: false });
            $(".modal-header .close").hide();
            $(".modal-footer").html("");
            $(".modal-title").html("Please wait");
            $(".modal-body").addClass("loadingButton");
            $(".modal-body").css({"margin-left":25});
            $(".modal-body").html("Deleting API : "+ name + " - " + version );
            $(".modal").css({width:350});

            buttonElement.hidden = true;
            apiThumbnail.hide();

            jagg.post("/site/blocks/item-add/ajax/remove.jag", { action:"removeAPI", name:name, version:version, provider:provider },
                      function (result) {

                          $(".modal-header .close").show();
                          $(".modal-body").css({"margin-left":0});
                          $(".modal-body").html("");
                          $(".modal").css({width:560});
                          $(".modal-body").removeClass("loadingButton");
                          $("#messageModal").hide();

                          if (result.message == "timeout") {
                              if (ssoEnabled) {
                                  var current = window.location.pathname;
                                  if (current.indexOf(".jag") >= 0) {
                                      location.href = "index.jag";
                                  } else {
                                      location.href = 'site/pages/index.jag';
                                  }
                              } else {
                                  $("#messageModal").show();
                                  jagg.showLogin();
                              }
                          }
                  else if (!result.error) {
                      window.location.reload();
                  }else{
                       $("#messageModal").show();
                       jagg.message({content:result.message,type:"error"});
                       buttonElement.hidden = false;
                       apiThumbnail.show();
                  }
            }, "json");

    }});

};

var selectUserTab = function(path){
    $.cookie("selectedTab","users");
    location.href = path;
};
$(document).ready(
         function() {
             if (($.cookie("selectedTab") != null)) {
                 $.cookie("selectedTab", null);
             }

         }
);

