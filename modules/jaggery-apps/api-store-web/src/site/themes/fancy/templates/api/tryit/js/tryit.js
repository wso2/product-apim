var putInsidePre = function (txt,toPre){
    var preText = document.createTextNode(txt);
    document.getElementById(toPre).appendChild(preText);
};

$('#sendBtn').click(
        function() {
            $('#progressBar').show('fast');
            $('.tryit-header-section').css('height', '120px');
            $('#sendBtn').attr('disabled', 'disabled').val('Sending...');

            var req_url = $('#req_url').val();
            var req_verb = $('#req_verb').val();
            var req_body = $('#req_body').val();
            var content_type = $('#content_type').val();
            console.info(content_type);

            var req_body_candidate  = "";
            if(req_verb=="POST" && req_body == ""){
                if(req_url.split('?') > 0){
                    req_body_candidate = req_url.split('?')[1];
                }
            }
            if(req_body_candidate != ""){
                req_body = req_body_candidate;
            }

            $.ajax({
                       url:jagg.site.context + "/site/blocks/api/tryit/ajax/api-tryit.jag",
                       data:{action:"makeCall",
                           req_url:req_url,
                           req_verb:req_verb,
                           req_headers:$('#req_headers').val(),
                           req_body:req_body,
                           content_type:content_type
                       },
                       statusCode:{
                           200:function(result) {
                               if(result.split("**#*#**#*#").length <= 1){
                                   //Error happened
                                   $('#responseSection').show();
                                   $('#responseDivHeaders').html('');

                                   $('#responseDivContent').html(result);

                                   $('.tryit-header-section').css('height', '');
                                   $('#progressBar').hide('fast');
                                   $('#sendBtn').removeAttr('disabled').val('Send');

                                   $('html, body').animate({
                                       scrollTop: $(".tryit-req-res").offset().top
                                   }, 500);
                                   return;
                               }
                               $('#responseSection').show();
                               var content = result.split("**#*#**#*#")[1];
                               result = jQuery.parseJSON(result.split("**#*#**#*#")[0]);
                               var resDivHeadersHtml = '';
                               var reqDivHeadersHtml = '';
                               var printRow = function(row) {
                                   if (row.right == "null" || row.right == null || row.right == undefined || row.right == "undefined") {
                                       return "";
                                   } else {
                                       return '<div class="row-fluid"><div class="span3"><strong>' + row.left + '</strong></div><div class="span9">' + row.right + '</div></div>';
                                   }
                               };

                               resDivHeadersHtml += printRow({left:'Ready State:',right:result.rh_rs});
                               resDivHeadersHtml += printRow({left:'Status:',right:result.rh_st});
                               resDivHeadersHtml += printRow({left:'Status Text:',right:result.rh_stt});
                               resDivHeadersHtml += printRow({left:'Content-Type:',right:result.rh_ct});
                               resDivHeadersHtml += printRow({left:'Age:',right:result.rh_ag});
                               resDivHeadersHtml += printRow({left:'Cache-Control:',right:result.rh_cc});
                               resDivHeadersHtml += printRow({left:'Content-Encoding:',right:result.rh_ce});
                               resDivHeadersHtml += printRow({left:'Expires:',right:result.rh_ex});
                               resDivHeadersHtml += printRow({left:'Server:',right:result.rh_se});
                               resDivHeadersHtml += printRow({left:'Vary:',right:result.rh_va});
                               resDivHeadersHtml += printRow({left:'Via:',right:result.rh_vi});
                               resDivHeadersHtml += printRow({left:'X-Frame-Options:',right:result.rh_xf});
                               resDivHeadersHtml += printRow({left:'X-Transaction:',right:result.rh_xt});
                               resDivHeadersHtml += printRow({left:'X-Varnish:',right:result.rh_xv});
                               resDivHeadersHtml += printRow({left:'Date:',right:result.rh_da});
                               resDivHeadersHtml += printRow({left:'Transfer-Encoding:',right:result.rh_te});
                               resDivHeadersHtml += printRow({left:'Connection:',right:result.rh_co});

                               reqDivHeadersHtml += printRow({left:'Origin:',right:result.req_or});
                               reqDivHeadersHtml += printRow({left:'User-Agent:',right:result.req_aa});
                               reqDivHeadersHtml += printRow({left:'Content-Type:',right:result.req_ct});
                               reqDivHeadersHtml += printRow({left:'Accept:',right:result.req_ac});
                               reqDivHeadersHtml += printRow({left:'Accept-Encoding:',right:result.req_ae});
                               reqDivHeadersHtml += printRow({left:'Accept-Language:',right:result.req_al});
                               reqDivHeadersHtml += printRow({left:'Accept-Charset:',right:result.req_al});


                               $('#requestDivHeaders').html(reqDivHeadersHtml);
                               $('#responseDivHeaders').html(resDivHeadersHtml);

                               $('#responseDivContent').html('');
                               putInsidePre(content,'responseDivContent');

                               $('.tryit-header-section').css('height', '');
                               $('#progressBar').hide('fast');
                               $('#sendBtn').removeAttr('disabled').val('Send');

                               $('html, body').animate({
                                                           scrollTop: $(".tryit-req-res").offset().top
                                                       }, 500);

                           },
                       500:function(result){

                           $('#responseSection').show();
                           $('#responseDivHeaders').html();

                           $('#responseDivContent').html(result.statusText);

                           $('.tryit-header-section').css('height', '');
                           $('#progressBar').hide('fast');
                           $('#sendBtn').removeAttr('disabled').val('Send');

                           $('html, body').animate({
                               scrollTop: $(".tryit-req-res").offset().top
                           }, 500);
                       }
                       },
                       type:"POST"
                   });

        }
);
var addNewHeaderRow = function() {
    $('#tryit-headers-form-content').append(createRow('', '','header'));
};

var addNewPayloadRow = function() {
    $('#tryit-body-form-content').append(createRow('', '','body'));
};
var removeRow = function(btn,where) {
    $(btn).parent().remove();
    if(where == "header"){
        createTextFromForm_header();
    }else{
        createTextFromForm_body();
    }
};
var createRow = function(key, value,where) {
    return $('<div class="row-by-row-rows">' +
             '<input type="text" onchange="updateHeadersBodies(\''+where+'\')" class="input-large key" value="' + key + '" placeholder="Name">' +
             '<input type="text" onchange="updateHeadersBodies(\''+where+'\')" class="input-xxlarge value" value="' + value + '" placeholder="Value">' +
             '<input type="button" class="btn" value="x" onclick="removeRow(this,\''+where+'\')">' +
             '</div>');
};
var createFormFromString_header = function(to, str) {
    var headers = str.split('\n');
    $(to).html('');
    for (var i = 0; i < headers.length; i++) {
        if (headers[i] != "") {
            var headerKey = headers[i].split(':')[0];
            var headerValue = headers[i].split(':')[1];
            $(to).append(createRow(headerKey, headerValue,'header'));
        }
    }
};
var createFormFromString_body = function(to, str) {
    var body_parts = str.split('&');
    $(to).html('');
    for (var i = 0; i < body_parts.length; i++) {
        if (body_parts[i] != "") {
            var headerKey = body_parts[i].split('=')[0];
            var headerValue = body_parts[i].split('=')[1];
            $(to).append(createRow(headerKey, headerValue,'body'));
        }
    }
};
var createTextFromForm_header = function() {
//get form elements and put them in the text area
    $('#req_headers').val('');
    var length = $('#tryit-headers-form-content .row-by-row-rows').length;
    $('#tryit-headers-form-content .row-by-row-rows').each(function(index) {
        var key = $('input.key', this).val();
        var value = $('input.value', this).val();
        if (key == "" || value == "") {
            return;
        }
        var currentVal = $('#req_headers').val();
        var newVal = currentVal + key + ":" + value;
        if(index != length -1){
            newVal+="\n";
        }
        $('#req_headers').val(newVal);
    });
    $.cookie('headers', $('#req_headers').val());
};

var createTextFromForm_body = function() {
//get form elements and put them in the text area
    $('#req_body').val('');
    var length =  $('#tryit-body-form-content .row-by-row-rows').length;
    $('#tryit-body-form-content .row-by-row-rows').each(function(index) {
        var key = $('input.key', this).val();
        var value = $('input.value', this).val();
        if (key == "" || value =="") {
            return;
        }
        var currentVal = $('#req_body').val();
        var newVal = currentVal + key + "=" + value;
        if(index != length -1){
            newVal+="&";
        }
        $('#req_body').val(newVal);

    });
    $.cookie('body', $('#req_body').val());
};

var updateHeadersBodies = function(where) {
    if(where == "header"){
        createTextFromForm_header();
    }else{
        createTextFromForm_body();
    }
};
$(document).ready(function() {
    var width = $(document).width();
    var height = $(document).height();

    $('#tryit-content').css('width', width + "px");
    $('#tryit-content').css('height', height + "px").show();
    $('div.header-menu a.brand img').css('height','35px');
    $('.tryit-menu-item').addClass('active');
    $(window).resize(function() {
        var width = $(document).width();
        var height = $(document).height();


        $('#tryit-content').css('width', width + "px");
        $('#tryit-content').css('height', height + "px").show();
    });

    $('#sendBtn').removeAttr('disabled').val('Send');
    $('a[data-toggle="tab"]').on('shown', function (e) {
        var tabId = e.target.href.split('#')[1];
        if (tabId == 'tryit-headers-form') {
            //get text area info and put them in the forms
            var header_str = $('#req_headers').val();
            createFormFromString_header('#tryit-headers-form-content', header_str);
            if(header_str==""){
                $('#tryit-headers-form .row-by-row').append(createRow('', '','header'));
            }
            $.cookie('headers', header_str);
        }

        if (tabId == 'tryit-headers-row') {
            createTextFromForm_header();
        }

        if (tabId == 'tryit-body-form') {
            //get text area info and put them in the forms
            var body_str = $('#req_body').val();
            createFormFromString_body('#tryit-body-form-content', body_str);
            if(body_str==""){
                $('#tryit-body-form-content').append(createRow('', '','body'));
            }
            $.cookie('headers', header_str);

        }

        if (tabId == 'tryit-body-row') {
            createTextFromForm_body();
        }

    });
    var pre_verb = $.cookie('verb');
    var pre_url = $.cookie('url');
    var pre_headers = $.cookie('headers');
    var pre_body = $.cookie('body');

    /* Reading cookies and reloading the previous content */

    var apiName = $('#apiName').val();
    if($.cookie('apiName') == null || $.cookie('apiName') != apiName){

    }else{
        if(pre_url != null){
            var preApiName = pre_url.split("**")[0];
            var urlPart = pre_url.split("**")[1];
            if(preApiName == apiName){
                $('#req_url').val(urlPart);
            }
        }
    }
    $.cookie('apiName',apiName);
    //Setting headers
    if (pre_headers != null) {
        $('#req_headers').html(pre_headers);
    } else {
        $('#tryit-headers-form .row-by-row').html('');
        $('#tryit-headers-form .row-by-row').append(createRow('', '','header'));
    }

    //Setting body
    if (pre_body != null) {
        $('#req_body').html(pre_body);
    } else {
        $('#tryit-body-form .row-by-row').html('');
        $('#tryit-body-form .row-by-row').append(createRow('', '','body'));
    }

    // Setting the verb and show hide body section according to it..
    if (pre_verb != null) {
        if (pre_verb == "GET") {
            $('#bodySection').hide();
            $('#content-type-section').hide();
        } else {
            $('#bodySection').show();
            $('#content-type-section').show();
        }
        $('#req_verb').val(pre_verb);
    }else{
        $('#req_verb').val("GET");
        $('#bodySection').hide();
        $('#content-type-section').hide();
    }

    $('#req_verb').change(function() {
        var selectedVerb = $(this).val();
        if (selectedVerb == "GET") {
            $('#bodySection').hide();
            $('#content-type-section').hide();
        } else {
            $('#bodySection').show();
            $('#content-type-section').show();
        }
        $.cookie('verb', selectedVerb);
    });

    $('#req_headers').change(function() {
        $.cookie('headers', $(this).val());
    });
    $('#req_body').change(function() {
        $.cookie('body', $(this).val());
    });

    $('#req_url').change(function() {
        var url_plain = $(this).attr('data-value');
        var url = $(this).val();
        var apiName = $('#apiName').val();
        var query = "";
        if(url.search(url_plain)!=-1){
            query = url.split(url_plain)[1];
        }
        $(this).attr('data-value',url);
        $.cookie('url', apiName+ "**" + url);
    });
});

var hideTryit = function(){
    $('#tryit-content').hide('fast');
};
