package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeys;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeysList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface ApplicationKeyCollectionApi extends ApiClient.Api {


  /**
   * Retrieve all application keys
   * Retrieve keys (Consumer key/secret) of application 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
   * @return ApplicationKeysList
   */
  @RequestLine("GET /applications/{applicationId}/keys")
  @Headers({
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
    "Accept: application/json",
  })
  ApplicationKeys applicationsApplicationIdKeysKeyTypeGet(@Param("applicationId") String applicationId, @Param
          ("keyType") String keyType);
}
