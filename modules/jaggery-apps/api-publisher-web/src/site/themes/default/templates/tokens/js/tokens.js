var revokeAccessToken = function(k) {
    var accessToken = $("#accessToken-"+k).val();
    var consumerKey = $("#consumerKey-"+k).val();
    var authorizedUser = $("#authorizedUser-"+k).val();
    jagg.post("/site/blocks/tokens/ajax/revokeToken.jag", { action:"revokeAccessToken", accessToken:accessToken,consumerKey:consumerKey,authUser:authorizedUser },
             function (result) {
                 if (!result.error) {

                     $('#messageModal').html($('#confirmation-data').html());
                     $('#messageModal h3.modal-title').html(i18n.t('confirm.revoke'));
                     $('#messageModal div.modal-body').html('\n\n'+i18n.t('confirm.revokeMsg')+'<b>' + accessToken + '</b>');
                     $('#messageModal a.btn-primary').html(i18n.t('confirm.ok'));
                     $('#messageModal a.btn-other').hide();
                     $('#messageModal a.btn-primary').click(function() {
                         var current = window.location.pathname;
                         if (current.indexOf(".jag") >= 0) {
                             location.href = "tokens.jag";
                         } else {
                             location.href = 'site/pages/tokens.jag';
                         }
                     });

                     $('#messageModal').modal();


                 } else {
                     jagg.message({content:result.message,type:"error"});
                     $('#populatedDataDiv #search').attr("disabled", false);
                     $("#catogName").val('');

                 }
             }, "json");


};


