$(document).ready(function(){

    $(".implementation_methods").change(function(event){
        $(".implementation_method").hide();
        $(".implementation_method_"+$(this).val()).show();
    });


    $('#endpointType').on('change',function(){
        var endpointType = $('#endpointType').find(":selected").val();
        if(endpointType == "secured"){
            $('#credentials').show();
        }
        else{
            $('#credentials').hide();
        }
    });
    $('#endpointType').trigger('change');

    /*$("#implement_form").submit(function (e) {
      e.preventDefault();
    });*/

    var v = $("#implement_form").validate({
        submitHandler: function(form) {        
        var designer = APIDesigner();
        APP.update_ep_config();
        $('#swagger').val(JSON.stringify(designer.api_doc));
        $('#saveMessage').show();
        $('#saveButtons').hide();
        $(form).ajaxSubmit({
            success:function(responseText, statusText, xhr, $form) {
             if (!responseText.error) {
                var designer = APIDesigner();
                designer.saved_api = {};
                designer.saved_api.name = responseText.data.apiName;
                designer.saved_api.version = responseText.data.version;
                designer.saved_api.provider = responseText.data.provider;                
                $('#saveMessage').hide();
                $('#saveButtons').show();                
                $( "body" ).trigger( "api_saved" );                             
             } else {
                 if (responseText.message == "timeout") {
                     if (ssoEnabled) {
                         var currentLoc = window.location.pathname;
                         if (currentLoc.indexOf(".jag") >= 0) {
                             location.href = "index.jag";
                         } else {
                             location.href = 'site/pages/index.jag';
                         }
                     } else {
                         jagg.showLogin();
                     }
                 } else {
                     jagg.message({content:responseText.message,type:"error"});
                 }
                 $('#saveMessage').hide();
                 $('#saveButtons').show();
             }
            }, dataType: 'json'
        });
        }
    });
    
    $("#prototyped_api").click(function(e){
        $("body").on("api_saved", function(e){
            $("body").unbind("api_saved");    
                var designer = APIDesigner();            
                $.ajax({
                    type: "POST",
                    url: jagg.site.context + "/site/blocks/life-cycles/ajax/life-cycles.jag",
                    data: {
                        action :"updateStatus",
                        name:designer.saved_api.name,
                        version:designer.saved_api.version,
                        provider: designer.saved_api.provider,
                        status: "PROTOTYPED",
                        publishToGateway:true,
                        requireResubscription:true
                    },
                    success: function(responseText){
                        if (!responseText.error) {
                            jagg.message({content:"API deployed as a Prototype.",type:"info"});
                        }else{
                             if (responseText.message == "timeout") {
                                 if (ssoEnabled) {
                                     var currentLoc = window.location.pathname;
                                     if (currentLoc.indexOf(".jag") >= 0) {
                                         location.href = "index.jag";
                                     } else {
                                    	 location.href = 'site/pages/index.jag';
                                     }
                                 } else {
                                     jagg.showLogin();
                                 }
                             } else {
                                 jagg.message({content:responseText.message,type:"error"});
                             }
                        }
                    },
                    dataType: "json"
                });               
            });
            $("#implement_form").submit();                        
        });

});
