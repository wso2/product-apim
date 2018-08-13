package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:41:58.538+05:30")
public interface ExportConfigurationApi extends ApiClient.Api {


  /**
   * Export information related to an API.
   * This operation can be used to export information related to a particular API. 
    * @param query API search query  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
   * @return File
   */
  @RequestLine("GET /export/apis?query={query}&limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/zip",
  })
  File exportApisGet(@Param("query") String query, @Param("limit") Integer limit, @Param("offset") Integer offset);
}
