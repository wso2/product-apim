package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


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
}
