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
import org.wso2.am.integration.clients.admin.api.dto.SettingsGatewayConfigurationDTO;
import org.wso2.am.integration.clients.admin.api.dto.SettingsKeyManagerConfigurationDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
/**
* SettingsDTO
*/

public class SettingsDTO {
        public static final String SERIALIZED_NAME_SCOPES = "scopes";
        @SerializedName(SERIALIZED_NAME_SCOPES)
            private List<String> scopes = null;

        public static final String SERIALIZED_NAME_GATEWAY_TYPES = "gatewayTypes";
        @SerializedName(SERIALIZED_NAME_GATEWAY_TYPES)
            private List<String> gatewayTypes = null;

        public static final String SERIALIZED_NAME_IS_J_W_T_ENABLED_FOR_LOGIN_TOKENS = "IsJWTEnabledForLoginTokens";
        @SerializedName(SERIALIZED_NAME_IS_J_W_T_ENABLED_FOR_LOGIN_TOKENS)
            private Boolean isJWTEnabledForLoginTokens = false;

        public static final String SERIALIZED_NAME_ORG_ACCESS_CONTROL_ENABLED = "orgAccessControlEnabled";
        @SerializedName(SERIALIZED_NAME_ORG_ACCESS_CONTROL_ENABLED)
            private Boolean orgAccessControlEnabled;

        public static final String SERIALIZED_NAME_KEY_MANAGER_CONFIGURATION = "keyManagerConfiguration";
        @SerializedName(SERIALIZED_NAME_KEY_MANAGER_CONFIGURATION)
            private List<SettingsKeyManagerConfigurationDTO> keyManagerConfiguration = null;

        public static final String SERIALIZED_NAME_GATEWAY_CONFIGURATION = "gatewayConfiguration";
        @SerializedName(SERIALIZED_NAME_GATEWAY_CONFIGURATION)
            private List<SettingsGatewayConfigurationDTO> gatewayConfiguration = null;

        public static final String SERIALIZED_NAME_ANALYTICS_ENABLED = "analyticsEnabled";
        @SerializedName(SERIALIZED_NAME_ANALYTICS_ENABLED)
            private Boolean analyticsEnabled;

        public static final String SERIALIZED_NAME_TRANSACTION_COUNTER_ENABLE = "transactionCounterEnable";
        @SerializedName(SERIALIZED_NAME_TRANSACTION_COUNTER_ENABLE)
            private Boolean transactionCounterEnable;

        public static final String SERIALIZED_NAME_IS_GATEWAY_NOTIFICATION_ENABLED = "isGatewayNotificationEnabled";
        @SerializedName(SERIALIZED_NAME_IS_GATEWAY_NOTIFICATION_ENABLED)
            private Boolean isGatewayNotificationEnabled = false;

        public static final String SERIALIZED_NAME_UNIVERSAL_GATEWAY_VERSION = "universalGatewayVersion";
        @SerializedName(SERIALIZED_NAME_UNIVERSAL_GATEWAY_VERSION)
            private String universalGatewayVersion;

        public static final String SERIALIZED_NAME_CONSUMPTION_EXPORT_ENABLED = "consumptionExportEnabled";
        @SerializedName(SERIALIZED_NAME_CONSUMPTION_EXPORT_ENABLED)
            private Boolean consumptionExportEnabled;


        public SettingsDTO scopes(List<String> scopes) {
        
        this.scopes = scopes;
        return this;
        }

    /**
        * Get scopes
    * @return scopes
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public List<String> getScopes() {
        return scopes;
    }


    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }


        public SettingsDTO gatewayTypes(List<String> gatewayTypes) {
        
        this.gatewayTypes = gatewayTypes;
        return this;
        }

    /**
        * Get gatewayTypes
    * @return gatewayTypes
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public List<String> getGatewayTypes() {
        return gatewayTypes;
    }


    public void setGatewayTypes(List<String> gatewayTypes) {
        this.gatewayTypes = gatewayTypes;
    }


        public SettingsDTO isJWTEnabledForLoginTokens(Boolean isJWTEnabledForLoginTokens) {
        
        this.isJWTEnabledForLoginTokens = isJWTEnabledForLoginTokens;
        return this;
        }

    /**
        * Get isJWTEnabledForLoginTokens
    * @return isJWTEnabledForLoginTokens
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public Boolean isIsJWTEnabledForLoginTokens() {
        return isJWTEnabledForLoginTokens;
    }


    public void setIsJWTEnabledForLoginTokens(Boolean isJWTEnabledForLoginTokens) {
        this.isJWTEnabledForLoginTokens = isJWTEnabledForLoginTokens;
    }


        public SettingsDTO orgAccessControlEnabled(Boolean orgAccessControlEnabled) {
        
        this.orgAccessControlEnabled = orgAccessControlEnabled;
        return this;
        }

    /**
        * Is Organization-based access control configuration enabled 
    * @return orgAccessControlEnabled
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "true", value = "Is Organization-based access control configuration enabled ")
    
    public Boolean isOrgAccessControlEnabled() {
        return orgAccessControlEnabled;
    }


    public void setOrgAccessControlEnabled(Boolean orgAccessControlEnabled) {
        this.orgAccessControlEnabled = orgAccessControlEnabled;
    }


        public SettingsDTO keyManagerConfiguration(List<SettingsKeyManagerConfigurationDTO> keyManagerConfiguration) {
        
        this.keyManagerConfiguration = keyManagerConfiguration;
        return this;
        }

    /**
        * Get keyManagerConfiguration
    * @return keyManagerConfiguration
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public List<SettingsKeyManagerConfigurationDTO> getKeyManagerConfiguration() {
        return keyManagerConfiguration;
    }


    public void setKeyManagerConfiguration(List<SettingsKeyManagerConfigurationDTO> keyManagerConfiguration) {
        this.keyManagerConfiguration = keyManagerConfiguration;
    }


        public SettingsDTO gatewayConfiguration(List<SettingsGatewayConfigurationDTO> gatewayConfiguration) {
        
        this.gatewayConfiguration = gatewayConfiguration;
        return this;
        }

    /**
        * Get gatewayConfiguration
    * @return gatewayConfiguration
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "")
    
    public List<SettingsGatewayConfigurationDTO> getGatewayConfiguration() {
        return gatewayConfiguration;
    }


    public void setGatewayConfiguration(List<SettingsGatewayConfigurationDTO> gatewayConfiguration) {
        this.gatewayConfiguration = gatewayConfiguration;
    }


        public SettingsDTO analyticsEnabled(Boolean analyticsEnabled) {
        
        this.analyticsEnabled = analyticsEnabled;
        return this;
        }

    /**
        * To determine whether analytics is enabled or not
    * @return analyticsEnabled
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "false", value = "To determine whether analytics is enabled or not")
    
    public Boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }


    public void setAnalyticsEnabled(Boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }


        public SettingsDTO transactionCounterEnable(Boolean transactionCounterEnable) {
        
        this.transactionCounterEnable = transactionCounterEnable;
        return this;
        }

    /**
        * To determine whether the transaction counter is enabled or not
    * @return transactionCounterEnable
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "false", value = "To determine whether the transaction counter is enabled or not")
    
    public Boolean isTransactionCounterEnable() {
        return transactionCounterEnable;
    }


    public void setTransactionCounterEnable(Boolean transactionCounterEnable) {
        this.transactionCounterEnable = transactionCounterEnable;
    }


        public SettingsDTO isGatewayNotificationEnabled(Boolean isGatewayNotificationEnabled) {
        
        this.isGatewayNotificationEnabled = isGatewayNotificationEnabled;
        return this;
        }

    /**
        * Is Gateway Notification Enabled
    * @return isGatewayNotificationEnabled
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(value = "Is Gateway Notification Enabled")
    
    public Boolean isIsGatewayNotificationEnabled() {
        return isGatewayNotificationEnabled;
    }


    public void setIsGatewayNotificationEnabled(Boolean isGatewayNotificationEnabled) {
        this.isGatewayNotificationEnabled = isGatewayNotificationEnabled;
    }


        public SettingsDTO universalGatewayVersion(String universalGatewayVersion) {
        
        this.universalGatewayVersion = universalGatewayVersion;
        return this;
        }

    /**
        * Universal Gateway version for quick-start guide (e.g. \&quot;0.11.0\&quot;)
    * @return universalGatewayVersion
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "1.0.0", value = "Universal Gateway version for quick-start guide (e.g. \"0.11.0\")")
    
    public String getUniversalGatewayVersion() {
        return universalGatewayVersion;
    }


    public void setUniversalGatewayVersion(String universalGatewayVersion) {
        this.universalGatewayVersion = universalGatewayVersion;
    }


        public SettingsDTO consumptionExportEnabled(Boolean consumptionExportEnabled) {
        
        this.consumptionExportEnabled = consumptionExportEnabled;
        return this;
        }

    /**
        * Whether the ConsumptionDataExportService OSGi service is available
    * @return consumptionExportEnabled
    **/
        @javax.annotation.Nullable
      @ApiModelProperty(example = "false", value = "Whether the ConsumptionDataExportService OSGi service is available")
    
    public Boolean isConsumptionExportEnabled() {
        return consumptionExportEnabled;
    }


    public void setConsumptionExportEnabled(Boolean consumptionExportEnabled) {
        this.consumptionExportEnabled = consumptionExportEnabled;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
        return true;
        }
        if (o == null || getClass() != o.getClass()) {
        return false;
        }
            SettingsDTO settings = (SettingsDTO) o;
            return Objects.equals(this.scopes, settings.scopes) &&
            Objects.equals(this.gatewayTypes, settings.gatewayTypes) &&
            Objects.equals(this.isJWTEnabledForLoginTokens, settings.isJWTEnabledForLoginTokens) &&
            Objects.equals(this.orgAccessControlEnabled, settings.orgAccessControlEnabled) &&
            Objects.equals(this.keyManagerConfiguration, settings.keyManagerConfiguration) &&
            Objects.equals(this.gatewayConfiguration, settings.gatewayConfiguration) &&
            Objects.equals(this.analyticsEnabled, settings.analyticsEnabled) &&
            Objects.equals(this.transactionCounterEnable, settings.transactionCounterEnable) &&
            Objects.equals(this.isGatewayNotificationEnabled, settings.isGatewayNotificationEnabled) &&
            Objects.equals(this.universalGatewayVersion, settings.universalGatewayVersion) &&
            Objects.equals(this.consumptionExportEnabled, settings.consumptionExportEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopes, gatewayTypes, isJWTEnabledForLoginTokens, orgAccessControlEnabled, keyManagerConfiguration, gatewayConfiguration, analyticsEnabled, transactionCounterEnable, isGatewayNotificationEnabled, universalGatewayVersion, consumptionExportEnabled);
    }


@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("class SettingsDTO {\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
    sb.append("    gatewayTypes: ").append(toIndentedString(gatewayTypes)).append("\n");
    sb.append("    isJWTEnabledForLoginTokens: ").append(toIndentedString(isJWTEnabledForLoginTokens)).append("\n");
    sb.append("    orgAccessControlEnabled: ").append(toIndentedString(orgAccessControlEnabled)).append("\n");
    sb.append("    keyManagerConfiguration: ").append(toIndentedString(keyManagerConfiguration)).append("\n");
    sb.append("    gatewayConfiguration: ").append(toIndentedString(gatewayConfiguration)).append("\n");
    sb.append("    analyticsEnabled: ").append(toIndentedString(analyticsEnabled)).append("\n");
    sb.append("    transactionCounterEnable: ").append(toIndentedString(transactionCounterEnable)).append("\n");
    sb.append("    isGatewayNotificationEnabled: ").append(toIndentedString(isGatewayNotificationEnabled)).append("\n");
    sb.append("    universalGatewayVersion: ").append(toIndentedString(universalGatewayVersion)).append("\n");
    sb.append("    consumptionExportEnabled: ").append(toIndentedString(consumptionExportEnabled)).append("\n");
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

