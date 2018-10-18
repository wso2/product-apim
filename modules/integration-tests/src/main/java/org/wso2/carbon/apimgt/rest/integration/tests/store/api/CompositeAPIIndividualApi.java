package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CompositeAPI;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import java.io.File;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface CompositeAPIIndividualApi extends ApiClient.Api {


  /**
   * Delete a Composite API
   * This operation can be used to delete an existing Composite API proving the Id of the Composite API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /composite-apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void compositeApisApiIdDelete(@Param("apiId") String apiId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get details of a Composite API
   * Using this operation, you can retrieve complete details of a single Composite API. You need to provide the Id of the Composite API to retrive it. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return CompositeAPI
   */
  @RequestLine("GET /composite-apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  CompositeAPI compositeApisApiIdGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get Composite API implementation
   * This operation can be used to retrieve the Ballerina implementation of a Composite API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /composite-apis/{apiId}/implementation")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void compositeApisApiIdImplementationGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update Composite API implementation
   * This operation can be used to update the Ballerina implementation of a Composite API. Ballerina implementation to be updated is passed as a form data parameter &#x60;apiImplementation&#x60;. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param apiImplementation Ballerina implementation of the Composite API (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return FileInfo
   */
  @RequestLine("PUT /composite-apis/{apiId}/implementation")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  FileInfo compositeApisApiIdImplementationPut(@Param("apiId") String apiId, @Param("apiImplementation") File apiImplementation, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Update a Composite API
   * This operation can be used to update an existing Composite API. But the properties &#x60;name&#x60;, &#x60;version&#x60;, &#x60;context&#x60;, &#x60;provider&#x60;, &#x60;state&#x60; will not be changed by this operation. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body API object that needs to be added  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return CompositeAPI
   */
  @RequestLine("PUT /composite-apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  CompositeAPI compositeApisApiIdPut(@Param("apiId") String apiId, CompositeAPI body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get swagger definition
   * This operation can be used to retrieve the swagger definition of a Composite API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /composite-apis/{apiId}/swagger")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void compositeApisApiIdSwaggerGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update swagger definition
   * This operation can be used to update the swagger definition of an existing Composite API. Swagger definition to be updated is passed as a form data parameter &#x60;apiDefinition&#x60;. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param apiDefinition Swagger definition of the Composite API (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("PUT /composite-apis/{apiId}/swagger")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void compositeApisApiIdSwaggerPut(@Param("apiId") String apiId, @Param("apiDefinition") String apiDefinition, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
