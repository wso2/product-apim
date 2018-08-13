package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.CustomRule;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.CustomRuleList;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:59:16.844+05:30")
public interface CustomRulesApi extends ApiClient.Api {


  /**
   * Get all Custom Rules
   * Get all Custom Rules 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return CustomRuleList
   */
  @RequestLine("GET /policies/throttling/custom")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  CustomRuleList policiesThrottlingCustomGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Add a Custom Rule
   * Add a Custom Rule 
    * @param body Custom Rule object that should to be added  (required)
   * @return CustomRule
   */
  @RequestLine("POST /policies/throttling/custom")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  CustomRule policiesThrottlingCustomPost(CustomRule body);

  /**
   * Delete a Custom Rule
   * Delete a Custom Rule 
    * @param ruleId Custom rule UUID  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /policies/throttling/custom/{ruleId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void policiesThrottlingCustomRuleIdDelete(@Param("ruleId") String ruleId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Retrieve a Custom Rule
   * Retrieve a Custom Rule providing the policy name. 
    * @param ruleId Custom rule UUID  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return CustomRule
   */
  @RequestLine("GET /policies/throttling/custom/{ruleId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  CustomRule policiesThrottlingCustomRuleIdGet(@Param("ruleId") String ruleId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update a Custom Rule
   * Update a Custom Rule 
    * @param ruleId Custom rule UUID  (required)
    * @param body Policy object that needs to be modified  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return CustomRule
   */
  @RequestLine("PUT /policies/throttling/custom/{ruleId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  CustomRule policiesThrottlingCustomRuleIdPut(@Param("ruleId") String ruleId, CustomRule body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
