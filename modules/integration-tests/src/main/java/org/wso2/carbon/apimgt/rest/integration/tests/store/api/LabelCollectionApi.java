package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.LabelList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface LabelCollectionApi extends ApiClient.Api {


  /**
   * Get label information based on the label name
   * This operation can be used to retrieve the information of the labels 
    * @param labelType type of the label.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return LabelList
   */
  @RequestLine("GET /labels?labelType={labelType}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  LabelList labelsGet(@Param("labelType") String labelType, @Param("ifNoneMatch") String ifNoneMatch, @Param
          ("ifModifiedSince") String ifModifiedSince);

  /**
   * Get label information based on the label name
   * This operation can be used to retrieve the information of the labels 
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
   *   <li>labelType - type of the label.  (optional)</li>
   *   </ul>
   * @return LabelList
   */
  @RequestLine("GET /labels?labelType={labelType}")
  @Headers({
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}",
      
      "If-Modified-Since: {ifModifiedSince}"
  })
  LabelList labelsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince,
                      @QueryMap(encoded = true) Map<String, Object> queryParams);

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
