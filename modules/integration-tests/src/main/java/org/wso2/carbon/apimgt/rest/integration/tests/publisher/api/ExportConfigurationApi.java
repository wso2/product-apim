package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ExportConfigurationApi extends ApiClient.Api {


  /**
   * Export information related to an API.
   * This operation can be used to export information related to a particular API. 
    * @param query API search query  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
   * @return File
   */
  @RequestLine("GET /export/apis?query={query}&limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/zip",
  })
  File exportApisGet(@Param("query") String query, @Param("limit") Integer limit, @Param("offset") Integer offset);

  /**
   * Export information related to an API.
   * This operation can be used to export information related to a particular API. 
   * Note, this is equivalent to the other <code>exportApisGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ExportApisGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>query - API search query  (required)</li>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return File
   */
  @RequestLine("GET /export/apis?query={query}&limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/zip",
  })
  File exportApisGet(@QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>exportApisGet</code> method in a fluent style.
   */
  public static class ExportApisGetQueryParams extends HashMap<String, Object> {
    public ExportApisGetQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
    public ExportApisGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ExportApisGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }
}
