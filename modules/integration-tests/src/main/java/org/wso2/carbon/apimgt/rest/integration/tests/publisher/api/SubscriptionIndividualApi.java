package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-09-11T19:34:51.739+05:30")
public interface SubscriptionIndividualApi extends ApiClient.Api {


  /**
   * Block a subscription
   * This operation can be used to block a subscription. Along with the request, &#x60;blockState&#x60; must be specified as a query parameter.  1. &#x60;BLOCKED&#x60; : Subscription is completely blocked for both Production and Sandbox environments. 2. &#x60;PROD_ONLY_BLOCKED&#x60; : Subscription is blocked for Production environment only. 
    * @param subscriptionId Subscription Id  (required)
    * @param blockState Subscription block state.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("POST /subscriptions/block-subscription?subscriptionId={subscriptionId}&blockState={blockState}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void subscriptionsBlockSubscriptionPost(@Param("subscriptionId") String subscriptionId, @Param("blockState") String blockState, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Block a subscription
   * This operation can be used to block a subscription. Along with the request, &#x60;blockState&#x60; must be specified as a query parameter.  1. &#x60;BLOCKED&#x60; : Subscription is completely blocked for both Production and Sandbox environments. 2. &#x60;PROD_ONLY_BLOCKED&#x60; : Subscription is blocked for Production environment only. 
   * Note, this is equivalent to the other <code>subscriptionsBlockSubscriptionPost</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link SubscriptionsBlockSubscriptionPostQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
   * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>subscriptionId - Subscription Id  (required)</li>
   *   <li>blockState - Subscription block state.  (required)</li>
   *   </ul>
   */
  @RequestLine("POST /subscriptions/block-subscription?subscriptionId={subscriptionId}&blockState={blockState}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-Match: {ifMatch}",
      
      "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void subscriptionsBlockSubscriptionPost(@Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>subscriptionsBlockSubscriptionPost</code> method in a fluent style.
   */
  public static class SubscriptionsBlockSubscriptionPostQueryParams extends HashMap<String, Object> {
    public SubscriptionsBlockSubscriptionPostQueryParams subscriptionId(final String value) {
      put("subscriptionId", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsBlockSubscriptionPostQueryParams blockState(final String value) {
      put("blockState", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get details of a subscription
   * This operation can be used to get details of a single subscription. 
    * @param subscriptionId Subscription Id  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Subscription
   */
  @RequestLine("GET /subscriptions/{subscriptionId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Subscription subscriptionsSubscriptionIdGet(@Param("subscriptionId") String subscriptionId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Unblock a Subscription
   * This operation can be used to unblock a subscription specifying the subscription Id. The subscription will be fully unblocked after performing this operation. 
    * @param subscriptionId Subscription Id  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("POST /subscriptions/unblock-subscription?subscriptionId={subscriptionId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void subscriptionsUnblockSubscriptionPost(@Param("subscriptionId") String subscriptionId, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Unblock a Subscription
   * This operation can be used to unblock a subscription specifying the subscription Id. The subscription will be fully unblocked after performing this operation. 
   * Note, this is equivalent to the other <code>subscriptionsUnblockSubscriptionPost</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link SubscriptionsUnblockSubscriptionPostQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
   * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>subscriptionId - Subscription Id  (required)</li>
   *   </ul>
   */
  @RequestLine("POST /subscriptions/unblock-subscription?subscriptionId={subscriptionId}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-Match: {ifMatch}",
      
      "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void subscriptionsUnblockSubscriptionPost(@Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>subscriptionsUnblockSubscriptionPost</code> method in a fluent style.
   */
  public static class SubscriptionsUnblockSubscriptionPostQueryParams extends HashMap<String, Object> {
    public SubscriptionsUnblockSubscriptionPostQueryParams subscriptionId(final String value) {
      put("subscriptionId", EncodingUtils.encode(value));
      return this;
    }
  }
}
