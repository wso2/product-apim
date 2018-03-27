package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface ApiCollectionApi extends ApiClient.Api {


  /**
   * Retrieve/Search APIs 
   * Get a list of available APIs qualifying under a given search condition. 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param labels Comma seperated store labels  (optional)
    * @param query **Search condition**.  You can search in attributes by using an **\&quot;attribute:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider, tag **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return APIList
   */
  @RequestLine("GET /apis?limit={limit}&offset={offset}&labels={labels}&query={query}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  APIList apisGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("labels") String labels,
                  @Param("query") String query, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Retrieve/Search APIs 
   * Get a list of available APIs qualifying under a given search condition. 
   * Note, this is equivalent to the other <code>apisGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   <li>labels - Comma seperated store labels  (optional)</li>
   *   <li>query - **Search condition**.  You can search in attributes by using an **\&quot;attribute:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider, tag **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)</li>
   *   </ul>
   * @return APIList
   */
  @RequestLine("GET /apis?limit={limit}&offset={offset}&labels={labels}&query={query}")
  @Headers({
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  APIList apisGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded = true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisGet</code> method in a fluent style.
   */
  public static class ApisGetQueryParams extends HashMap<String, Object> {
    public ApisGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApisGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
    public ApisGetQueryParams labels(final String value) {
      put("labels", EncodingUtils.encode(value));
      return this;
    }
    public ApisGetQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
  }
}
