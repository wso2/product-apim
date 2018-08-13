package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.SubscriptionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:42:47.879+05:30")
public interface SubscriptionCollectionApi extends ApiClient.Api {


  /**
   * Get all subscriptions
   * Get subscription list. The API Identifier or Application Identifier the subscriptions of which are to be returned are passed as parameters. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param apiType **API Type** filters information pertaining to a specific type of API  (optional)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return SubscriptionList
   */
  @RequestLine("GET /subscriptions?apiId={apiId}&applicationId={applicationId}&apiType={apiType}&offset={offset}&limit={limit}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  SubscriptionList subscriptionsGet(@Param("apiId") String apiId, @Param("applicationId") String applicationId, @Param("apiType") String apiType, @Param("offset") Integer offset, @Param("limit") Integer limit, @Param("ifNoneMatch") String ifNoneMatch);
}
