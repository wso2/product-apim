$(document).ready(function(){
    $( "body" ).delegate( "button.check_url_valid", "click", function() {
        var btn = this;
        var url = $(this).parent().find('input:first').val();
        var type = $.parseJSON($("#endpoint_config").val())['endpoint_type']
        $(btn).parent().parent().find('.url_validate_label').remove();
        $(btn).addClass("loadingButton-small");
        $(btn).val(i18n.t('validationMsgs.validating'));

        if (url == '') {
            $(btn).parent().after(' <span class="label label-important url_validate_label"><i class="icon-remove icon-white"></i>'+ i18n.t('validationMsgs.invalid')+'</span>');
            var toFade = $(btn).next();
            $(btn).removeClass("loadingButton-small");
            $(btn).val(i18n.t('validationMsgs.testUri'));
            var foo = setTimeout(function(){$(toFade).hide()},3000);
            return;
        }
        if (!type) {
            type = "";
        }
        jagg.post("/site/blocks/item-add/ajax/add.jag", { action:"isURLValid", type:type,url:url },
                  function (result) {
                      if (!result.error) {
                          if (result.response == "success") {
                              $(btn).parent().after(' <span class="label label-success url_validate_label"><i class="icon-ok icon-white"></i>'+ i18n.t('validationMsgs.valid')+'</span>');

                          } else {
                              $(btn).parent().after(' <span class="label label-important url_validate_label"><i class="icon-remove icon-white"></i>'+ i18n.t('validationMsgs.invalid')+'</span>');
                          }
                          var toFade = $(btn).parent().parent().find('.url_validate_label');
                          var foo = setTimeout(function() {
                                $(toFade).hide();
                          }, 3000);

                      }
                      $(btn).removeClass("loadingButton-small");
                      $(btn).val(i18n.t('validationMsgs.testUri'));
                  }, "json");

    });
});
