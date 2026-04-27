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
* APIInfoKeyManagerDTO
*/

public class APIInfoKeyManagerDTO {
        public static final String SERIALIZED_NAME_ID = "id";
        @SerializedName(SERIALIZED_NAME_ID)
            private String id;

        public static final String SERIALIZED_NAME_TYPE = "type";
        @SerializedName(SERIALIZED_NAME_TYPE)
            private String type;

        public static final String SERIALIZED_NAME_NAME = "name";
        @SerializedName(SERIALIZED_NAME_NAME)
            private String name;

        public static final String SERIALIZED_NAME_TRANSPORT_TYPE = "transportType";
        @SerializedName(SERIALIZED_NAME_TRANSPORT_TYPE)
            private String transportType;

        public static final String SERIALIZED_NAME_DESCRIPTION = "description";
        @SerializedName(SERIALIZED_NAME_DESCRIPTION)
            private String description;

        public static final String SERIALIZED_NAME_CONTEXT = "context";
        @SerializedName(SERIALIZED_NAME_CONTEXT)
            private String context;

        public static final String SERIALIZED_NAME_VERSION = "version";
        @SerializedName(SERIALIZED_NAME_VERSION)
            private String version;

        public static final String SERIALIZED_NAME_PROVIDER = "provider";
        @SerializedName(SERIALIZED_NAME_PROVIDER)
            private String provider;

        public static final String SERIALIZED_NAME_STATUS = "status";
        @SerializedName(SERIALIZED_NAME_STATUS)
            private String status;

        public static final String SERIALIZED_NAME_THUMBNAIL_URI = "thumbnailUri";
        @SerializedName(SERIALIZED_NAME_THUMBNAIL_URI)
            private String thumbnailUri;

        public static final String SERIALIZED_NAME_ADVERTISE_ONLY = "advertiseOnly";
        @SerializedName(SERIALIZED_NAME_ADVERTISE_ONLY)
            private Boolean advertiseOnly;

        public static final String SERIALIZED_NAME_KEY_MANAGER_ENTRY = "keyManagerEntry";
        @SerializedName(SERIALIZED_NAME_KEY_MANAGER_ENTRY)
            private String keyManagerEntry;


        public APIInfoKeyManagerDTO id(String id) {
        
        this.id = id;
        return this;
        }

    /**
        * The ID of the API.
    * @return id
    **/
      @ApiModelProperty(required = true, value = "The ID of the API.")
    
    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


        public APIInfoKeyManagerDTO type(String type) {
        
        this.type = type;
        return this;
        }

    /**
        * The type of the entry (e.g., \&quot;API\&quot;).
    * @return type
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The type of the entry (e.g., \"API\").")
    
    public String getType() {
        return type;
    }


    public void setType(String type) {
        this.type = type;
    }


        public APIInfoKeyManagerDTO name(String name) {
        
        this.name = name;
        return this;
        }

    /**
        * The name of the API.
    * @return name
    **/
      @ApiModelProperty(required = true, value = "The name of the API.")
    
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


        public APIInfoKeyManagerDTO transportType(String transportType) {
        
        this.transportType = transportType;
        return this;
        }

    /**
        * The transport type of the API.
    * @return transportType
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The transport type of the API.")
    
    public String getTransportType() {
        return transportType;
    }


    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }


        public APIInfoKeyManagerDTO description(String description) {
        
        this.description = description;
        return this;
        }

    /**
        * The description of the API.
    * @return description
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The description of the API.")
    
    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


        public APIInfoKeyManagerDTO context(String context) {
        
        this.context = context;
        return this;
        }

    /**
        * The context of the API.
    * @return context
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The context of the API.")
    
    public String getContext() {
        return context;
    }


    public void setContext(String context) {
        this.context = context;
    }


        public APIInfoKeyManagerDTO version(String version) {
        
        this.version = version;
        return this;
        }

    /**
        * The version of the API.
    * @return version
    **/
      @ApiModelProperty(required = true, value = "The version of the API.")
    
    public String getVersion() {
        return version;
    }


    public void setVersion(String version) {
        this.version = version;
    }


        public APIInfoKeyManagerDTO provider(String provider) {
        
        this.provider = provider;
        return this;
        }

    /**
        * The provider of the API.
    * @return provider
    **/
      @ApiModelProperty(required = true, value = "The provider of the API.")
    
    public String getProvider() {
        return provider;
    }


    public void setProvider(String provider) {
        this.provider = provider;
    }


        public APIInfoKeyManagerDTO status(String status) {
        
        this.status = status;
        return this;
        }

    /**
        * The status of the API.
    * @return status
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The status of the API.")
    
    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


        public APIInfoKeyManagerDTO thumbnailUri(String thumbnailUri) {
        
        this.thumbnailUri = thumbnailUri;
        return this;
        }

    /**
        * The URI of the thumbnail of the API.
    * @return thumbnailUri
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The URI of the thumbnail of the API.")
    
    public String getThumbnailUri() {
        return thumbnailUri;
    }


    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }


        public APIInfoKeyManagerDTO advertiseOnly(Boolean advertiseOnly) {
        
        this.advertiseOnly = advertiseOnly;
        return this;
        }

    /**
        * Indicates if the API is advertised only.
    * @return advertiseOnly
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "Indicates if the API is advertised only.")
    
    public Boolean isAdvertiseOnly() {
        return advertiseOnly;
    }


    public void setAdvertiseOnly(Boolean advertiseOnly) {
        this.advertiseOnly = advertiseOnly;
    }


        public APIInfoKeyManagerDTO keyManagerEntry(String keyManagerEntry) {
        
        this.keyManagerEntry = keyManagerEntry;
        return this;
        }

    /**
        * The key manager entry related to the API.
    * @return keyManagerEntry
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "The key manager entry related to the API.")
    
    public String getKeyManagerEntry() {
        return keyManagerEntry;
    }


    public void setKeyManagerEntry(String keyManagerEntry) {
        this.keyManagerEntry = keyManagerEntry;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
        return true;
        }
        if (o == null || getClass() != o.getClass()) {
        return false;
        }
            APIInfoKeyManagerDTO apIInfoKeyManager = (APIInfoKeyManagerDTO) o;
            return Objects.equals(this.id, apIInfoKeyManager.id) &&
            Objects.equals(this.type, apIInfoKeyManager.type) &&
            Objects.equals(this.name, apIInfoKeyManager.name) &&
            Objects.equals(this.transportType, apIInfoKeyManager.transportType) &&
            Objects.equals(this.description, apIInfoKeyManager.description) &&
            Objects.equals(this.context, apIInfoKeyManager.context) &&
            Objects.equals(this.version, apIInfoKeyManager.version) &&
            Objects.equals(this.provider, apIInfoKeyManager.provider) &&
            Objects.equals(this.status, apIInfoKeyManager.status) &&
            Objects.equals(this.thumbnailUri, apIInfoKeyManager.thumbnailUri) &&
            Objects.equals(this.advertiseOnly, apIInfoKeyManager.advertiseOnly) &&
            Objects.equals(this.keyManagerEntry, apIInfoKeyManager.keyManagerEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name, transportType, description, context, version, provider, status, thumbnailUri, advertiseOnly, keyManagerEntry);
    }


@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("class APIInfoKeyManagerDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    transportType: ").append(toIndentedString(transportType)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    context: ").append(toIndentedString(context)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    thumbnailUri: ").append(toIndentedString(thumbnailUri)).append("\n");
    sb.append("    advertiseOnly: ").append(toIndentedString(advertiseOnly)).append("\n");
    sb.append("    keyManagerEntry: ").append(toIndentedString(keyManagerEntry)).append("\n");
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

