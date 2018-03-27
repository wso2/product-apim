package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Rating;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Subscription;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface CreateApi extends ApiClient.Api {


  /**
   * Add an API comment
   * 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Comment object that should to be added  (required)
   * @return Comment
   */
  @RequestLine("POST /apis/{apiId}/comments")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Comment apisApiIdCommentsPost(@Param("apiId") String apiId, Comment body);

  /**
   * Add or update logged in user&#39;s rating for an API
   * Adds or updates a rating 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Rating object that should to be added  (required)
   * @return Rating
   */
  @RequestLine("PUT /apis/{apiId}/user-rating")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Rating apisApiIdUserRatingPut(@Param("apiId") String apiId, Rating body);

  /**
   * Create a new application
   * Create a new application. 
    * @param body Application object that is to be created.  (required)
   * @return Application
   */
  @RequestLine("POST /applications")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Application applicationsPost(Application body);

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
}
