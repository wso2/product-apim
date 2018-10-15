package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndPoint;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndPointList;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface EndpointCollectionApi extends ApiClient.Api {


  /**
   * Get all endpoints
   * This operation can be used to retrieve the list of endpoints available. 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return EndPointList
   */
  @RequestLine("GET /endpoints")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  EndPointList endpointsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Check given Endpoint is already exist 
   * Using this operation, you can check a given Endpoint name is already used. You need to provide the name you want to check. 
    * @param name  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   */
  @RequestLine("HEAD /endpoints?name={name}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  void endpointsHead(@Param("name") String name, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Check given Endpoint is already exist 
   * Using this operation, you can check a given Endpoint name is already used. You need to provide the name you want to check. 
   * Note, this is equivalent to the other <code>endpointsHead</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link EndpointsHeadQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>name -  (optional)</li>
   *   </ul>
   */
  @RequestLine("HEAD /endpoints?name={name}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  void endpointsHead(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>endpointsHead</code> method in a fluent style.
   */
  public static class EndpointsHeadQueryParams extends HashMap<String, Object> {
    public EndpointsHeadQueryParams name(final String value) {
      put("name", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Add a new endpoint
   * This operation can be used to add a new endpoint. 
    * @param body EndPoint object that needs to be added  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return EndPoint
   */
  @RequestLine("POST /endpoints")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  EndPoint endpointsPost(EndPoint body, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);
}
