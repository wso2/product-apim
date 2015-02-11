$(document).ready(function() {
    $.validator.addMethod('contextExists', function(value, element) {
        if (value.charAt(0) != "/") {
            value = "/" + value;
        }
        var contextExist = false;
        var oldContext=$('#spanContext').text();
        jagg.syncPost("/site/blocks/item-add/ajax/add.jag", { action:"isContextExist", context:value,oldContext:oldContext },
                      function (result) {
                          if (!result.error) {
                              contextExist = result.exist;
                          }
                      });
        return this.optional(element) || contextExist != "true";
    }, 'Duplicate context value.');

    $.validator.addMethod('apiNameExists', function(value, element) {
        var apiNameExist = false;
        jagg.syncPost("/site/blocks/item-add/ajax/add.jag", { action:"isAPINameExist", apiName:value },
                      function (result) {
                          if (!result.error) {
                              apiNameExist = result.exist;
                          }
                      });
        return this.optional(element) || apiNameExist != "true";
    }, 'Duplicate api name.');

    $.validator.addMethod('selected', function(value, element) {
        return value!="";
    },'Select a value for the tier.');

    $.validator.addMethod('validRegistryName', function(value, element) {
        var illegalChars = /([~!@#;%^*+={}\|\\<>\"\'\/,])/;
        return !illegalChars.test(value);
    }, 'Name contains one or more illegal characters  (~ ! @ #  ; % ^ * + = { } | &lt; &gt;, \' / " \\ ) .');

    $.validator.addMethod('noSpace', function(value, element) {
        return !/\s/g.test(value);
    },'Name contains white spaces.');

    $.validator.addMethod('validInput', function(value, element) {
        var illegalChars = /([<>\"\'])/;
        return !illegalChars.test(value);
    }, 'Input contains one or more illegal characters  (& &lt; &gt; \'  " ');

    $.validator.addMethod('validateRoles', function(value, element) {
        var valid = false;
        var oldContext=$('#spanContext').text();
        jagg.syncPost("/site/blocks/item-add/ajax/add.jag", { action:"validateRoles", roles:value },
                      function (result) {
                          if (!result.error) {
                              valid = result.response;
                          }
                      });
        return this.optional(element) || valid == true;
    }, 'Invalid role name[s]');

    $.validator.addMethod('validateEndpoints', function (value, element){
        return APP.is_production_endpoint_specified() || APP.is_sandbox_endpoint_specified();
    }, 'A Production or Sandbox URL must be provided.');
    
    $.validator.addMethod('validateProdWSDLService', function (value, element){
    	if (APP.is_production_endpoint_specified()) {
    		return APP.is_production_wsdl_endpoint_service_specified();
    	} 
    	return true;        
    }, 'Service Name must be provided for WSDL endpoint.');
    
    $.validator.addMethod('validateProdWSDLPort', function (value, element){
    	if (APP.is_production_endpoint_specified()) {
    		return APP.is_production_wsdl_endpoint_port_specified();
    	} 
    	return true;   
    }, 'Service Port must be provided for WSDL endpoint.');
    
    $.validator.addMethod('validateSandboxWSDLService', function (value, element){
    	if (APP.is_sandbox_endpoint_specified()) {
    		return APP.is_sandbox_wsdl_endpoint_service_specified();
    	}
        return true;
    }, 'Service Name must be provided for WSDL endpoint.');
    
    $.validator.addMethod('validateSandboxWSDLPort', function (value, element){
    	if (APP.is_sandbox_endpoint_specified()) {
    		return APP.is_sandbox_wsdl_endpoint_port_specified();
    	}
    	return true;
    }, 'Service Port must be provided for WSDL endpoint.');

    $.validator.addMethod('validateImageFile', function (value, element) {
        if ($(element).val() == "") {
            return true;
        }
        else {
            var validFileExtensions = ["jpg", "jpeg", "bmp", "gif", "png"];
            var ext = $(element).val().split('.').pop().toLowerCase();
            return ($.inArray(ext, validFileExtensions)) > -1;
        }
        return true;
    }, 'File must be in image file format.');

});
