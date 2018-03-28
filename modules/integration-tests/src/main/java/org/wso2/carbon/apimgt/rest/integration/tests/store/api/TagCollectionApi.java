package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.TagList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface TagCollectionApi extends ApiClient.Api {


  /**
   * Get all tags
   * Get a list of tags that are already added to APIs 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return TagList
   */
  @RequestLine("GET /tags?limit={limit}&offset={offset}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  TagList tagsGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String
          ifNoneMatch);

  /**
   * Get all tags
   * Get a list of tags that are already added to APIs 
   * Note, this is equivalent to the other <code>tagsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link TagsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return TagList
   */
  @RequestLine("GET /tags?limit={limit}&offset={offset}")
  @Headers({
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  TagList tagsGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded = true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>tagsGet</code> method in a fluent style.
   */
  public static class TagsGetQueryParams extends HashMap<String, Object> {
    public TagsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public TagsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }
}
