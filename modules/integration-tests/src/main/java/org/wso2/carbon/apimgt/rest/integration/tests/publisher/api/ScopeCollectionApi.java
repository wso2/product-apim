package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Scope;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.ScopeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:28:03.315+05:30")
public interface ScopeCollectionApi extends ApiClient.Api {


  /**
   * Get a list of scopes of an API
   * This operation can be used to retrieve a list of scopes belonging to an API by providing the id of the API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return ScopeList
   */
  @RequestLine("GET /apis/{apiId}/scopes")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  ScopeList apisApiIdScopesGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Add a new scope to an API
   * This operation can be used to add a new scope to an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Scope object that needs to be added  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return Scope
   */
  @RequestLine("POST /apis/{apiId}/scopes")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  Scope apisApiIdScopesPost(@Param("apiId") String apiId, Scope body, @Param("ifMatch") String ifMatch, @Param
          ("ifUnmodifiedSince") String ifUnmodifiedSince);
}
