package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.DedicatedGateway;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface DedicatedGatewayIndividualApi extends ApiClient.Api {


  /**
   * Get details of dedicated-gateway
   * This operation can be used to retrieve whether the dedicated gateway is enabled in a certain Composite API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return DedicatedGateway
   */
  @RequestLine("GET /composite-apis/{apiId}/dedicated-gateway")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  DedicatedGateway compositeApisApiIdDedicatedGatewayGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String
          ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update enabling of dedicated Gateway of Composite API
   * This operation can be used to update metadata of an API&#39;s dedicated-gateway. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body dedicated Gateway object that needs to be added  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return DedicatedGateway
   */
  @RequestLine("PUT /composite-apis/{apiId}/dedicated-gateway")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  DedicatedGateway compositeApisApiIdDedicatedGatewayPut(@Param("apiId") String apiId, DedicatedGateway body, @Param
          ("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
