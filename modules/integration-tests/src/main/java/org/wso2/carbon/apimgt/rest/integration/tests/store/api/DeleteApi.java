package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface DeleteApi extends ApiClient.Api {


  /**
   * Delete an API comment
   * Remove a Comment 
    * @param commentId Comment Id  (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /apis/{apiId}/comments/{commentId}")
  @Headers({
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void apisApiIdCommentsCommentIdDelete(@Param("commentId") String commentId, @Param("apiId") String apiId, @Param
          ("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Delete an application
   * Delete an application 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /applications/{applicationId}")
  @Headers({
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void applicationsApplicationIdDelete(@Param("applicationId") String applicationId, @Param("ifMatch") String
          ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

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
}
