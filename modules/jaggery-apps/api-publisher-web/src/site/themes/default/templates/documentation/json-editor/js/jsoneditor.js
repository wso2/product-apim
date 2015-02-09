function loadDefaultTinyMCEContent(provider,apiName, version, docName) {
    jagg.post("/site/blocks/documentation/ajax/docs.jag", { action:"getInlineContent", provider:provider,apiName:apiName,version:version,docName:docName },
              function (json) {
                  if (!json.error) {
                      var docName = json.doc.provider.docName;
                      var apiName = json.doc.provider.apiName;
                      var json = json.doc.provider.content;
                      $('#json').val(JSON.stringify(JSON.parse(json), null, 4));
                      $('#editor').jsonEditor(JSON.parse(json), { change: updateJSON, propertyclick: showPath });
                  } else {
                      $('#inlineError').show('fast');
                      $('#inlineSpan').html('<strong>'+ i18n.t('errorMsgs.inlineContent')+'</strong><br />'+result.message);
                  }
              }, "json");



}

//var json = {"apiVersion":"1.0","swaggerVersion":"1.1","basePath":"http://10.200.0.202:8280","resourcePath":"/abc","apis":[{"path":"/abc/1.0","description":"no-info","operations":[{"httpMethod":"GET","summary":"no-info","nickname":"no-info","parameters":[{"name":"Authorization","description":"Access Token","paramType":"header","required":true,"allowMultiple":false,"dataType":"String"},{"name":"Query Parameters","description":"Request Query Parameters","paramType":"body","required":false,"allowMultiple":false,"dataType":"String"}]},{"httpMethod":"POST","summary":"no-info","nickname":"no-info","parameters":[{"name":"Authorization","description":"Access Token","paramType":"header","required":true,"allowMultiple":false,"dataType":"String"},{"name":"Payload","description":"Request Payload","paramType":"body","required":false,"allowMultiple":false,"dataType":"String"}]},{"httpMethod":"PUT","summary":"no-info","nickname":"no-info","parameters":[{"name":"Authorization","description":"Access Token","paramType":"header","required":true,"allowMultiple":false,"dataType":"String"},{"name":"Payload","description":"Request Payload","paramType":"body","required":false,"allowMultiple":false,"dataType":"String"}]},{"httpMethod":"DELETE","summary":"no-info","nickname":"no-info","parameters":[{"name":"Authorization","description":"Access Token","paramType":"header","required":true,"allowMultiple":false,"dataType":"String"},{"name":"Query Parameters","description":"Request Query Parameters","paramType":"body","required":false,"allowMultiple":false,"dataType":"String"}]},{"httpMethod":"OPTIONS","summary":"no-info","nickname":"no-info","parameters":[{"name":"Payload","description":"Request Payload","paramType":"body","required":false,"allowMultiple":false,"dataType":"String"}]}]}]};

function printJSON() {
    $('#json').val(JSON.stringify(json));
	$('#json').val(JSON.stringify(JSON.parse(json), null, 4));

}

function updateJSON(data) {
    json = data;
    printJSON();
}

function showPath(path) {
    //$('#path').text(path);
}

$('#beautify').click(function(evt) {
	evt.preventDefault();
	var jsonText = $('#json').val();
	$('#json').val(JSON.stringify(JSON.parse(jsonText), null, 4));
}); 

$(document).ready(function() {
    $.ajaxSetup({
      contentType: "application/x-www-form-urlencoded; charset=utf-8"
    });

    $('#rest > button').click(function() {
        var url = $('#rest-url').val();
        $.ajax({
            url: url,
            dataType: 'jsonp',
            jsonp: $('#rest-callback').val(),
            success: function(data) {
                json = data;
                $('#editor').jsonEditor(json, { change: updateJSON, propertyclick: showPath });
                printJSON();
            },
            error: function() {

            }
        });
    });

    $('#json').change(function() {
        var val = $('#json').val();

        if (val) {
            try { json = JSON.parse(val); }
            catch (e) {  }
        } else {
            json = {};
        }
        
        $('#editor').jsonEditor(json, { change: updateJSON, propertyclick: showPath });
    });

    $('#expander').click(function() {
        var editor = $('#editor');
        editor.toggleClass('expanded');
        $(this).text(editor.hasClass('expanded') ? 'Collapse' : 'Expand all');
    });
    
    printJSON();
    $('#editor').jsonEditor(json, { change: updateJSON, propertyclick: showPath });
});

function saveContent(provider, apiName, apiVersion, docName, mode) {
	var contentDoc = $('#json').val();
	jagg.post("/site/blocks/documentation/ajax/docs.jag", { action:"addInlineContent",provider:provider,apiName:apiName,version:apiVersion,docName:docName,content:contentDoc},
              function (result) {
                  if (result.error) {
                      if (result.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:result.message,type:"error"});
                      }
                  } else {
                      if (mode == "save") {
                         /* $('#messageModal').html($('#confirmation-data').html());
                          $('#messageModal h3.modal-title').html('Document Content Addition Successful');
                          $('#messageModal div.modal-body').html('\n\n Successfully saved the documentation content and you will be moved away from this tab.');
                          $('#messageModal a.btn-primary').html('OK');
                          $('#messageModal a.btn-other').hide();
                          $('#messageModal a.btn-primary').click(function() {*/
                              window.close();
                          /*});
                          $('#messageModal').modal();*/
                      } else {
                           $('#docAddMessage').show();
                           setTimeout("hideMsg()", 3000);
                      }
                  }
              }, "json");
}


