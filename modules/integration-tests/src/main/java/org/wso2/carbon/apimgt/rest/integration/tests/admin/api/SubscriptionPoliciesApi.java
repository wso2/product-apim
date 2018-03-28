package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.SubscriptionThrottlePolicy;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.SubscriptionThrottlePolicyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface SubscriptionPoliciesApi extends ApiClient.Api {


  /**
   * Get all Subscription level throttle policies
   * Get all Subscription level throttle policies 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return SubscriptionThrottlePolicyList
   */
  @RequestLine("GET /policies/throttling/subscription")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  SubscriptionThrottlePolicyList policiesThrottlingSubscriptionGet(@Param("ifNoneMatch") String ifNoneMatch, @Param
          ("ifModifiedSince") String ifModifiedSince);

  /**
   * Delete a Subscription level throttle policy
   * Delete a Subscription level throttle policy 
    * @param id Thorttle policy UUID  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /policies/throttling/subscription/{id}")
  @Headers({
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void policiesThrottlingSubscriptionIdDelete(@Param("id") String id, @Param("ifMatch") String ifMatch, @Param
          ("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Retrieve a Subscription Policy
   * Retrieve a Subscription Policy providing the policy name. 
    * @param id Thorttle policy UUID  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return SubscriptionThrottlePolicy
   */
  @RequestLine("GET /policies/throttling/subscription/{id}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  SubscriptionThrottlePolicy policiesThrottlingSubscriptionIdGet(@Param("id") String id, @Param("ifNoneMatch") String
          ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update a Subscription level throttle policy
   * Update a Subscription level throttle policy 
    * @param id Thorttle policy UUID  (required)
    * @param body Policy object that needs to be modified  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return SubscriptionThrottlePolicy
   */
  @RequestLine("PUT /policies/throttling/subscription/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  SubscriptionThrottlePolicy policiesThrottlingSubscriptionIdPut(@Param("id") String id, SubscriptionThrottlePolicy
          body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Add a Subscription level throttle policy
   * Add a Subscription level throttle policy 
    * @param body Subscripion level policy object that should to be added  (required)
   * @return SubscriptionThrottlePolicy
   */
  @RequestLine("POST /policies/throttling/subscription")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  SubscriptionThrottlePolicy policiesThrottlingSubscriptionPost(SubscriptionThrottlePolicy body);
}
