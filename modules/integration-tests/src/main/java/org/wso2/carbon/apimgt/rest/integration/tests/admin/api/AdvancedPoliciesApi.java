package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.AdvancedThrottlePolicy;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.AdvancedThrottlePolicyList;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface AdvancedPoliciesApi extends ApiClient.Api {


  /**
   * Get all Advanced level throttle policies
   * Get all Advanced level throttle policies 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return AdvancedThrottlePolicyList
   */
  @RequestLine("GET /policies/throttling/advanced")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  AdvancedThrottlePolicyList policiesThrottlingAdvancedGet(@Param("ifNoneMatch") String ifNoneMatch, @Param
          ("ifModifiedSince") String ifModifiedSince);

  /**
   * Delete an Advanced level throttle policy
   * Delete an Advanced level throttle policy 
    * @param id Thorttle policy UUID  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /policies/throttling/advanced/{id}")
  @Headers({
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void policiesThrottlingAdvancedIdDelete(@Param("id") String id, @Param("ifMatch") String ifMatch, @Param
          ("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Retrieve an Advanced Policy
   * Retrieve a Advanced Policy providing the policy name. 
    * @param id Thorttle policy UUID  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return AdvancedThrottlePolicy
   */
  @RequestLine("GET /policies/throttling/advanced/{id}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  AdvancedThrottlePolicy policiesThrottlingAdvancedIdGet(@Param("id") String id, @Param("ifNoneMatch") String
          ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update an Advanced level throttle policy
   * Update an Advanced level throttle policy 
    * @param id Thorttle policy UUID  (required)
    * @param body Policy object that needs to be modified  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return AdvancedThrottlePolicy
   */
  @RequestLine("PUT /policies/throttling/advanced/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  AdvancedThrottlePolicy policiesThrottlingAdvancedIdPut(@Param("id") String id, AdvancedThrottlePolicy body, @Param
          ("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Add an Advanced level throttle policy
   * Add an Advanced level throttle policy 
    * @param body Advanced level policy object that should to be added  (required)
   * @return AdvancedThrottlePolicy
   */
  @RequestLine("POST /policies/throttling/advanced")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  AdvancedThrottlePolicy policiesThrottlingAdvancedPost(AdvancedThrottlePolicy body);
}
