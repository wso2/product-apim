package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeyGenerateRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeyMappingRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeys;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface ApplicationKeyIndividualApi extends ApiClient.Api {


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
  ApplicationKeys applicationsApplicationIdGenerateKeysPost(@Param("applicationId") String applicationId,
                                                            ApplicationKeyGenerateRequest body);

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
}
