package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ApplicationCollectionApi extends ApiClient.Api {


  /**
   * Get all applications
   * Get a list of applications 
    * @param query **Search condition**.  You can search for an application by specifying the name as \&quot;query\&quot; attribute.  Eg. \&quot;app1\&quot; will match an application if the name is exactly \&quot;app1\&quot;.  Currently this does not support wildcards. Given name must exactly match the application name.  (optional)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return ApplicationList
   */
  @RequestLine("GET /applications?query={query}&limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  ApplicationList applicationsGet(@Param("query") String query, @Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get all applications
   * Get a list of applications 
   * Note, this is equivalent to the other <code>applicationsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApplicationsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>query - **Search condition**.  You can search for an application by specifying the name as \&quot;query\&quot; attribute.  Eg. \&quot;app1\&quot; will match an application if the name is exactly \&quot;app1\&quot;.  Currently this does not support wildcards. Given name must exactly match the application name.  (optional)</li>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return ApplicationList
   */
  @RequestLine("GET /applications?query={query}&limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  ApplicationList applicationsGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>applicationsGet</code> method in a fluent style.
   */
  public static class ApplicationsGetQueryParams extends HashMap<String, Object> {
    public ApplicationsGetQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
    public ApplicationsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApplicationsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }
}
