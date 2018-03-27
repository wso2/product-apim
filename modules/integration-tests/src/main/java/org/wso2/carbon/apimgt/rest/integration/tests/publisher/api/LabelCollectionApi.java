package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.LabelList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:28:03.315+05:30")
public interface LabelCollectionApi extends ApiClient.Api {


  /**
   * Get all labels
   * This operation can be used to retrieve the list of labels available. 
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return LabelList
   */
  @RequestLine("GET /labels")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  LabelList labelsGet(@Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);
}
