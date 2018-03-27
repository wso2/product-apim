package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.TierList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface TierCollectionApi extends ApiClient.Api {


  /**
   * Get all available policies
   * Get available policies 
    * @param tierLevel List API or Application type policies.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return List&lt;TierList&gt;
   */
  @RequestLine("GET /policies/{tierLevel}?limit={limit}&offset={offset}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  List<TierList> policiesTierLevelGet(@Param("tierLevel") String tierLevel, @Param("limit") Integer limit, @Param
          ("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get all available policies
   * Get available policies 
   * Note, this is equivalent to the other <code>policiesTierLevelGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link PoliciesTierLevelGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param tierLevel List API or Application type policies.  (required)
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return List&lt;TierList&gt;
   */
  @RequestLine("GET /policies/{tierLevel}?limit={limit}&offset={offset}")
  @Headers({
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  List<TierList> policiesTierLevelGet(@Param("tierLevel") String tierLevel, @Param("ifNoneMatch") String ifNoneMatch,
                                      @QueryMap(encoded = true) Map<String, Object> queryParams);

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
