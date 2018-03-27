package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.WorkflowList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface WorkflowsCollectionApi extends ApiClient.Api {


  /**
   * Get all the uncompleted Workflows
   * Get all uncompleted workflows entries 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
    * @param workflowType Type of the worklfow  (optional)
   * @return WorkflowList
   */
  @RequestLine("GET /workflows?workflowType={workflowType}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  WorkflowList workflowsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String
          ifModifiedSince, @Param("workflowType") String workflowType);

  /**
   * Get all the uncompleted Workflows
   * Get all uncompleted workflows entries 
   * Note, this is equivalent to the other <code>workflowsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link WorkflowsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>workflowType - Type of the worklfow  (optional)</li>
   *   </ul>
   * @return WorkflowList
   */
  @RequestLine("GET /workflows?workflowType={workflowType}")
  @Headers({
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}",
      
      "If-Modified-Since: {ifModifiedSince}"
  })
  WorkflowList workflowsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String
          ifModifiedSince, @QueryMap(encoded = true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>workflowsGet</code> method in a fluent style.
   */
  public static class WorkflowsGetQueryParams extends HashMap<String, Object> {
    public WorkflowsGetQueryParams workflowType(final String value) {
      put("workflowType", EncodingUtils.encode(value));
      return this;
    }
  }
}
