function tierChanged(element){
    var index = element.selectedIndex;
    var selectedDesc = $("#tierDescriptions").val().split(",")[index];
    $("#tierHelpStr").text(selectedDesc);
}

$(document).ready(function () {
    $.ajaxSetup({
      contentType: "application/x-www-form-urlencoded; charset=utf-8"
    });

    var application = $("#application-name").val("");

     $.validator.addMethod('validateSpecialChars', function(value, element) {
        return !/(["\'])/g.test(value);
     }, 'The Name contains one or more illegal characters' + '( &nbsp;&nbsp; " &nbsp;&nbsp; \' &nbsp;&nbsp; )');

    $("#appAddForm").validate({
        submitHandler: function(form) {
            applicationAdd();
        }
    });
    var applicationAdd = function(){
        var application = $("#application-name").val();
        var tier = $("#appTier").val();
        var callbackUrl = $("#callback-url").val();
        var apiPath = $("#apiPath").val();
        var goBack = $("#goBack").val();
        var description = $("#description").val();
        var status='';
        jagg.post("/site/blocks/application/application-add/ajax/application-add.jag", {
            action:"addApplication",
            application:application,
            tier:tier,
            callbackUrl:callbackUrl,
            description:description
        }, function (result) {
            if (result.error == false) {
                status=result.status;
                var date = new Date();
                date.setTime(date.getTime() + (3 * 1000));
                $.cookie('highlight','true',{ expires: date});
                $.cookie('lastAppName',application,{ expires: date});
                $.cookie('lastAppStatus',status,{ expires: date});
                if(goBack == "yes"){
                    jagg.message({content:i18n.t('info.returntoAPIPage'),type:'confirm',okCallback:function(){
                    window.location.href = apiViewUrl + "?" +  apiPath;
                    },cancelCallback:function(){
                        window.location.reload(true);
                    }});
                } else{
                    window.location.reload(true);
                }

            } else {
                jagg.message({content:result.message,type:"error"});
            }
        }, "json");
    };


    $("#application-name").charCount({
			allowed: 70,
			warning: 50,
			counterText: 'Characters left: '
		});
    $("#application-name").val('');

    /*$('#application-name').keydown(function(event) {
         if (event.which == 13) {
               applicationAdd();
            }
        });*/
   
    $('.help_popup').click(function()
	    {
	        $('#callback_help').toggle('fast', function()
	        {
	            $('#callback_help').html(i18n.t('info.callBackHelpMsg'));
	        });
	        return false;
	    });
});

