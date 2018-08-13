package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.BlockingCondition;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.BlockingConditionList;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:59:16.844+05:30")
public interface BlacklistApi extends ApiClient.Api {


  /**
   * Delete a Blocking condition
   * Delete a Blocking condition 
    * @param conditionId Blocking condition identifier  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /blacklist/{conditionId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void blacklistConditionIdDelete(@Param("conditionId") String conditionId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Retrieve a Blocking Condition
   * Retrieve a Blocking Condition providing the condition Id 
    * @param conditionId Blocking condition identifier  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return BlockingCondition
   */
  @RequestLine("GET /blacklist/{conditionId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  BlockingCondition blacklistConditionIdGet(@Param("conditionId") String conditionId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get all blocking condtions
   * Get all blocking condtions 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return BlockingConditionList
   */
  @RequestLine("GET /blacklist")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  BlockingConditionList blacklistGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Add a Blocking condition
   * Add a Blocking condition 
    * @param body Blocking condition object that should to be added  (required)
   * @return BlockingCondition
   */
  @RequestLine("POST /blacklist")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  BlockingCondition blacklistPost(BlockingCondition body);
}
