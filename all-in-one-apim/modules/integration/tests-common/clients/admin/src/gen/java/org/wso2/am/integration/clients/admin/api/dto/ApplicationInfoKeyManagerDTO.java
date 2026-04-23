/*
 * WSO2 API Manager - Admin
 * This document specifies a **RESTful API** for WSO2 **API Manager** - **Admin Portal**. Please see [full OpenAPI Specification](https://raw.githubusercontent.com/wso2/carbon-apimgt/master/components/apimgt/org.wso2.carbon.apimgt.rest.api.admin.v1/src/main/resources/admin-api.yaml) of the API which is written using [OAS 3.0](http://swagger.io/) specification.  # Authentication The Admin REST API is protected using OAuth2 and access control is achieved through scopes. Before you start invoking the the API you need to obtain an access token with the required scopes. This guide will walk you through the steps that you will need to follow to obtain an access token. First you need to obtain the consumer key/secret key pair by calling the dynamic client registration (DCR) endpoint. You can add your preferred grant types in the payload. A sample payload is shown below. ```   {   \"callbackUrl\":\"www.example.com\",   \"clientName\":\"rest_api_admin\",   \"owner\":\"admin\",   \"grantType\":\"client_credentials password refresh_token\",   \"saasApp\":true   } ``` Create a file (payload.json) with the above sample payload, and use the cURL shown bellow to invoke the DCR endpoint. Authorization header of this should contain the base64 encoded admin username and password. **Format of the request** ```   curl -X POST -H \"Authorization: Basic Base64(admin_username:admin_password)\" -H \"Content-Type: application/json\"   \\ -d @payload.json https://<host>:<servlet_port>/client-registration/v0.17/register ``` **Sample request** ```   curl -X POST -H \"Authorization: Basic YWRtaW46YWRtaW4=\" -H \"Content-Type: application/json\"   \\ -d @payload.json https://localhost:9443/client-registration/v0.17/register ``` Following is a sample response after invoking the above curl. ``` { \"clientId\": \"fOCi4vNJ59PpHucC2CAYfYuADdMa\", \"clientName\": \"rest_api_admin\", \"callBackURL\": \"www.example.com\", \"clientSecret\": \"a4FwHlq0iCIKVs2MPIIDnepZnYMa\", \"isSaasApplication\": true, \"appOwner\": \"admin\", \"jsonString\": \"{\\\"grant_types\\\":\\\"client_credentials password refresh_token\\\",\\\"redirect_uris\\\":\\\"www.example.com\\\",\\\"client_name\\\":\\\"rest_api_admin\\\"}\", \"jsonAppAttribute\": \"{}\", \"tokenType\": null } ``` Note that in a distributed deployment or IS as KM separated environment to invoke RESTful APIs (product APIs), users must generate tokens through API-M Control Plane's token endpoint. The tokens generated using third party key managers, are to manage end-user authentication when accessing APIs.  Next you must use the above client id and secret to obtain the access token. We will be using the password grant type for this, you can use any grant type you desire. You also need to add the proper **scope** when getting the access token. All possible scopes for Admin REST API can be viewed in **OAuth2 Security** section of this document and scope for each resource is given in **authorizations** section of resource documentation. Following is the format of the request if you are using the password grant type. ``` curl -k -d \"grant_type=password&username=<admin_username>&password=<admin_passowrd>&scope=<scopes seperated by space>\" \\ -H \"Authorization: Basic base64(cliet_id:client_secret)\" \\ https://<host>:<server_port>/oauth2/token ``` **Sample request** ``` curl https://localhost:9443/oauth2/token -k \\ -H \"Authorization: Basic Zk9DaTR2Tko1OVBwSHVjQzJDQVlmWXVBRGRNYTphNEZ3SGxxMGlDSUtWczJNUElJRG5lcFpuWU1h\" \\ -d \"grant_type=password&username=admin&password=admin&scope=apim:admin apim:tier_view\" ``` Shown below is a sample response to the above request. ``` { \"access_token\": \"e79bda48-3406-3178-acce-f6e4dbdcbb12\", \"refresh_token\": \"a757795d-e69f-38b8-bd85-9aded677a97c\", \"scope\": \"apim:admin apim:tier_view\", \"token_type\": \"Bearer\", \"expires_in\": 3600 } ``` Now you have a valid access token, which you can use to invoke an API. Navigate through the API descriptions to find the required API, obtain an access token as described above and invoke the API with the authentication header. If you use a different authentication mechanism, this process may change.  # Try out in Postman If you want to try-out the embedded postman collection with \"Run in Postman\" option, please follow the guidelines listed below. * All of the OAuth2 secured endpoints have been configured with an Authorization Bearer header with a parameterized access token. Before invoking any REST API resource make sure you run the `Register DCR Application` and `Generate Access Token` requests to fetch an access token with all required scopes. * Make sure you have an API Manager instance up and running. * Update the `basepath` parameter to match the hostname and port of the APIM instance.  [![Run in Postman](https://run.pstmn.io/button.svg)](https://app.getpostman.com/run-collection/32294946-71bea2bc-f808-4208-a4f6-861ede6f0434) 
 *
 * The version of the OpenAPI document: v4
 * Contact: architecture@wso2.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.wso2.am.integration.clients.admin.api.dto;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import com.fasterxml.jackson.annotation.JsonCreator;
/**
* ApplicationInfoKeyManagerDTO
*/

public class ApplicationInfoKeyManagerDTO {
        public static final String SERIALIZED_NAME_NAME = "name";
        @SerializedName(SERIALIZED_NAME_NAME)
            private String name;

        public static final String SERIALIZED_NAME_UUID = "uuid";
        @SerializedName(SERIALIZED_NAME_UUID)
            private String uuid;

        public static final String SERIALIZED_NAME_ORGANIZATION_ID = "organizationId";
        @SerializedName(SERIALIZED_NAME_ORGANIZATION_ID)
            private String organizationId;

        public static final String SERIALIZED_NAME_OWNER = "owner";
        @SerializedName(SERIALIZED_NAME_OWNER)
            private String owner;

        public static final String SERIALIZED_NAME_ORGANIZATION = "organization";
        @SerializedName(SERIALIZED_NAME_ORGANIZATION)
            private String organization;


        public ApplicationInfoKeyManagerDTO name(String name) {
        
        this.name = name;
        return this;
        }

    /**
        * The name of the application.
    * @return name
    **/
      @ApiModelProperty(required = true, value = "The name of the application.")
    
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


        public ApplicationInfoKeyManagerDTO uuid(String uuid) {
        
        this.uuid = uuid;
        return this;
        }

    /**
        * The UUID of the application.
    * @return uuid
    **/
      @ApiModelProperty(required = true, value = "The UUID of the application.")
    
    public String getUuid() {
        return uuid;
    }


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


        public ApplicationInfoKeyManagerDTO organizationId(String organizationId) {
        
        this.organizationId = organizationId;
        return this;
        }

    /**
        * The ID of the organization to which the application belongs.
    * @return organizationId
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The ID of the organization to which the application belongs.")
    
    public String getOrganizationId() {
        return organizationId;
    }


    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }


        public ApplicationInfoKeyManagerDTO owner(String owner) {
        
        this.owner = owner;
        return this;
        }

    /**
        * The owner of the application.
    * @return owner
    **/
      @ApiModelProperty(required = true, value = "The owner of the application.")
    
    public String getOwner() {
        return owner;
    }


    public void setOwner(String owner) {
        this.owner = owner;
    }


        public ApplicationInfoKeyManagerDTO organization(String organization) {
        
        this.organization = organization;
        return this;
        }

    /**
        * The organization of the application.
    * @return organization
    **/
      @ApiModelProperty(required = true, value = "The organization of the application.")
    
    public String getOrganization() {
        return organization;
    }


    public void setOrganization(String organization) {
        this.organization = organization;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
        return true;
        }
        if (o == null || getClass() != o.getClass()) {
        return false;
        }
            ApplicationInfoKeyManagerDTO applicationInfoKeyManager = (ApplicationInfoKeyManagerDTO) o;
            return Objects.equals(this.name, applicationInfoKeyManager.name) &&
            Objects.equals(this.uuid, applicationInfoKeyManager.uuid) &&
            Objects.equals(this.organizationId, applicationInfoKeyManager.organizationId) &&
            Objects.equals(this.owner, applicationInfoKeyManager.owner) &&
            Objects.equals(this.organization, applicationInfoKeyManager.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uuid, organizationId, owner, organization);
    }


@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("class ApplicationInfoKeyManagerDTO {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    organizationId: ").append(toIndentedString(organizationId)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    organization: ").append(toIndentedString(organization)).append("\n");
sb.append("}");
return sb.toString();
}

/**
* Convert the given object to string with each line indented by 4 spaces
* (except the first line).
*/
private String toIndentedString(Object o) {
if (o == null) {
return "null";
}
return o.toString().replace("\n", "\n    ");
}

}

