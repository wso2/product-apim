package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeyGenerateRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeyMappingRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeys;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeysList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationToken;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationTokenGenerateRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import java.io.File;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ApplicationIndividualApi extends ApiClient.Api {


  /**
   * Delete an application
   * Delete an application 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /applications/{applicationId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void applicationsApplicationIdDelete(@Param("applicationId") String applicationId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Generate application keys
   * Generate keys (Consumer key/secret) for application 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param body Application key generation request object  (required)
   * @return ApplicationKeys
   */
  @RequestLine("POST /applications/{applicationId}/generate-keys")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApplicationKeys applicationsApplicationIdGenerateKeysPost(@Param("applicationId") String applicationId, ApplicationKeyGenerateRequest body);

  /**
   * Generate application token
   * Generate an access token for application by client_credentials grant type 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param body Application token generation request object  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return ApplicationToken
   */
  @RequestLine("POST /applications/{applicationId}/generate-token")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  ApplicationToken applicationsApplicationIdGenerateTokenPost(@Param("applicationId") String applicationId, ApplicationTokenGenerateRequest body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get details of an application
   * Get application details 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Application
   */
  @RequestLine("GET /applications/{applicationId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Application applicationsApplicationIdGet(@Param("applicationId") String applicationId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Retrieve all application keys
   * Retrieve keys (Consumer key/secret) of application 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
   * @return ApplicationKeysList
   */
  @RequestLine("GET /applications/{applicationId}/keys")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApplicationKeysList applicationsApplicationIdKeysGet(@Param("applicationId") String applicationId);

  /**
   * Retrieve application keys for a provided type
   * Retrieve keys (Consumer key/secret) of application by a given type 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param keyType **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  (required)
   * @return ApplicationKeys
   */
  @RequestLine("GET /applications/{applicationId}/keys/{keyType}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApplicationKeys applicationsApplicationIdKeysKeyTypeGet(@Param("applicationId") String applicationId, @Param("keyType") String keyType);

  /**
   * Update an application key
   * Update grant types and callback url (Consumer Key and Consumer Secret are ignored) 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param keyType **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  (required)
    * @param body Grant types/Callback URL update request object  (required)
   * @return ApplicationKeys
   */
  @RequestLine("PUT /applications/{applicationId}/keys/{keyType}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApplicationKeys applicationsApplicationIdKeysKeyTypePut(@Param("applicationId") String applicationId, @Param("keyType") String keyType, ApplicationKeys body);

  /**
   * Map application keys
   * Map keys (Consumer key/secret) to an application 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param body Application key mapping request object  (required)
   * @return ApplicationKeys
   */
  @RequestLine("POST /applications/{applicationId}/map-keys")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApplicationKeys applicationsApplicationIdMapKeysPost(@Param("applicationId") String applicationId, ApplicationKeyMappingRequest body);

  /**
   * Update an application
   * Update application details 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param body Application object that needs to be updated  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return Application
   */
  @RequestLine("PUT /applications/{applicationId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  Application applicationsApplicationIdPut(@Param("applicationId") String applicationId, Application body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Create a new application
   * Create a new application. 
    * @param body Application object that is to be created.  (required)
   * @return Application
   */
  @RequestLine("POST /applications")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Application applicationsPost(Application body);

  /**
   * Export details related to an Application.
   * This operation can be used to export details related to a perticular application. 
    * @param appId Application Search Query  (required)
   * @return File
   */
  @RequestLine("GET /export/applications?appId={appId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/zip",
  })
  File exportApplicationsGet(@Param("appId") String appId);

  /**
   * Export details related to an Application.
   * This operation can be used to export details related to a perticular application. 
   * Note, this is equivalent to the other <code>exportApplicationsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ExportApplicationsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>appId - Application Search Query  (required)</li>
   *   </ul>
   * @return File
   */
  @RequestLine("GET /export/applications?appId={appId}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/zip",
  })
  File exportApplicationsGet(@QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>exportApplicationsGet</code> method in a fluent style.
   */
  public static class ExportApplicationsGetQueryParams extends HashMap<String, Object> {
    public ExportApplicationsGetQueryParams appId(final String value) {
      put("appId", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Imports an Application.
   * This operation can be used to import an existing Application. 
    * @param file Zip archive consisting on exported application configuration  (required)
   * @return Application
   */
  @RequestLine("POST /import/applications")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  Application importApplicationsPost(@Param("file") File file);

  /**
   * Imports an Updates an Application.
   * This operation can be used to import an existing Application. 
    * @param file Zip archive consisting on exported application configuration  (required)
   * @return Application
   */
  @RequestLine("PUT /import/applications")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  Application importApplicationsPut(@Param("file") File file);
}
