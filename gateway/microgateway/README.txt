WSO2 Offline Micro Gateway with API key

This is WSO2 Offline Micro Gateway with API key, powered by Ballerina 0.89. 

Building from the source

If you want to build APIM Gateway from the source code:

1. Install Java 8(http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
2. Install Apache Maven 3.x.x(https://maven.apache.org/download.cgi#)
3. Get a clone or download the source from this repository (https://github.com/wso2/product-apimgt).
4. Run the Maven command ``mvn clean install`` from the ``product-apimgt/gateway`` directory.
5. Extract the WSO2 APIM Gateway distribution created at `product-apimgt/gateway/target/wso2apim-gateway-<version>-SNAPSHOT.zip` to your local directory.

Starting the server

1. Place bal file and swagger file of each API in wso2apim-gateway-<version>-SNAPSHOT/microgateway folder.

NOTE 1: If any API files are not given, the microgateway will not start, example files are given in `product-apimgt/gateway/microgateway`
NOTE 2: Make sure the package name in each API bal file is changed to "microgateway"
NOTE 3: Include the security component in the swagger structure, providing the api keys. (check for example files give in `product-apimgt/gateway/microgateway`)
"security": [{
		"api_key": [
			"api_key_1",
			"api_key_2",
			"api_key_3",
			"api_key_4"
		]
}]


2. Place "microConf.json" file in the microgateway folder to pass the gateway configuration information,
template is given below,

{
	"keyManagerInfo": {
		"dcrEndpoint": "",
		"tokenEndpoint": "",
		"revokeEndpoint": "",
		"introspectEndpoint": "",
		"credentials": {
			"username": "",
			"password": ""
		}
	},
	"jwTInfo": {
		"enableJWTGeneration":,
		"jwtHeader": ""
	},
	"analyticsInfo": {
		"enabled": 0,
		"type": "",
		"serverURL": "",
		"authServerURL": "",
		"credentials": {
			"username": "",
			"password": ""
		}
	},
	"throttlingInfo": {
		"enabled": ,
		"type": "",
		"serverURL": "",
		"authServerURL": "",
		"credentials": {
			"username": "",
			"password": ""
		}
	}
}

Alternative:

If you wish to start the gateway without analytics and throttling,you you need not to provide the "microConf.json" file, then you need to comment the analytics and throttling handlers in the gateway "deployment.yaml" file.

Note: analytics and throttling are not enabled yet. For this pack "microConf.json" will come by default

3. Execute the below command from the gateway home.
"bin/microgatewayinit.sh"




