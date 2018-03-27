package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Subscription;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface SubscriptionIndividualApi extends ApiClient.Api {


  /**
   * Add a new subscription
   * Add a new subscription 
    * @param body Subscription object that should to be added  (required)
   * @return Subscription
   */
  @RequestLine("POST /subscriptions")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Subscription subscriptionsPost(Subscription body);

  /**
   * Remove a subscription
   * Remove subscription 
    * @param subscriptionId Subscription Id  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /subscriptions/{subscriptionId}")
  @Headers({
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void subscriptionsSubscriptionIdDelete(@Param("subscriptionId") String subscriptionId, @Param("ifMatch") String
          ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get details of a subscription
   * Get subscription details 
    * @param subscriptionId Subscription Id  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Subscription
   */
  @RequestLine("GET /subscriptions/{subscriptionId}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Subscription subscriptionsSubscriptionIdGet(@Param("subscriptionId") String subscriptionId, @Param("ifNoneMatch")
          String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);
}
