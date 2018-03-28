package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CompositeAPI;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CompositeAPIList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface CompositeApiCollectionApi extends ApiClient.Api {


  /**
   * Retrieve/Search Composite APIs 
   * This operation provides you a list of available Composite APIs qualifying under a given search condition.  Each retrieved Composite API is represented with a minimal amount of attributes. If you want to get complete details of a Composite API, you need to use **Get details of a Composite API** operation. 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param query **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match a Composite API if the provider of the Composite API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match a Composite API if the provider of the Composite API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against Composite API Name.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return CompositeAPIList
   */
  @RequestLine("GET /composite-apis?limit={limit}&offset={offset}&query={query}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  CompositeAPIList compositeApisGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("query")
          String query, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Retrieve/Search Composite APIs 
   * This operation provides you a list of available Composite APIs qualifying under a given search condition.  Each retrieved Composite API is represented with a minimal amount of attributes. If you want to get complete details of a Composite API, you need to use **Get details of a Composite API** operation. 
   * Note, this is equivalent to the other <code>compositeApisGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link CompositeApisGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   <li>query - **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match a Composite API if the provider of the Composite API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match a Composite API if the provider of the Composite API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against Composite API Name.  (optional)</li>
   *   </ul>
   * @return CompositeAPIList
   */
  @RequestLine("GET /composite-apis?limit={limit}&offset={offset}&query={query}")
  @Headers({
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  CompositeAPIList compositeApisGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded = true) Map<String,
          Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>compositeApisGet</code> method in a fluent style.
   */
  public static class CompositeApisGetQueryParams extends HashMap<String, Object> {
    public CompositeApisGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public CompositeApisGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
    public CompositeApisGetQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Create a new API
   * This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 
    * @param body API object that needs to be added  (required)
   * @return CompositeAPI
   */
  @RequestLine("POST /composite-apis")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  CompositeAPI compositeApisPost(CompositeAPI body);
}
