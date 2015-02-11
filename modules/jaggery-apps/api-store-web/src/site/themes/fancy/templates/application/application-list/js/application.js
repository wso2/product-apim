function changeAppNameMode(linkObj){
    jagg.sessionAwareJS({redirect:'site/pages/applications.jag'});
    var theTr = $(linkObj).parent().parent();
    var appName = $(theTr).attr('data-value');
    $('td:first',theTr).html('<div class="row-fluid"><div class="span6"> <input class="app_name_new" maxlength="70" value="'
        +'" type="text" /> </div></div> ');
    $('td:first',theTr).find(".app_name_new").val(theTr.attr('data-value'));
    $('td:eq(3)',theTr).html('<div class="row-fluid"><div class="span6"> <input class="callback_new validInput" maxlength="70" value="'
        +'" type="text" /> </div></div> ');
    $('td:eq(3)',theTr).find(".callback_new").val(theTr.attr('callback-value'));
    $('td:eq(4)',theTr).html('<div class="row-fluid"><div class="span6"> <input class="description-new" maxlength="70" value="'
        +'" type="text" /> </div></div> ');
    $('td:eq(4)',theTr).find(".description-new").val(theTr.attr('description-value'));
    //Hide the Edit link
    $("td:eq(4)", theTr).children("a").hide();
    $("td:eq(5)", theTr).children("a").hide();
    //Show the Save and Cancel buttons
    $("td:eq(4)", theTr).children("div").show();
    $("td:eq(5)", theTr).children("div").show();

    $('input.app_name_new',theTr).focus();
    $('input.app_name_new',theTr).keyup(function(){
        var error = "";
        var illegalChars = /([~!#$;%^*+={}\|\\<>\"\'\/,])/;
        if($(this).val() == ""){
            error = i18n.t('validationMsgs.fieldRequired');
        }else if($(this).val().length>70){
            error = i18n.t('validationMsgs.exceedChars');
        }else if(/(["\'])/g.test($(this).val())){
            error = i18n.t('validationMsgs.illegalChars')+'( " \' )';
        } else if ($(this).val().search(illegalChars) != -1) {
            error = i18n.t('validationMsgs.illegalChars');
        }
        if(error != ""){
            $(this).addClass('error');
            if(!$(this).next().hasClass('error')){
                $(this).parent().append('<label class="error">'+error+'</label>');
            }else{
                $(this).next().show().html(error);
            }
        }else{
            $(this).removeClass('error');
            $(this).next().hide();
        }
    });
    $('input.callback_new',theTr).focus();
    $('input.callback_new',theTr).keyup(function(){
        var error = "";
        var illegalChars = /([<>\"\'])/;
        if(/(["\'])/g.test($(this).val())){
            error = i18n.t('validationMsgs.illegalChars')+'( " \' )';
        } else if ($(this).val().search(illegalChars) != -1) {
            error = i18n.t('validationMsgs.illegalChars');
        }
        if(error != ""){
            $(this).addClass('error');
            if(!$(this).next().hasClass('error')){
                $(this).parent().append('<label class="error">'+error+'</label>');
            }else{
                $(this).next().show().html(error);
            }
        }else{
            $(this).removeClass('error');
            $(this).next().hide();
        }
    });
    var row = $(linkObj).parent().parent();
    $("td:eq(1)", theTr).children("select").removeAttr("disabled");
}
function updateApplication_reset(linkObj){
    jagg.sessionAwareJS({redirect:'site/pages/applications.jag'});
    var theTr = $(linkObj).parent().parent().parent();
    var appName = $(theTr).attr('data-value');
    var tier = $(theTr).attr('tier-value');
    var callbackUrl = $(theTr).attr('callback-value');
    var description = $(theTr).attr('description-value');
    $('td:first',theTr).text(appName);
    $("td:eq(1)", theTr).children("select").val(tier);
    $("td:eq(1)", theTr).children("select").attr("disabled", "disabled");
    $('td:eq(3)',theTr).text(callbackUrl);
    $('td:eq(4)',theTr).text(description);
    //Hide the Save and Cancel buttons
    $("td:eq(5)", theTr).children("div").hide();
    //Show the Edit link
    $("td:eq(5)", theTr).children("a").show();
}
function updateApplication(linkObj){
    jagg.sessionAwareJS({redirect:'site/pages/applications.jag'});
    var theTr = $(linkObj).parent().parent().parent();
    var applicationOld = $(theTr).attr('data-value');
    var applicationNew = $('input.app_name_new',theTr).val();
    var callbackUrlNew = $('input.callback_new',theTr).val();
    var descriptionNew = $('input.description-new',theTr).val();
    var tier = $("td:eq(1)", theTr).children("select").val();
    var error = "";
    var illegalChars = /([~!#$;%^*+={}\|\\<>\"\'\/,])/;
    if (applicationNew == "") {
        error =  i18n.t("validationMsgs.fieldRequired");
    } else if (applicationNew.length > 70) {
        error = i18n.t('validationMsgs.exceedChars');
    } else if (/(["\'])/g.test(applicationNew)) {
        error = i18n.t('validationMsgs.illegalChars')+'( " \' )';
    }else if (applicationNew.search(illegalChars)!=-1) {
        error = i18n.t('validationMsgs.illegalChars');
    }
    if(error != ""){
        return;
    }
        jagg.post("/site/blocks/application/application-update/ajax/application-update.jag", {
            action:"updateApplication",
            applicationOld:applicationOld,
            applicationNew:applicationNew,
            tier:tier,
            callbackUrlNew:callbackUrlNew,
            descriptionNew:descriptionNew
        }, function (result) {
            if (result.error == false) {
                window.location.reload();
            } else {
                jagg.message({content:result.message,type:"error"});
            }
        }, "json");
}

function deleteApp(linkObj) {
    jagg.sessionAwareJS({redirect:'site/pages/applications.jag'});
    var theTr = $(linkObj).parent().parent();
    var appName = $(theTr).attr('data-value');
    $('#messageModal').html($('#confirmation-data').html());
    $('#messageModal h3.modal-title').html(i18n.t('confirm.delete'));
    $('#messageModal div.modal-body').text('\n\n'+i18n.t('confirm.deleteMsg')+'"' + appName + '"'+i18n.t('confirm.deleteMsgPostfix'));
    $('#messageModal a.btn-primary').html(i18n.t('info.yes'));
    $('#messageModal a.btn-other').html(i18n.t('info.no'));
    $('#messageModal a.btn-primary').click(function() {
        jagg.post("/site/blocks/application/application-remove/ajax/application-remove.jag", {
            action:"removeApplication",
            application:appName
        }, function (result) {
            if (!result.error) {
                window.location.reload(true);
            } else {
                jagg.message({content:result.message,type:"error"});
            }
        }, "json");
    });
    $('#messageModal a.btn-other').click(function() {
        window.location.reload(true);
    });
    $('#messageModal').modal();

}

function hideMsg() {
    $('#applicationTable tr:last').css("background-color", "");
    $('#appAddMessage').hide("fast");
}
$(document).ready(function() {
    if ($.cookie('highlight') != null && $.cookie('highlight') == "true") {
        $.cookie('highlight', "false");

        $('#applicationTable tr:last').css("background-color", "#d1dce3");
        if($.cookie('lastAppStatus')=='CREATED'){
        $('#appAddPendingMessage').show();
        $('#applicationPendingShowName').text($.cookie('lastAppName'));
        var t = setTimeout("hideMsg()", 3000);
        }else{
        $('#appAddMessage').show();
        $('#applicationShowName').text($.cookie('lastAppName'));
        var t = setTimeout("hideMsg()", 3000);

        }
    }
});
