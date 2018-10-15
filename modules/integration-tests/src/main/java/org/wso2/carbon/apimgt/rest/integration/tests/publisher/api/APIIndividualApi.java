package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import java.io.File;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.FileInfo;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.LifecycleState;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface APIIndividualApi extends ApiClient.Api {


  /**
   * Delete an API
   * This operation can be used to delete an existing API proving the Id of the API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void apisApiIdDelete(@Param("apiId") String apiId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get gateway definition
   * This operation can be used to retrieve the gateway configuration of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/gateway-config")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdGatewayConfigGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update gateway configuration
   * This operation can be used to update the gateway configuration of an existing API. gateway configuration to be updated is passed as a form data parameter &#x60;gatewayConfig&#x60;. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param gatewayConfig gateway configuration of the API (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("PUT /apis/{apiId}/gateway-config")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void apisApiIdGatewayConfigPut(@Param("apiId") String apiId, @Param("gatewayConfig") String gatewayConfig, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get details of an API
   * Using this operation, you can retrieve complete details of a single API. You need to provide the Id of the API to retrive it. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return API
   */
  @RequestLine("GET /apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  API apisApiIdGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get Lifecycle state data of the API.
   * This operation can be used to retrieve Lifecycle state data of the API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return LifecycleState
   */
  @RequestLine("GET /apis/{apiId}/lifecycle")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  LifecycleState apisApiIdLifecycleGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get Lifecycle state change history of the API.
   * This operation can be used to retrieve Lifecycle state change history of the API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/lifecycle-history")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdLifecycleHistoryGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Delete pending lifecycle state change tasks.
   * This operation can be used to remove pending lifecycle state change requests that are in pending state 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   */
  @RequestLine("DELETE /apis/{apiId}/lifecycle/lifecycle-pending-task")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void apisApiIdLifecycleLifecyclePendingTaskDelete(@Param("apiId") String apiId);

  /**
   * Update an API
   * This operation can be used to update an existing API. But the properties &#x60;name&#x60;, &#x60;version&#x60;, &#x60;context&#x60;, &#x60;provider&#x60;, &#x60;state&#x60; will not be changed by this operation. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body API object that needs to be added  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return API
   */
  @RequestLine("PUT /apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  API apisApiIdPut(@Param("apiId") String apiId, API body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get swagger definition
   * This operation can be used to retrieve the swagger definition of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return String
   */
  @RequestLine("GET /apis/{apiId}/swagger")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  String apisApiIdSwaggerGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update swagger definition
   * This operation can be used to update the swagger definition of an existing API. Swagger definition to be updated is passed as a form data parameter &#x60;apiDefinition&#x60;. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param endpointId Swagger definition of the API (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("PUT /apis/{apiId}/swagger")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void apisApiIdSwaggerPut(@Param("apiId") String apiId, @Param("endpointId") String endpointId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Delete a threat protection policy from an API
   * 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param policyId Threat protection policy id (required)
   */
  @RequestLine("DELETE /apis/{apiId}/threat-protection-policies?policyId={policyId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void apisApiIdThreatProtectionPoliciesDelete(@Param("apiId") String apiId, @Param("policyId") String policyId);

  /**
   * Delete a threat protection policy from an API
   * 
   * Note, this is equivalent to the other <code>apisApiIdThreatProtectionPoliciesDelete</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisApiIdThreatProtectionPoliciesDeleteQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>policyId - Threat protection policy id (required)</li>
   *   </ul>
   */
  @RequestLine("DELETE /apis/{apiId}/threat-protection-policies?policyId={policyId}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
  })
  void apisApiIdThreatProtectionPoliciesDelete(@Param("apiId") String apiId, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisApiIdThreatProtectionPoliciesDelete</code> method in a fluent style.
   */
  public static class ApisApiIdThreatProtectionPoliciesDeleteQueryParams extends HashMap<String, Object> {
    public ApisApiIdThreatProtectionPoliciesDeleteQueryParams policyId(final String value) {
      put("policyId", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get all threat protection policies associated with an API
   * 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @return List&lt;String&gt;
   */
  @RequestLine("GET /apis/{apiId}/threat-protection-policies")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<String> apisApiIdThreatProtectionPoliciesGet(@Param("apiId") String apiId);

  /**
   * Add a threat protection policy to an API
   * 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param policyId Threat protection policy id (required)
   */
  @RequestLine("POST /apis/{apiId}/threat-protection-policies?policyId={policyId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void apisApiIdThreatProtectionPoliciesPost(@Param("apiId") String apiId, @Param("policyId") String policyId);

  /**
   * Add a threat protection policy to an API
   * 
   * Note, this is equivalent to the other <code>apisApiIdThreatProtectionPoliciesPost</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisApiIdThreatProtectionPoliciesPostQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>policyId - Threat protection policy id (required)</li>
   *   </ul>
   */
  @RequestLine("POST /apis/{apiId}/threat-protection-policies?policyId={policyId}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
  })
  void apisApiIdThreatProtectionPoliciesPost(@Param("apiId") String apiId, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisApiIdThreatProtectionPoliciesPost</code> method in a fluent style.
   */
  public static class ApisApiIdThreatProtectionPoliciesPostQueryParams extends HashMap<String, Object> {
    public ApisApiIdThreatProtectionPoliciesPostQueryParams policyId(final String value) {
      put("policyId", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get thumbnail image
   * This operation can be used to download a thumbnail image of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/thumbnail")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdThumbnailGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Upload a thumbnail image
   * This operation can be used to upload a thumbnail image of an API. The thumbnail to be uploaded should be given as a form data parameter &#x60;file&#x60;. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param file Image to upload (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return FileInfo
   */
  @RequestLine("POST /apis/{apiId}/thumbnail")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  FileInfo apisApiIdThumbnailPost(@Param("apiId") String apiId, @Param("file") File file, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get swagger definition
   * This operation can be used to retrieve the swagger definition of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/wsdl")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/octet-stream",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdWsdlGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update WSDL definition
   * This operation can be used to update the WSDL definition of an existing API. WSDL to be updated is passed as a form data parameter &#x60;inlineContent&#x60;. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param file WSDL file or archive to upload (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("PUT /apis/{apiId}/wsdl")
  @Headers({
    "Content-Type: multipart/form-ldata",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void apisApiIdWsdlPut(@Param("apiId") String apiId, @Param("file") File file, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Change API Status
   * This operation is used to change the lifecycle of an API. Eg: Publish an API which is in &#x60;CREATED&#x60; state. In order to change the lifecycle, we need to provide the lifecycle &#x60;action&#x60; as a query parameter.  For example, to Publish an API, &#x60;action&#x60; should be &#x60;Publish&#x60;.  Some actions supports providing additional paramters which should be provided as &#x60;lifecycleChecklist&#x60; parameter. Please see parameters table for more information. 
    * @param action The action to demote or promote the state of the API.  Supported actions are [ **Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Move to Maintenance, Deprecate, Re-Publish, Retire **]  (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)
    * @param lifecycleChecklist  You can specify additional checklist items by using an **\&quot;attribute:\&quot;** modifier.  Eg: \&quot;Deprecate Old Versions:true\&quot; will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \&quot;attribute1:true, attribute2:false\&quot; format.  Supported checklist items are as follows. 1. **Deprecate Old Versions**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Require Re-Subscription**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version.  (optional)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return WorkflowResponse
   */
  @RequestLine("POST /apis/change-lifecycle?action={action}&lifecycleChecklist={lifecycleChecklist}&apiId={apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  WorkflowResponse apisChangeLifecyclePost(@Param("action") String action, @Param("apiId") String apiId, @Param("lifecycleChecklist") String lifecycleChecklist, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Change API Status
   * This operation is used to change the lifecycle of an API. Eg: Publish an API which is in &#x60;CREATED&#x60; state. In order to change the lifecycle, we need to provide the lifecycle &#x60;action&#x60; as a query parameter.  For example, to Publish an API, &#x60;action&#x60; should be &#x60;Publish&#x60;.  Some actions supports providing additional paramters which should be provided as &#x60;lifecycleChecklist&#x60; parameter. Please see parameters table for more information. 
   * Note, this is equivalent to the other <code>apisChangeLifecyclePost</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisChangeLifecyclePostQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
   * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>action - The action to demote or promote the state of the API.  Supported actions are [ **Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Move to Maintenance, Deprecate, Re-Publish, Retire **]  (required)</li>
   *   <li>lifecycleChecklist -  You can specify additional checklist items by using an **\&quot;attribute:\&quot;** modifier.  Eg: \&quot;Deprecate Old Versions:true\&quot; will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \&quot;attribute1:true, attribute2:false\&quot; format.  Supported checklist items are as follows. 1. **Deprecate Old Versions**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Require Re-Subscription**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version.  (optional)</li>
   *   <li>apiId - **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)</li>
   *   </ul>
   * @return WorkflowResponse
   */
  @RequestLine("POST /apis/change-lifecycle?action={action}&lifecycleChecklist={lifecycleChecklist}&apiId={apiId}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-Match: {ifMatch}",
      
      "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  WorkflowResponse apisChangeLifecyclePost(@Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisChangeLifecyclePost</code> method in a fluent style.
   */
  public static class ApisChangeLifecyclePostQueryParams extends HashMap<String, Object> {
    public ApisChangeLifecyclePostQueryParams action(final String value) {
      put("action", EncodingUtils.encode(value));
      return this;
    }
    public ApisChangeLifecyclePostQueryParams lifecycleChecklist(final String value) {
      put("lifecycleChecklist", EncodingUtils.encode(value));
      return this;
    }
    public ApisChangeLifecyclePostQueryParams apiId(final String value) {
      put("apiId", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Create a new API version
   * This operation can be used to create a new version of an existing API. The new version is specified as &#x60;newVersion&#x60; query parameter. New API will be in &#x60;CREATED&#x60; state. 
    * @param newVersion Version of the new API. (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)
   */
  @RequestLine("POST /apis/copy-api?newVersion={newVersion}&apiId={apiId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void apisCopyApiPost(@Param("newVersion") String newVersion, @Param("apiId") String apiId);

  /**
   * Create a new API version
   * This operation can be used to create a new version of an existing API. The new version is specified as &#x60;newVersion&#x60; query parameter. New API will be in &#x60;CREATED&#x60; state. 
   * Note, this is equivalent to the other <code>apisCopyApiPost</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisCopyApiPostQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>newVersion - Version of the new API. (required)</li>
   *   <li>apiId - **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)</li>
   *   </ul>
   */
  @RequestLine("POST /apis/copy-api?newVersion={newVersion}&apiId={apiId}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
  })
  void apisCopyApiPost(@QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisCopyApiPost</code> method in a fluent style.
   */
  public static class ApisCopyApiPostQueryParams extends HashMap<String, Object> {
    public ApisCopyApiPostQueryParams newVersion(final String value) {
      put("newVersion", EncodingUtils.encode(value));
      return this;
    }
    public ApisCopyApiPostQueryParams apiId(final String value) {
      put("apiId", EncodingUtils.encode(value));
      return this;
    }
  }
}
