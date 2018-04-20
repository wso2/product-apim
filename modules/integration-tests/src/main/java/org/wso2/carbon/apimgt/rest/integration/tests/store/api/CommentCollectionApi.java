package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CommentList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:42:47.879+05:30")
public interface CommentCollectionApi extends ApiClient.Api {


  /**
   * Retrieve API comments
   * Get a list of Comments that are already added to APIs 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
   * @return CommentList
   */
  @RequestLine("GET /apis/{apiId}/comments?limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  CommentList apisApiIdCommentsGet(@Param("apiId") String apiId, @Param("limit") Integer limit, @Param("offset") Integer offset);
}
