package org.wso2.carbon.apimgt.rest.integration.tests.scim.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Group;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.GroupList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-17T00:32:36.849+05:30")
public interface GroupsApi extends ApiClient.Api {


  /**
   * Retrieve groups
   * Retrieve list of available groups qualifying under a given filter condition 
    * @param startIndex The index of the first element in the result.  (optional, default to 0)
    * @param count Number of elements returned in the paginated result.  (optional, default to 25)
    * @param filter A filter expression to request a subset of the result.  (optional)
   * @return GroupList
   */
  @RequestLine("GET /Groups?startIndex={startIndex}&count={count}&filter={filter}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  GroupList groupsGet(@Param("startIndex") Integer startIndex, @Param("count") Integer count, @Param("filter") String filter);

  /**
   * Retrieve groups
   * Retrieve list of available groups qualifying under a given filter condition 
   * Note, this is equivalent to the other <code>groupsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link GroupsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>startIndex - The index of the first element in the result.  (optional, default to 0)</li>
   *   <li>count - Number of elements returned in the paginated result.  (optional, default to 25)</li>
   *   <li>filter - A filter expression to request a subset of the result.  (optional)</li>
   *   </ul>
   * @return GroupList
   */
  @RequestLine("GET /Groups?startIndex={startIndex}&count={count}&filter={filter}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
  })
  GroupList groupsGet(@QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>groupsGet</code> method in a fluent style.
   */
  public static class GroupsGetQueryParams extends HashMap<String, Object> {
    public GroupsGetQueryParams startIndex(final Integer value) {
      put("startIndex", EncodingUtils.encode(value));
      return this;
    }
    public GroupsGetQueryParams count(final Integer value) {
      put("count", EncodingUtils.encode(value));
      return this;
    }
    public GroupsGetQueryParams filter(final String value) {
      put("filter", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Create a group
   * Create a new group 
    * @param body Group object that needs to be added  (required)
   * @return Group
   */
  @RequestLine("POST /Groups")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Group groupsPost(Group body);
}
