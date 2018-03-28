package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Label;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.LabelList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface LabelCollectionApi extends ApiClient.Api {


  /**
   * Get all registered Labels
   * Get all registered Labels 
   * @return LabelList
   */
  @RequestLine("GET /labels")
  @Headers({
    "Accept: application/json",
  })
  LabelList labelsGet();

  /**
   * Add a Label
   * Add a new gateway/store Label 
    * @param body Label object that should to be added  (required)
   * @return Label
   */
  @RequestLine("POST /labels")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Label labelsPost(Label body);
}
