package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:59:16.844+05:30")
public interface LabelApi extends ApiClient.Api {


  /**
   * Delete a Label
   * Delete a Label by label Id 
    * @param labelId Label identifier  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /labels/{labelId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void labelsLabelIdDelete(@Param("labelId") String labelId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Retrieve a Label
   * Retrieve a Label for labelId 
    * @param labelId Label identifier  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Label
   */
  @RequestLine("GET /labels/{labelId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Label labelsLabelIdGet(@Param("labelId") String labelId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update a Label
   * Update a Label by label Id 
    * @param labelId Label identifier  (required)
    * @param body Label object with updated information  (required)
   * @return Label
   */
  @RequestLine("PUT /labels/{labelId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Label labelsLabelIdPut(@Param("labelId") String labelId, Label body);
}
