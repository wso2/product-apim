package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface UpdateApi extends ApiClient.Api {


  /**
   * Update an API comment
   * Update a certain Comment 
    * @param commentId Comment Id  (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Comment object that needs to be updated  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return Comment
   */
  @RequestLine("PUT /apis/{apiId}/comments/{commentId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  Comment apisApiIdCommentsCommentIdPut(@Param("commentId") String commentId, @Param("apiId") String apiId, Comment
          body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Update an application
   * Update application details 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param body Application object that needs to be updated  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return Application
   */
  @RequestLine("PUT /applications/{applicationId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  Application applicationsApplicationIdPut(@Param("applicationId") String applicationId, Application body, @Param
          ("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
