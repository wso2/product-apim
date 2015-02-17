
//This is the default place holder
var api_doc = 
{
    "apiVersion": "",
    "swaggerVersion": "1.2",
    "apis": [


    ],
    "info": {
        "title": "",
        "description": "",
        "termsOfServiceUrl": "",
        "contact": "",
        "license": "",
        "licenseUrl": ""
    },
    "authorizations":{
        "oauth2":{
            "type": "oauth2",
            "scopes":[]
        }
    }
};



Handlebars.registerHelper('setIndex', function(value){
    this.index = Number(value);
});

Handlebars.registerHelper('console_log', function(value){
    console.log(value);
});

var content_types = [
       { value : "application/json", text :  "application/json"},
       { value : "application/xml", text :  "application/xml"},
       { value : "text/plain", text :  "text/plain"},
       { value : "text/html", text :  "text/html"}
];

//Create a designer class
function APIDesigner(){
    //implement singleton pattern
    this.baseURLValue = "";

    if ( arguments.callee._singletonInstance )
        return arguments.callee._singletonInstance;
    arguments.callee._singletonInstance = this;

    this.api_doc = {};
    this.resources = [] ;

    this.container = $( "#api_designer" );

    //initialise the partials
    source   = $("#designer-resources-template").html();
    Handlebars.partials['designer-resources-template'] = Handlebars.compile(source);
    source   = $("#designer-resource-template").html();
    Handlebars.partials['designer-resource-template'] = Handlebars.compile(source);
    if($('#scopes-template').length){
        source   = $("#scopes-template").html();
        Handlebars.partials['scopes-template'] = Handlebars.compile(source);        
    }

    this.init_controllers();

    $( "#api_designer" ).delegate( ".resource_expand", "click", this, function( event ) {
        if(this.resource_created == undefined){
            event.data.render_resource($(this).parent().next().find('.resource_body'));
            this.resource_created = true;
            $(this).parent().next().find('.resource_body').show();
        }
        else{
            $(this).parent().next().find('.resource_body').toggle();
        }
    });

    $( "#api_designer" ).delegate( "#add_resource", "click", this, function( event ) {
        var designer = APIDesigner();
        if($("#resource_url_pattern").val() == "" || $('#inputResource').val() == ""){
            jagg.message({content:"URL pattern & Resource cannot be empty.",type:"error"});
            return;
        }
        var path = $("#resource_url_pattern").val();
        if(path.charAt(0) != "/")
            path = "/"+path;
        
    	var resource_exist = false;
        $(".http_verb_select").each(function(){    //added this validation to fix https://wso2.org/jira/browse/APIMANAGER-2671
            if($(this).is(':checked')){
                if(designer.check_if_resource_exist( path , $(this).val() ) ){
                	resource_exist = true;
                    var err_message = "Resource already exist for URL Pattern "+path+" and Verb "+$(this).val();
                    jagg.message({content:err_message,type:"error"});
                    return;
                }
            }
        });
        if(resource_exist){
        	return;
        }
        
        var resource = {
            path: path
        };
        //create parameters
        var re = /\{[a-zA-Z0-9_-]*\}/g;
        var parameters = [];

        /*parameters.push({
            "name": "Authorization",
            "description": "Access Token",
            "paramType": "header",
            "required": true,
            "allowMultiple": false,
            "dataType": "String"
        });*/ // Authorization will be set globaly in swagger console.
        parameters.push({
            name : "body",
            "description": "Request Body",
            "allowMultiple": false,
            "required": true,
            "paramType": "body",
            "type":"string"
        });

        while ((m = re.exec($("#resource_url_pattern").val())) != null) {
            if (m.index === re.lastIndex) {
                re.lastIndex++;
            }
            parameters.push({
                name : m[0].replace("{","").replace("}",""),
                "paramType": "path",
                "allowMultiple": false,
                "required": true,
				"type":"string"
            })            
        }        

        resource.operations = [];
        var vc=0;
        var ic=0;
        $(".http_verb_select").each(function(){
            if($(this).is(':checked')){
                if(!designer.check_if_resource_exist( path , $(this).val() ) ){
                resource.operations.push({ 
                    method : $(this).val(),
                    parameters : parameters,
                    nickname : $(this).val().toLowerCase() + '_' +$("#resource_url_pattern").val()
                });
                ic++
                }
                vc++;                
            }
        });
        if(vc==0){
            jagg.message({content:"You should select at least one HTTP verb." ,type:"error"});            
            return;
        }
        event.data.add_resource(resource,$('#inputResource').val());
        //RESOURCES.unshift(resource);
        $("#resource_url_pattern").val("");
        $(".http_verb_select").attr("checked",false);
    });

 
}

APIDesigner.prototype.check_if_resource_exist = function(path, method){    
    var apis = this.query("$.apis[*].file.apis[*]");
    for(var i=0; i< apis.length;i++){
        if(apis[i].path == path){
            for(var j=0; j < apis[i].operations.length; j++){
                if(apis[i].operations[j].method == method){
                    return true;
                }
            }
        }
    }
    return false;
}


APIDesigner.prototype.set_default_management_values = function(){
    var operations = this.query("$.apis[*].file.apis[*].operations[*]");
    for(var i=0;i < operations.length;i++){
        if(!operations[i].auth_type){
            operations[i].auth_type = DEFAULT_AUTH;
        }
        if(operations[i].method == "OPTIONS"){
            operations[i].auth_type = OPTION_DEFAULT_AUTH;
        }
        if(!operations[i].throttling_tier){
            operations[i].throttling_tier = DEFAULT_TIER;
        }
    }
}

APIDesigner.prototype.add_default_resource = function(){
    $("#resource_url_pattern").val("*");
    $(".http_verb_select").attr("checked","checked");    
    $("#inputResource").val("Default");
    $("#add_resource").trigger('click');
}

APIDesigner.prototype.get_scopes = function(){
     if(typeof(this.api_doc.authorizations)!='undefined'){
	var scopes = this.api_doc.authorizations.oauth2.scopes;
	var options = [{ "value": "" , "text": "" }]
	for(var i =0; i < scopes.length ; i++ ){
	    options.push({ "value": scopes[i].key , "text": scopes[i].name });
	}
	return options;
    }
}

APIDesigner.prototype.has_resources = function(){
    if(this.api_doc.apis.length == 0) return false;
}

APIDesigner.prototype.update_elements = function(resource, newValue){
    var API_DESIGNER = APIDesigner();
    var obj = API_DESIGNER.query($(this).attr('data-path'));
    var obj = obj[0]
    var i = $(this).attr('data-attr');
    obj[i] = newValue;
};

APIDesigner.prototype.clean_resources = function(){
    for(var i =0 ; i < this.api_doc.apis.length ; i++){
        for(var j=0; j < this.api_doc.apis[i].file.apis.length; j++){
            if(this.api_doc.apis[i].file.apis[j].operations.length == 0){
                this.api_doc.apis[i].file.apis.splice(j,1);
            }
        }
        if(this.api_doc.apis[i].file.apis.length == 0){
            this.api_doc.apis.splice(i,1);
        }        
    }
}

APIDesigner.prototype.init_controllers = function(){
    var API_DESIGNER = this;

    $("#version").change(function(e){
        APIDesigner().api_doc.apiVersion = $(this).val();
        APIDesigner().baseURLValue = "http://localhost:8280/"+$("#context").val().replace("/","")+"/"+$(this).val()});
    $("#context").change(function(e){ APIDesigner().baseURLValue = "http://localhost:8280/"+$(this).val().replace("/","")+"/"+$("#version").val()});
    $("#name").change(function(e){ APIDesigner().api_doc.info.title = $(this).val() });
    $("#description").change(function(e){ APIDesigner().api_doc.info.description = $(this).val() });

    this.container.delegate( ".delete_resource", "click", function( event ) {        
        var operations = API_DESIGNER.query($(this).attr('data-path'));
        var operations = operations[0]
        var i = $(this).attr('data-index');
        var pn = $(this).attr('data-path-name');
        console.log(operations[i]);
        jagg.message({content:'Do you want to remove "'+operations[i].method+' : '+pn+'" resource from list.',type:'confirm',title:"Remove Resource",
        okCallback:function(){
            API_DESIGNER = APIDesigner();
            operations.splice(i, 1);
            API_DESIGNER.clean_resources();
            API_DESIGNER.render_resources(); 
        }});
        //delete resource if no operations       
    });

    this.container.delegate(".movedown_resource","click", function(){
        var operations = API_DESIGNER.query($(this).attr('data-path'));
        var operations = operations[0]
        var i = parseInt($(this).attr('data-index'));
        if(i != (operations.length - 1)){
            var tmp = operations[i];
            operations[i] = operations[i+1];
            operations[i+1] = tmp;
        }
        API_DESIGNER.render_resources();        
    });     

    this.container.delegate(".moveup_resource","click", function(){
        var operations = API_DESIGNER.query($(this).attr('data-path'));
        var operations = operations[0];
        var i = parseInt($(this).attr('data-index'));
        if(i != 0){
            var tmp = operations[i];
            operations[i] = operations[i-1];
            operations[i-1] = tmp;
        }
        API_DESIGNER.render_resources();        
    });     

    this.container.delegate(".add_parameter", "click", function(event){
        var parameter = $(this).parent().find('.parameter_name').val();
        if(parameter == "") return false;
        var resource_body = $(this).parent().parent();        
        var resource = API_DESIGNER.query(resource_body.attr('data-path'));
        var resource = resource[0]
        if(resource.parameters ==undefined){
            resource.parameters = [];
        }
        resource.parameters.push({ name : parameter , paramType : "query" });
        //@todo need to checge parent.parent to stop code brak when template change.
        API_DESIGNER.render_resource(resource_body);
    });

    this.container.delegate(".remove_parameter", "click", function (event) {
        var parameter = $(this).parent().attr("id");
        if (parameter == "") return false;
        var resource_body = $(this).parents("td.resource_body.hide");
        resource_body.find('tr#' + parameter).remove();
        console.log(resource_body);
        if (resource_body != null) {
            var resource = API_DESIGNER.query(resource_body.attr('data-path'));
            var resource = resource[0]
            if (resource.parameters == undefined) {
                resource.parameters = [];
            }
            $.each(resource.parameters, function (i) {
                if (resource.parameters[i].name === parameter) {
                    resource.parameters.splice(i, 1);
                    return false;
                }
            });
            API_DESIGNER.render_resource(resource_body);
        }
    });

    this.container.delegate(".delete_scope","click", function(){
        var i = $(this).attr("data-index");
        API_DESIGNER.api_doc.authorizations.oauth2.scopes.splice(i, 1);
        API_DESIGNER.render_scopes();
    });

    this.container.delegate("#define_scopes" ,'click', function(){
        $("#scopeName").val('');
        $("#scopeDescription").val('');
        $("#scopeKey").val('');
        $("#scopeRoles").val('');
        $("#define_scope_modal").modal('show');
    });

	    $("#scope_submit")
			.click(
					function() {
						var scope = {
							name : $("#scopeName").val(),
							description : $("#scopeDescription").val(),
							key : $("#scopeKey").val(),
							roles : $("#scopeRoles").val()
						};
						if (API_DESIGNER.api_doc.authorizations.oauth2.scopes == undefined) {
							SCOPES = [];
						}
						for (var i = 0; i < API_DESIGNER.api_doc.authorizations.oauth2.scopes.length; i++) {
							if (API_DESIGNER.api_doc.authorizations.oauth2.scopes[i].key === $(
									"#scopeKey").val() || API_DESIGNER.api_doc.authorizations.oauth2.scopes[i].key === $(
									"#scopeName").val()) {
								jagg
										.message({
											content : "You should not define same scope.",
											type : "error"
										});
								return;
							}
						}
						
						API_DESIGNER.api_doc.authorizations.oauth2.scopes
								.push(scope);
						$("#define_scope_modal").modal('hide');
						API_DESIGNER.render_scopes();
						API_DESIGNER.render_resources();
					}); 

    $("#swaggerEditor").click(API_DESIGNER.edit_swagger);

    $("#update_swagger").click(API_DESIGNER.update_swagger);
}
 
APIDesigner.prototype.load_api_document = function(api_document){
    this.api_doc = api_document
    this.render_resources();
    this.render_scopes();
    $("#version").val(api_document.apiVersion);
    $("#name").val(api_document.info.title);
    if(api_document.info.description){
    	$("#description").val(api_document.info.description);
    }
};


APIDesigner.prototype.render_scopes = function(){
    if($('#scopes-template').length){    
        context = {
            "api_doc" : this.api_doc
        }
        var output = Handlebars.partials['scopes-template'](context);
        $('#scopes_view').html(output);
    }    
};


APIDesigner.prototype.render_resources = function(){
    context = {
        "api_doc" : jQuery.extend(true, {}, this.api_doc),
        "verbs" :VERBS,
        "has_resources" : this.has_resources()
    }
    var output = Handlebars.partials['designer-resources-template'](context);
    $('#resource_details').html(output);
    $('#resource_details').find('.scope_select').editable({
        emptytext: '+ Scope',
        source: this.get_scopes(),
        success : this.update_elements
    });

    if(typeof(TIERS) !== 'undefined'){
        $('#resource_details').find('.throttling_select').editable({
            emptytext: '+ Throttling',        
            source: TIERS,
            success : this.update_elements
        });
    }   

    if(typeof(AUTH_TYPES) !== 'undefined'){
        $('#resource_details').find('.auth_type_select').editable({
            emptytext: '+ Auth Type',        
            source: AUTH_TYPES,
            success : this.update_elements
        });
    }

    $('#resource_details').find('.change_summary').editable({
        emptytext: '+ Summary',        
        success : this.update_elements,
        inputclass : 'resource_summary'
    });  
};

APIDesigner.prototype.render_resource = function(container){
    var operation = this.query(container.attr('data-path'));    
    var context = jQuery.extend(true, {}, operation[0]);
    context.resource_path = container.attr('data-path');
    var output = Handlebars.partials['designer-resource-template'](context);
    container.html(output);
    container.show();

    if(container.find('.editor').length){
        var textarea = container.find('.editor').ace({ theme: 'textmate', lang: 'javascript' ,fontSize: "10pt"});
        var decorator = container.find('.editor').data('ace');
        var aceInstance = decorator.editor.ace;
        aceInstance.getSession().on('change', function(e) {
            operation[0].mediation_script = aceInstance.getValue();
        });
    }

    container.find('.notes').editable({
        type: 'textarea',
        emptytext: '+ Add Implementation Notes',
        success : this.update_elements
    });
    container.find('.content_type').editable({
        value : "application/json",
        source: content_types,
        success : this.update_elements
    });
    container.find('.param_desc').editable({
        emptytext: '+ Empty',
        success : this.update_elements
    });
    container.find('.param_paramType').editable({
        emptytext: '+ Set Param Type',
        source: [ { value:"query", text:"query" },{ value:"body", text:"body"}, { value:"header", text:"header" }, { value:"form", value:"form"} ],
        success : this.update_elements
    });
    container.find('.param_type').editable({
        emptytext: '+ Empty',
        success : this.update_elements
    });
    container.find('.param_required').editable({
        emptytext: '+ Empty',
        source: [ { value:"True", text:"True" },{ value:"False", text:"False"} ],
        success : this.update_elements
    });    
};

APIDesigner.prototype.query = function(path){
    return jsonPath(this.api_doc,path);
}

APIDesigner.prototype.add_resource = function(resource, path){    
    var path = path.toLowerCase();
    if(path.charAt(0) != "/")
        path = "/" + path;
    var i = 0;
    var api = undefined;
    for(i=0; i < this.api_doc.apis.length ; i++ ){
        if(this.api_doc.apis[i].path == path){
            api = this.api_doc.apis[i];
            break;
        }
    }
    if(api == undefined){
        this.api_doc.apis.push({
            path : path,
            description : ""
        });
    }
    if(this.api_doc.apis[i].file == undefined){
        this.api_doc.apis[i].file = { 
            "apiVersion": this.api_doc.apiVersion,
            "swaggerVersion": "1.2",
            "basePath":this.baseURLValue,
            "resourcePath": path ,
            apis : [] 
        };
    }    
    this.api_doc.apis[i].file.apis.push(resource);    
    this.render_resources();
};

APIDesigner.prototype.edit_swagger = function(){
    var designer =  APIDesigner();
    designer.swagger_editor = ace.edit("swagger_editor");
    //var textarea = $('textarea[name="description"]').hide();    
    designer.swagger_editor.setFontSize(16);
    designer.swagger_editor.setTheme("ace/theme/textmate");
    designer.swagger_editor.getSession().setMode("ace/mode/yaml");
    designer.swagger_editor.getSession().setValue(jsyaml.safeDump(designer.api_doc));
    
};

APIDesigner.prototype.update_swagger = function(){
    var designer =  APIDesigner();
    var json = jsyaml.safeLoad(designer.swagger_editor.getSession().getValue());
    designer.load_api_document(json);
    $('#swaggerEditer').modal('toggle');    
};



$(document).ready(function(){
    $.fn.editable.defaults.mode = 'inline';
    var designer = new APIDesigner();
    designer.load_api_document(api_doc);

    $("#clearThumb").on("click", function () {
        $('#apiThumb-container').html('<input type="file" class="input-xlarge" name="apiThumb" />');
    });

    $('#import_swagger').click(function(){
        var data = {
            "swagger_url" : $("#swagger_import_url").val() // "http://petstore.swagger.wordnik.com/api/api-docs"
        }
        $.get( jagg.site.context + "/site/blocks/item-design/ajax/import.jag", data , function( data ) {
            var designer = APIDesigner();
            designer.load_api_document(data);
            $("#swaggerUpload").modal('hide');
        });
    });

    $("#resource_url_pattern").live('change',function(){
        var re = new RegExp("^/?([a-zA-Z0-9]|-|_)+");
        var arr = re.exec($(this).val());
        if(arr && arr.length)
            $('#inputResource').val(arr[0]);
    });


    var v = $("#design_form").validate({
        contentType : "application/x-www-form-urlencoded;charset=utf-8",
        dataType: "json",
	    onkeyup: false,
        submitHandler: function(form) {            
        var designer = APIDesigner();
        
        if(designer.has_resources() == false){
            jagg.message({
                content:"At least one resource should be specified. Do you want to add a wildcard resource (/*)." ,
                type:"confirm",
                title:"Resource not specified",
                anotherDialog:true,
                okCallback:function(){
                    var designer = APIDesigner();
                    designer.add_default_resource();
                    $("#design_form").submit();
                }
            });            
            return false;
        }

        $('#swagger').val(JSON.stringify(designer.api_doc));
        $('#saveMessage').show();
        $('#saveButtons').hide();
        $(form).ajaxSubmit({
            success:function(responseText, statusText, xhr, $form){
                $('#saveMessage').hide();
                $('#saveButtons').show();
                if (!responseText.error) {
                    var designer = APIDesigner();
                    designer.saved_api = {};
                    designer.saved_api.name = responseText.data.apiName;
                    designer.saved_api.version = responseText.data.version;
                    designer.saved_api.provider = responseText.data.provider;
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
                }
            }, dataType: 'json'
        });
        }
    });
});

function getContextValue() {
    var context = $('#context').val();
    var version = $('#apiVersion').val();

    if (context == "" && version != "") {
        $('#contextForUrl').html("/{context}/" + version);
        $('#contextForUrlDefault').html("/{context}/" + version);
    }
    if (context != "" && version == "") {
        if (context.charAt(0) != "/") {
            context = "/" + context;
        }
        $('#contextForUrl').html(context + "/{version}");
        $('#contextForUrlDefault').html(context + "/{version}");
    }
    if (context != "" && version != "") {
        if (context.charAt(0) != "/") {
            context = "/" + context;
        }
        $('.contextForUrl').html(context + "/" + version);
    }
}
