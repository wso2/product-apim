package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.BlockingCondition;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:59:16.844+05:30")
public interface BlacklistConditionApi extends ApiClient.Api {


  /**
   * Update a blacklist condition
   * Update a blacklist condition 
    * @param conditionId Blocking condition identifier  (required)
    * @param body Blacklist condition object that needs to be modified  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return BlockingCondition
   */
  @RequestLine("PUT /blacklist/{conditionId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  BlockingCondition blacklistConditionIdPut(@Param("conditionId") String conditionId, BlockingCondition body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
