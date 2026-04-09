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
* APIKeyDTO
*/

public class APIKeyDTO {
        public static final String SERIALIZED_NAME_KEY_U_U_I_D = "keyUUID";
        @SerializedName(SERIALIZED_NAME_KEY_U_U_I_D)
            private String keyUUID;

        public static final String SERIALIZED_NAME_KEY_NAME = "keyName";
        @SerializedName(SERIALIZED_NAME_KEY_NAME)
            private String keyName;

        public static final String SERIALIZED_NAME_API_NAME = "apiName";
        @SerializedName(SERIALIZED_NAME_API_NAME)
            private String apiName;

        public static final String SERIALIZED_NAME_APPLICATION_NAME = "applicationName";
        @SerializedName(SERIALIZED_NAME_APPLICATION_NAME)
            private String applicationName;

            /**
* Application Key Type
*/
    @JsonAdapter(KeyTypeEnum.Adapter.class)
public enum KeyTypeEnum {
        PRODUCTION("PRODUCTION"),
        
        SANDBOX("SANDBOX");

private String value;

KeyTypeEnum(String value) {
this.value = value;
}

public String getValue() {
return value;
}

@Override
public String toString() {
return String.valueOf(value);
}

public static KeyTypeEnum fromValue(String value) {
    for (KeyTypeEnum b : KeyTypeEnum.values()) {
    if (b.name().equals(value)) {
        return b;
    }
}
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
}

    public static class Adapter extends TypeAdapter<KeyTypeEnum> {
    @Override
    public void write(final JsonWriter jsonWriter, final KeyTypeEnum enumeration) throws IOException {
    jsonWriter.value(enumeration.getValue());
    }

    @Override
    public KeyTypeEnum read(final JsonReader jsonReader) throws IOException {
    String value =  jsonReader.nextString();
    return KeyTypeEnum.fromValue(value);
    }
    }
}

        public static final String SERIALIZED_NAME_KEY_TYPE = "keyType";
        @SerializedName(SERIALIZED_NAME_KEY_TYPE)
            private KeyTypeEnum keyType;

        public static final String SERIALIZED_NAME_USER = "user";
        @SerializedName(SERIALIZED_NAME_USER)
            private String user;

        public static final String SERIALIZED_NAME_ISSUED_ON = "issuedOn";
        @SerializedName(SERIALIZED_NAME_ISSUED_ON)
            private Long issuedOn;

        public static final String SERIALIZED_NAME_VALIDITY_PERIOD = "validityPeriod";
        @SerializedName(SERIALIZED_NAME_VALIDITY_PERIOD)
            private Long validityPeriod;

        public static final String SERIALIZED_NAME_LAST_USED = "lastUsed";
        @SerializedName(SERIALIZED_NAME_LAST_USED)
            private Long lastUsed;


        public APIKeyDTO keyUUID(String keyUUID) {
        
        this.keyUUID = keyUUID;
        return this;
        }

    /**
        * The UUID of the API key
    * @return keyUUID
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The UUID of the API key")
    
    public String getKeyUUID() {
        return keyUUID;
    }


    public void setKeyUUID(String keyUUID) {
        this.keyUUID = keyUUID;
    }


        public APIKeyDTO keyName(String keyName) {
        
        this.keyName = keyName;
        return this;
        }

    /**
        * API Key name
    * @return keyName
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "Test_Key", value = "API Key name")
    
    public String getKeyName() {
        return keyName;
    }


    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }


        public APIKeyDTO apiName(String apiName) {
        
        this.apiName = apiName;
        return this;
        }

    /**
        * API Name
    * @return apiName
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "NotificationsAPI", value = "API Name")
    
    public String getApiName() {
        return apiName;
    }


    public void setApiName(String apiName) {
        this.apiName = apiName;
    }


        public APIKeyDTO applicationName(String applicationName) {
        
        this.applicationName = applicationName;
        return this;
        }

    /**
        * Application Name
    * @return applicationName
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "DefaultApplication", value = "Application Name")
    
    public String getApplicationName() {
        return applicationName;
    }


    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }


        public APIKeyDTO keyType(KeyTypeEnum keyType) {
        
        this.keyType = keyType;
        return this;
        }

    /**
        * Application Key Type
    * @return keyType
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "PRODUCTION", value = "Application Key Type")
    
    public KeyTypeEnum getKeyType() {
        return keyType;
    }


    public void setKeyType(KeyTypeEnum keyType) {
        this.keyType = keyType;
    }


        public APIKeyDTO user(String user) {
        
        this.user = user;
        return this;
        }

    /**
        * Owner of the Application
    * @return user
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "Bob", value = "Owner of the Application")
    
    public String getUser() {
        return user;
    }


    public void setUser(String user) {
        this.user = user;
    }


        public APIKeyDTO issuedOn(Long issuedOn) {
        
        this.issuedOn = issuedOn;
        return this;
        }

    /**
        * Created time in Unix epoch milliseconds
    * @return issuedOn
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "1599196134000", value = "Created time in Unix epoch milliseconds")
    
    public Long getIssuedOn() {
        return issuedOn;
    }


    public void setIssuedOn(Long issuedOn) {
        this.issuedOn = issuedOn;
    }


        public APIKeyDTO validityPeriod(Long validityPeriod) {
        
        this.validityPeriod = validityPeriod;
        return this;
        }

    /**
        * Get validityPeriod
    * @return validityPeriod
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "3600", value = "")
    
    public Long getValidityPeriod() {
        return validityPeriod;
    }


    public void setValidityPeriod(Long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }


        public APIKeyDTO lastUsed(Long lastUsed) {
        
        this.lastUsed = lastUsed;
        return this;
        }

    /**
        * Last used time in Unix epoch milliseconds
    * @return lastUsed
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "1599196134000", value = "Last used time in Unix epoch milliseconds")
    
    public Long getLastUsed() {
        return lastUsed;
    }


    public void setLastUsed(Long lastUsed) {
        this.lastUsed = lastUsed;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
        return true;
        }
        if (o == null || getClass() != o.getClass()) {
        return false;
        }
            APIKeyDTO apIKey = (APIKeyDTO) o;
            return Objects.equals(this.keyUUID, apIKey.keyUUID) &&
            Objects.equals(this.keyName, apIKey.keyName) &&
            Objects.equals(this.apiName, apIKey.apiName) &&
            Objects.equals(this.applicationName, apIKey.applicationName) &&
            Objects.equals(this.keyType, apIKey.keyType) &&
            Objects.equals(this.user, apIKey.user) &&
            Objects.equals(this.issuedOn, apIKey.issuedOn) &&
            Objects.equals(this.validityPeriod, apIKey.validityPeriod) &&
            Objects.equals(this.lastUsed, apIKey.lastUsed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyUUID, keyName, apiName, applicationName, keyType, user, issuedOn, validityPeriod, lastUsed);
    }


@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("class APIKeyDTO {\n");
    sb.append("    keyUUID: ").append(toIndentedString(keyUUID)).append("\n");
    sb.append("    keyName: ").append(toIndentedString(keyName)).append("\n");
    sb.append("    apiName: ").append(toIndentedString(apiName)).append("\n");
    sb.append("    applicationName: ").append(toIndentedString(applicationName)).append("\n");
    sb.append("    keyType: ").append(toIndentedString(keyType)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    issuedOn: ").append(toIndentedString(issuedOn)).append("\n");
    sb.append("    validityPeriod: ").append(toIndentedString(validityPeriod)).append("\n");
    sb.append("    lastUsed: ").append(toIndentedString(lastUsed)).append("\n");
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

