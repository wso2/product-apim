package microgateway;
import ballerina.net.http;
import ballerina.lang.messages;
import ballerina.lang.errors;
import ballerina.lang.system;

@http:config {basePath:"/petstore"}
service<http> SwaggerPetstore_6e7e75ea_cec3_47ce_baae_4d336e16cff5 {
							http:ClientConnector productionEndpoint = create http:ClientConnector("http://petstore.swagger.io");
												http:ClientConnector sandBoxEndpoint = create http:ClientConnector("http://petstore.swagger.io");
				string KEY_TYPE = "KEY_TYPE";
			    @http:POST{}
        	@http:Path{value:"/"}
    resource post (message m) {
						http:ClientConnector postProductionEndpoint = productionEndpoint;
										http:ClientConnector postSandBoxEndpoint = sandBoxEndpoint;
				        message response;
		string endpointType = messages:getProperty(m,KEY_TYPE);

	try{
		if (endpointType == "PRODUCTION") {
				response = http:ClientConnector.execute(postProductionEndpoint, "post", "", m);
				} else {
				response = http:ClientConnector.execute(postSandBoxEndpoint, "post", "", m);
				}
	} catch (errors:Error e) {
			system:println(e.msg);
			//fault:mediate(m, e);
			response = {};
		    messages:setStringPayload(response, "Internal error occurred");
			http:setStatusCode (response, 500);
		    reply response;
	}

		reply response;
}
	    @http:GET{}
        	@http:Path{value:"/"}
    resource get (message m) {
						http:ClientConnector getProductionEndpoint = productionEndpoint;
										http:ClientConnector getSandBoxEndpoint = sandBoxEndpoint;
				        message response;
		string endpointType = messages:getProperty(m,KEY_TYPE);

	try{
		if (endpointType == "PRODUCTION") {
				response = http:ClientConnector.execute(getProductionEndpoint, "get", "", m);
				} else {
				response = http:ClientConnector.execute(getSandBoxEndpoint, "get", "", m);
				}
	} catch (errors:Error e) {
			system:println(e.msg);
			//fault:mediate(m, e);
			response = {};
		    messages:setStringPayload(response, "Internal error occurred");
			http:setStatusCode (response, 500);
		    reply response;
	}

		reply response;
}
		}
