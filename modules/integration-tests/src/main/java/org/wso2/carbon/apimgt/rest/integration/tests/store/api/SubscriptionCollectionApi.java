package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.SubscriptionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


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

  /**
   * Get all subscriptions
   * Get subscription list. The API Identifier or Application Identifier the subscriptions of which are to be returned are passed as parameters. 
   * Note, this is equivalent to the other <code>subscriptionsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link SubscriptionsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>apiId - **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)</li>
   *   <li>applicationId - **Application Identifier** consisting of the UUID of the Application.  (required)</li>
   *   <li>apiType - **API Type** filters information pertaining to a specific type of API  (optional)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   </ul>
   * @return SubscriptionList
   */
  @RequestLine("GET /subscriptions?apiId={apiId}&applicationId={applicationId}&apiType={apiType}&offset={offset}&limit={limit}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  SubscriptionList subscriptionsGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>subscriptionsGet</code> method in a fluent style.
   */
  public static class SubscriptionsGetQueryParams extends HashMap<String, Object> {
    public SubscriptionsGetQueryParams apiId(final String value) {
      put("apiId", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams applicationId(final String value) {
      put("applicationId", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams apiType(final String value) {
      put("apiType", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
  }
}
