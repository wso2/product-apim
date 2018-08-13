package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.ApplicationThrottlePolicy;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.ApplicationThrottlePolicyList;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:59:16.844+05:30")
public interface ApplicationPoliciesApi extends ApiClient.Api {


  /**
   * Get all Application level throttle policies
   * Get all Application level throttle policies 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return ApplicationThrottlePolicyList
   */
  @RequestLine("GET /policies/throttling/application")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  ApplicationThrottlePolicyList policiesThrottlingApplicationGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Delete an Application level throttle policy
   * Delete an Application level throttle policy 
    * @param id Thorttle policy UUID  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /policies/throttling/application/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void policiesThrottlingApplicationIdDelete(@Param("id") String id, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Retrieve an Application Policy
   * Retrieve an Application Policy providing the policy name. 
    * @param id Thorttle policy UUID  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return ApplicationThrottlePolicy
   */
  @RequestLine("GET /policies/throttling/application/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  ApplicationThrottlePolicy policiesThrottlingApplicationIdGet(@Param("id") String id, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update an Application level throttle policy
   * Update an Application level throttle policy 
    * @param id Thorttle policy UUID  (required)
    * @param body Policy object that needs to be modified  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return ApplicationThrottlePolicy
   */
  @RequestLine("PUT /policies/throttling/application/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  ApplicationThrottlePolicy policiesThrottlingApplicationIdPut(@Param("id") String id, ApplicationThrottlePolicy body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Add an Application level throttle policy
   * Add an Application level throttle policy 
    * @param body Application level policy object that should to be added  (required)
   * @return ApplicationThrottlePolicy
   */
  @RequestLine("POST /policies/throttling/application")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApplicationThrottlePolicy policiesThrottlingApplicationPost(ApplicationThrottlePolicy body);
}
