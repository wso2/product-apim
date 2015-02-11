
var HTTP_VERBS = [ "GET", "POST", "PUT", "DELETE", "OPTIONS"];

var SCOPES = [];

$( document ).ready(function() {

    SCOPES = jQuery.parseJSON($("#scopes_config").val());
    if(SCOPES == undefined)SCOPES=[];
    //RESOURCES.resources = jQuery.parseJSON($("#resource_config").val());
    

    Handlebars.registerHelper('property_exist', function(object,property,options) {
        if(object.hasOwnProperty(property)){
            return options.fn({"verb":property ,"object":object[property]});
        }
        return false;
    });

    Handlebars.registerHelper('selected', function(option, value){
        if (option === value) {
            return ' selected';
        } else {
            return ''
        }
    });    

    Handlebars.registerHelper('debug', function (object) {
        console.log(object);
    });

    $("#add_resource").click(function(){
        if($("#resource_url_pattern").val() == ""){
            alert("URL pattern cannot be null");
            return;
        }
        var resource = { "url_pattern": $("#resource_url_pattern").val() };
        resource.http_verbs = {};
        var vc=0;
        $(".http_verb_select").each(function(){
            if($(this).is(':checked')){
                resource.http_verbs[$(this).val()] = { "auth_type": AUTH_TYPES[AUTH_TYPES.length -1].key , "throttling_tier":TIERS[TIERS.length -1].tierName };
                vc++;
            }
        });
        if(vc==0){
            alert("You should select at least one HTTP verb.")
            return;
        }
        RESOURCES.unshift(resource);
        $("#resource_url_pattern").val("");
        $(".http_verb_select").attr("checked",false);
        $("#resource_view").trigger("draw");       
    });

    $("#resource_view").delegate(".http_verb_deselect","change", function(){
        var i = $(this).attr("data-index");
        var verb = $(this).attr("data-verb");
        delete(RESOURCES[i].http_verbs[verb]);
        $("#resource_view").trigger("draw");
    }); 


    $("#resource_view").delegate(".http_verb_add","change", function(){
        var i = $(this).attr("data-index");
        if(!RESOURCES[i].hasOwnProperty("http_verbs")){
            RESOURCES[i].http_verbs = {};
        }
        RESOURCES[i].http_verbs[$(this).val()] = {  "auth_type": AUTH_TYPES[AUTH_TYPES.length -1].key , "throttling_tier":TIERS[TIERS.length -1].tierName };
        $("#resource_view").trigger("draw");
    }); 

    $("#resource_view").delegate(".resource_url_pattern","change", function(){
        var i = $(this).attr("data-index");
        RESOURCES[i].url_pattern = $(this).val();
        $("#resource_view").trigger("draw");        
    }); 

    $("#resource_view").delegate(".movedown_resource","click", function(){
        var i = parseInt($(this).attr("data-index"),10);
        if(i != (RESOURCES.length - 1)){
            var tmp = RESOURCES[i];
            RESOURCES[i] = RESOURCES[i+1];
            RESOURCES[i+1] = tmp;
        }
        $("#resource_view").trigger("draw");
    });     

    $("#resource_view").delegate(".moveup_resource","click", function(){
        var i = parseInt($(this).attr("data-index"),10);
        if(i != 0){
            var tmp = RESOURCES[i];
            RESOURCES[i] = RESOURCES[i-1];
            RESOURCES[i-1] = tmp;
        }
        $("#resource_view").trigger("draw");        
    }); 

    $("#resource_view").delegate(".delete_resource","click", function(){
        var i = $(this).attr("data-index");
        RESOURCES.splice(i, 1);
        $("#resource_view").trigger("draw");                
    });

    $("#resource_view").delegate(".resource_scope_select","change", function(){
        var i = $(this).attr("data-index");
        var verb = $(this).attr("data-verb");
        RESOURCES[i].http_verbs[verb].scope = $(this).val();
        $("#resource_config").val(JSON.stringify({ "resources" : RESOURCES , "scopes":SCOPES }));                
    });

    $("#resource_view").delegate(".resource_auth_select","change", function(){
        var i = $(this).attr("data-index");
        var verb = $(this).attr("data-verb");
        RESOURCES[i].http_verbs[verb].auth_type = $(this).val();
        $("#resource_config").val(JSON.stringify({ "resources" : RESOURCES , "scopes":SCOPES }));
    });

    $("#resource_view").delegate(".resource_tier_select","change", function(){
        var i = $(this).attr("data-index");
        var verb = $(this).attr("data-verb");
        RESOURCES[i].http_verbs[verb].throttling_tier = $(this).val();
        $("#resource_config").val(JSON.stringify({ "resources" : RESOURCES , "scopes":SCOPES }));        
    });
    

    $("#scopes_view").delegate(".delete_scope","click", function(){
        var i = $(this).attr("data-index");
        SCOPES.splice(i, 1);
        $("#resource_view").trigger("draw");        
    });

    $("#scopes_view").delegate("#define_scopes" ,'click', function(){
        $("#scopeName").val('');
        $("#scopeDescription").val('');
        $("#scopeKey").val('');
        $("#scopeRoles").val('');
        $("#define_scope_modal").modal('show');
    });

    $("#scope_submit").click(function(){
        var scope = { 
            name :$("#scopeName").val(),
            description : $("#scopeDescription").val(),
            key:$("#scopeKey").val(),
            roles:$("#scopeRoles").val()
        };
        if(SCOPES == undefined){
            SCOPES=[];
        }
        SCOPES.push(scope);
        $("#define_scope_modal").modal('hide');
        $("#resource_view").trigger("draw");    
    });

    $("#resource_view").on("draw", function(){
        for(var i=0;i < RESOURCES.length ; i++){
            RESOURCES[i].url_pattern = RESOURCES[i].url_pattern.indexOf('/') == 0 ? RESOURCES[i].url_pattern.substring(1) : RESOURCES[i].url_pattern;
            RESOURCES[i].idx = i;
            RESOURCES[i].missing = [];
            if(RESOURCES[i].hasOwnProperty('http_verbs')){
                for(var y =0; y < HTTP_VERBS.length; y++){
                    if(RESOURCES[i].http_verbs[HTTP_VERBS[y]] == undefined){
                        RESOURCES[i].missing.push(HTTP_VERBS[y]);
                    }
                }
            }
            else{
                RESOURCES[i].missing = HTTP_VERBS;
            }
        }
        if(SCOPES){
            for(var i=0; i < SCOPES.length ; i++){
                SCOPES[i].idx = i;
            }
        }

        //get the version and the context
        var version = ($('#version[name=version]').val()=='')?"{version}":$('#version[name=version]').val();
        var context = ($('#context').val()=='')?"{context}":$('#context').val();
        console.log(version)
        console.log(context);
        context = context.indexOf('/') == 0 ? context.substring(1) : context;

        var template = Handlebars.partials['resources']({ "resources": RESOURCES, "verbs": HTTP_VERBS, "scopes": SCOPES, "tiers": TIERS, "auth_types":AUTH_TYPES, 'version':version , 'context':context });
        var template2 = Handlebars.partials['scopes']({ "resources": RESOURCES, "verbs": HTTP_VERBS, "scopes": SCOPES, "tiers": TIERS , "auth_types":AUTH_TYPES, 'version':version , 'context':context });        
        $("#resource_view").html(template);
        $("#scopes_view").html(template2);
        //set values
        $("#resource_config").val(JSON.stringify({ "resources" : RESOURCES , "scopes":SCOPES }));        
    });

    

    //load the resource partial
    var source   = $("#resource-template").html();
    Handlebars.partials['resources'] = Handlebars.compile(source);
    var source2   = $("#scopes-template").html();
    Handlebars.partials['scopes'] = Handlebars.compile(source2);    
    $("#resource_view").trigger("draw");

});

var validateResourceTable = function(){
    var errors = "";

    $('.resourceRow input.resourceTemplate').each(function(){
        var myVal = $(this).val();
        var foundMyVal = 0;
        $('.resourceRow input.resourceTemplate').each(function(){
            if($(this).val()==myVal){
                foundMyVal++;
            }
        });
        if(foundMyVal > 1){
            errors += "URL Pattern has to be unique. <strong>" + myVal + "</strong> has duplicated entries.<br/>";
        }
        if(myVal == ""){
            errors += "URL Pattern can't be empty.<br />";
        }
    });

    var allRowsHas_at_least_one_check = true;
    $('.resourceRow').each(function(){
        var tr = this;
        var noneChecked = true;
        $('input:checkbox',tr).each(function(){
            if($(this).is(":checked")){
                noneChecked = false;
            }
        });

        if(noneChecked){
            allRowsHas_at_least_one_check = false;
        }
    });


    if(!allRowsHas_at_least_one_check){
        errors += "At least one HTTP Verb has to be checked for a resource.<br />";
    }
    console.info(errors);
    if(errors != ""){
        $('#resourceTableError').show('fast');
        $('#resourceTableError').html(errors);
        $('#addNewAPIButton').attr('disabled','disabled');
    }else{
        $('#resourceTableError').hide('fast');
        $('#addNewAPIButton').removeAttr('disabled');
    }
    return errors;
};

