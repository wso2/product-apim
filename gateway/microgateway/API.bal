package microgateway;
import ballerina.net.http;
import ballerina.lang.messages;
import ballerina.lang.errors;
import ballerina.lang.system;

@http:config {basePath:"/api"}
service<http> API_3392d18b_eb44_4c43_9a3a_2a7c9b6b9b86 {
							http:ClientConnector productionEndpoint = create http:ClientConnector("http://www.mocky.io/v2/59a96c49100000300d3e0afa");
												http:ClientConnector sandBoxEndpoint = create http:ClientConnector("http://www.mocky.io/v2/59a96c49100000300d3e0afa");
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
