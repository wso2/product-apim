package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.LabelList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface LabelCollectionApi extends ApiClient.Api {


  /**
   * Get all labels
   * This operation can be used to retrieve the list of labels available. 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
    * @param labelType **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (optional)
   * @return LabelList
   */
  @RequestLine("GET /labels?labelType={labelType}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  LabelList labelsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince, @Param("labelType") String labelType);

  /**
   * Get all labels
   * This operation can be used to retrieve the list of labels available. 
   * Note, this is equivalent to the other <code>labelsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link LabelsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>labelType - **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (optional)</li>
   *   </ul>
   * @return LabelList
   */
  @RequestLine("GET /labels?labelType={labelType}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}",
      
      "If-Modified-Since: {ifModifiedSince}"
  })
  LabelList labelsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>labelsGet</code> method in a fluent style.
   */
  public static class LabelsGetQueryParams extends HashMap<String, Object> {
    public LabelsGetQueryParams labelType(final String value) {
      put("labelType", EncodingUtils.encode(value));
      return this;
    }
  }
}
