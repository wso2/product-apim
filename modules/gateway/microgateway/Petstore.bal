package microgateway;
import ballerina.net.http;
import ballerina.lang.messages;
import ballerina.lang.errors;
import ballerina.lang.system;

@http:config {basePath:"/v2"}
service<http> Swagger_Petstore_30befea0_606f_4dac_a4f5_c1d4c52b6d44 {
    http:ClientConnector productionEndpoint = create http:ClientConnector("http://petstore.swagger.io/v2");
												http:ClientConnector sandBoxEndpoint = create http:ClientConnector("http://petstore.swagger.io/v2");
	string KEY_TYPE = "KEY_TYPE";
    @http:GET{}
    @http:Path{value:"/pet/{petId}"}
    resource getPetById (message m, @http:PathParam{value:"petId"} string petId) {
string urlPath = "/pet/" + petId;
    http:ClientConnector getPetByIdProductionEndpoint = productionEndpoint;
    http:ClientConnector getPetByIdSandBoxEndpoint = sandBoxEndpoint;
    message response;
    string endpointType = messages:getProperty(m,KEY_TYPE);

	try{
		if (endpointType == "PRODUCTION") {
				response = http:ClientConnector.execute(getPetByIdProductionEndpoint, "GET", urlPath, m);
			system:println("Response");
			system:println(messages:getStringPayload(response));
				} else {
				response = http:ClientConnector.execute(getPetByIdSandBoxEndpoint,"GET", urlPath, m);
				}
	} catch (errors:Error e) {
			system:println("Error occurred");
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
    @http:Path{value:"/store/inventory"}
    resource getInventory (message m) {

    http:ClientConnector getInventoryProductionEndpoint = productionEndpoint;
    http:ClientConnector getInventorySandBoxEndpoint = sandBoxEndpoint;
    message response;
    string endpointType = messages:getProperty(m,KEY_TYPE);

	try{
		if (endpointType == "PRODUCTION") {
				response = http:ClientConnector.execute(getInventoryProductionEndpoint, "GET", "/store/inventory", m);
			system:println(messages:getStringPayload(response));
				} else {
				response = http:ClientConnector.execute(getInventorySandBoxEndpoint,"GET", "/store/inventory", m);
				}
	} catch (errors:Error e) {
			system:println("Error occurred");
			system:println(e.msg);
			//fault:mediate(m, e);
			response = {};
		    messages:setStringPayload(response, "Internal error occurred");
			http:setStatusCode (response, 500);
		    reply response;
	}

		reply response;
}

	    @http:POST{}
        	@http:Path{value:"/user"}
    resource createUser (message m) {
        http:ClientConnector createUserProductionEndpoint = productionEndpoint;
										http:ClientConnector createUserSandBoxEndpoint = sandBoxEndpoint;
												        message response;
		string endpointType = messages:getProperty(m,KEY_TYPE);

	try{
		if (endpointType == "PRODUCTION") {
				response = http:ClientConnector.execute(createUserProductionEndpoint, "post", "", m);
				} else {
				response = http:ClientConnector.execute(createUserSandBoxEndpoint,"post", "", m);
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
				    @http:POST{}
        	@http:Path{value:"/pet"}
    resource addPet (message m) {
		http:ClientConnector addPetProductionEndpoint = productionEndpoint;
										http:ClientConnector addPetSandBoxEndpoint = sandBoxEndpoint;
												        message response;
		string endpointType = messages:getProperty(m,KEY_TYPE);

	try{
		if (endpointType == "PRODUCTION") {
				response = http:ClientConnector.execute(addPetProductionEndpoint,"post", "", m);
				} else {
				response = http:ClientConnector.execute(addPetSandBoxEndpoint,"post", "", m);
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

	   
