package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.TierList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ThrottlingTierCollectionApi extends ApiClient.Api {


  /**
   * Get all policies
   * This operation can be used to list the available policies for a given policy level. Tier level should be specified as a path parameter and should be one of &#x60;api&#x60;, &#x60;application&#x60; and &#x60;resource&#x60;. 
    * @param tierLevel List API or Application or Resource type policies.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return TierList
   */
  @RequestLine("GET /policies/{tierLevel}?limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  TierList policiesTierLevelGet(@Param("tierLevel") String tierLevel, @Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get all policies
   * This operation can be used to list the available policies for a given policy level. Tier level should be specified as a path parameter and should be one of &#x60;api&#x60;, &#x60;application&#x60; and &#x60;resource&#x60;. 
   * Note, this is equivalent to the other <code>policiesTierLevelGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link PoliciesTierLevelGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param tierLevel List API or Application or Resource type policies.  (required)
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return TierList
   */
  @RequestLine("GET /policies/{tierLevel}?limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  TierList policiesTierLevelGet(@Param("tierLevel") String tierLevel, @Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>policiesTierLevelGet</code> method in a fluent style.
   */
  public static class PoliciesTierLevelGetQueryParams extends HashMap<String, Object> {
    public PoliciesTierLevelGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public PoliciesTierLevelGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }
}
