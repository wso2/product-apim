package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationToken;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationTokenGenerateRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ApplicationTokenIndividualApi extends ApiClient.Api {


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
}
