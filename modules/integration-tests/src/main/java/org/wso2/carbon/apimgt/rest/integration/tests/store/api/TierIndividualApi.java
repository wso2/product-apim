package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Tier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface TierIndividualApi extends ApiClient.Api {


  /**
   * Get a single policy details
   * Get policy details 
    * @param tierName Tier name  (required)
    * @param tierLevel List API or Application type policies.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Tier
   */
  @RequestLine("GET /policies/{tierLevel}/{tierName}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Tier policiesTierLevelTierNameGet(@Param("tierName") String tierName, @Param("tierLevel") String tierLevel, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);
}
