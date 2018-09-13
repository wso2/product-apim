package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndPoint;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-09-11T19:34:51.739+05:30")
public interface EndpointIndividualApi extends ApiClient.Api {


  /**
   * Delete an endpoint
   * This operation can be used to delete an existing Endpoint proving the Id of the Endpoint. 
    * @param endpointId **Endpoint ID** consisting of the **UUID** of the Endpoint**.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /endpoints/{endpointId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void endpointsEndpointIdDelete(@Param("endpointId") String endpointId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get specific endpoints
   * This operation can be used to retrieve endpoint specific details. 
    * @param endpointId **Endpoint ID** consisting of the **UUID** of the Endpoint**.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return EndPoint
   */
  @RequestLine("GET /endpoints/{endpointId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  EndPoint endpointsEndpointIdGet(@Param("endpointId") String endpointId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Update a Tier
   * This operation can be used to update an existing endpoint. &#x60;PUT https://127.0.0.1:9443/api/am/publisher/v1.0/endpoints/api/Low&#x60; 
    * @param endpointId **Endpoint ID** consisting of the **UUID** of the Endpoint**.  (required)
    * @param body Tier object that needs to be modified  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return EndPoint
   */
  @RequestLine("PUT /endpoints/{endpointId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  EndPoint endpointsEndpointIdPut(@Param("endpointId") String endpointId, EndPoint body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
