package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.RatingList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface RatingCollectionApi extends ApiClient.Api {


  /**
   * Get API ratings
   * Get the rating of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
   * @return RatingList
   */
  @RequestLine("GET /apis/{apiId}/ratings?limit={limit}&offset={offset}")
  @Headers({
    "Accept: application/json",
  })
  RatingList apisApiIdRatingsGet(@Param("apiId") String apiId, @Param("limit") Integer limit, @Param("offset")
          Integer offset);

  /**
   * Get API ratings
   * Get the rating of an API. 
   * Note, this is equivalent to the other <code>apisApiIdRatingsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisApiIdRatingsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return RatingList
   */
  @RequestLine("GET /apis/{apiId}/ratings?limit={limit}&offset={offset}")
  @Headers({
  "Accept: application/json",
  })
  RatingList apisApiIdRatingsGet(@Param("apiId") String apiId, @QueryMap(encoded = true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisApiIdRatingsGet</code> method in a fluent style.
   */
  public static class ApisApiIdRatingsGetQueryParams extends HashMap<String, Object> {
    public ApisApiIdRatingsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApisApiIdRatingsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }
}
