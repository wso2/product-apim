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
import java.util.ArrayList;
import java.util.List;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerInfoDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
/**
* ApplicationInfoDTO
*/

public class ApplicationInfoDTO {
        public static final String SERIALIZED_NAME_APPLICATION_ID = "applicationId";
        @SerializedName(SERIALIZED_NAME_APPLICATION_ID)
            private String applicationId;

        public static final String SERIALIZED_NAME_NAME = "name";
        @SerializedName(SERIALIZED_NAME_NAME)
            private String name;

        public static final String SERIALIZED_NAME_OWNER = "owner";
        @SerializedName(SERIALIZED_NAME_OWNER)
            private String owner;

            /**
* Gets or Sets tokenType
*/
    @JsonAdapter(TokenTypeEnum.Adapter.class)
public enum TokenTypeEnum {
        OAUTH("OAUTH"),
        
        JWT("JWT");

private String value;

TokenTypeEnum(String value) {
this.value = value;
}

public String getValue() {
return value;
}

@Override
public String toString() {
return String.valueOf(value);
}

public static TokenTypeEnum fromValue(String value) {
    for (TokenTypeEnum b : TokenTypeEnum.values()) {
    if (b.name().equals(value)) {
        return b;
    }
}
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
}

    public static class Adapter extends TypeAdapter<TokenTypeEnum> {
    @Override
    public void write(final JsonWriter jsonWriter, final TokenTypeEnum enumeration) throws IOException {
    jsonWriter.value(enumeration.getValue());
    }

    @Override
    public TokenTypeEnum read(final JsonReader jsonReader) throws IOException {
    String value =  jsonReader.nextString();
    return TokenTypeEnum.fromValue(value);
    }
    }
}

        public static final String SERIALIZED_NAME_TOKEN_TYPE = "tokenType";
        @SerializedName(SERIALIZED_NAME_TOKEN_TYPE)
            private TokenTypeEnum tokenType;

        public static final String SERIALIZED_NAME_CREATED_TIME = "createdTime";
        @SerializedName(SERIALIZED_NAME_CREATED_TIME)
            private String createdTime;

        public static final String SERIALIZED_NAME_KEY_MANAGERS = "keyManagers";
        @SerializedName(SERIALIZED_NAME_KEY_MANAGERS)
            private List<KeyManagerInfoDTO> keyManagers = null;

        public static final String SERIALIZED_NAME_STATUS = "status";
        @SerializedName(SERIALIZED_NAME_STATUS)
            private String status;

        public static final String SERIALIZED_NAME_GROUP_ID = "groupId";
        @SerializedName(SERIALIZED_NAME_GROUP_ID)
            private String groupId;


        public ApplicationInfoDTO applicationId(String applicationId) {
        
        this.applicationId = applicationId;
        return this;
        }

    /**
        * Get applicationId
    * @return applicationId
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "01234567-0123-0123-0123-012345678901", value = "")
    
    public String getApplicationId() {
        return applicationId;
    }


    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }


        public ApplicationInfoDTO name(String name) {
        
        this.name = name;
        return this;
        }

    /**
        * Get name
    * @return name
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "CalculatorApp", value = "")
    
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


        public ApplicationInfoDTO owner(String owner) {
        
        this.owner = owner;
        return this;
        }

    /**
        * Get owner
    * @return owner
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "admin", value = "")
    
    public String getOwner() {
        return owner;
    }


    public void setOwner(String owner) {
        this.owner = owner;
    }


        public ApplicationInfoDTO tokenType(TokenTypeEnum tokenType) {
        
        this.tokenType = tokenType;
        return this;
        }

    /**
        * Get tokenType
    * @return tokenType
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public TokenTypeEnum getTokenType() {
        return tokenType;
    }


    public void setTokenType(TokenTypeEnum tokenType) {
        this.tokenType = tokenType;
    }


        public ApplicationInfoDTO createdTime(String createdTime) {
        
        this.createdTime = createdTime;
        return this;
        }

    /**
        * Get createdTime
    * @return createdTime
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "1651555310208", value = "")
    
    public String getCreatedTime() {
        return createdTime;
    }


    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }


        public ApplicationInfoDTO keyManagers(List<KeyManagerInfoDTO> keyManagers) {
        
        this.keyManagers = keyManagers;
        return this;
        }

    /**
        * Get keyManagers
    * @return keyManagers
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public List<KeyManagerInfoDTO> getKeyManagers() {
        return keyManagers;
    }


    public void setKeyManagers(List<KeyManagerInfoDTO> keyManagers) {
        this.keyManagers = keyManagers;
    }


        public ApplicationInfoDTO status(String status) {
        
        this.status = status;
        return this;
        }

    /**
        * Get status
    * @return status
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "APPROVED", value = "")
    
    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


        public ApplicationInfoDTO groupId(String groupId) {
        
        this.groupId = groupId;
        return this;
        }

    /**
        * Get groupId
    * @return groupId
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public String getGroupId() {
        return groupId;
    }


    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
        return true;
        }
        if (o == null || getClass() != o.getClass()) {
        return false;
        }
            ApplicationInfoDTO applicationInfo = (ApplicationInfoDTO) o;
            return Objects.equals(this.applicationId, applicationInfo.applicationId) &&
            Objects.equals(this.name, applicationInfo.name) &&
            Objects.equals(this.owner, applicationInfo.owner) &&
            Objects.equals(this.tokenType, applicationInfo.tokenType) &&
            Objects.equals(this.createdTime, applicationInfo.createdTime) &&
            Objects.equals(this.keyManagers, applicationInfo.keyManagers) &&
            Objects.equals(this.status, applicationInfo.status) &&
            Objects.equals(this.groupId, applicationInfo.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, name, owner, tokenType, createdTime, keyManagers, status, groupId);
    }


@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("class ApplicationInfoDTO {\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    createdTime: ").append(toIndentedString(createdTime)).append("\n");
    sb.append("    keyManagers: ").append(toIndentedString(keyManagers)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
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

