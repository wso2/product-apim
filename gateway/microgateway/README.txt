WSO2 Offline Micro Gateway with API key

This is WSO2 Offline Micro Gateway with API key, powered by Ballerina. 


Building from the source

If you want to build APIM Gateway from the source code:

1. Install Java 8(http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
2. Install Apache Maven 3.x.x(https://maven.apache.org/download.cgi#)
3. Get a clone or download the source from this repository (https://github.com/wso2/product-apimgt).
4. Run the Maven command ``mvn clean install`` from the ``product-apimgt/gateway`` directory.
5. Extract the WSO2 APIM Gateway distribution created at `product-apimgt/gateway/target/wso2apim-gateway-<version>-SNAPSHOT.zip` to your local directory.

Starting the server

1. Go to `product-apimgt/gateway/target/wso2apim-gateway-<version>-SNAPSHOT` directory
2. Include 'apiKeys.json' in the microgateway folder where the data is in the following format,
{  
   "apis":[  
      {  
         "name":"",
         "context":"",
         "version":"",
         "securityScheme":,
	 "lifeCycleStatus":"",
         "resources":[  
            {  
               "uriTemplate":"",
               "httpVerb":"",
               "authType":"",
               "policy":"",
               "scope":""
            },
            {  
               "uriTemplate":"",
               "httpVerb":"",
               "authType":"",
               "policy":"",
               "scope":""
            }
         ],
         "apps":[  
            {  
               "name":"",
               "sandbox":"",
               "production":""
            }
         ]
      }
   ]
}
3. Include all the API.bal files in the microgateway folder.
4. (optional) If you wish to start the gateway with analytics and throttling, uncomment the analytics and throttling in the gateway deployment.yaml file and include the 'microConf.json' where the data is in the following format.

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


5. Execute the below command.
``bin/microgatewayinit.sh``



