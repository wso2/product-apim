package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.ThreatProtectionPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ThreatProtectionPolicyApi extends ApiClient.Api {


  /**
   * Get a threat protection policy
   * 
    * @param policyId The UUID of a Policy  (required)
   * @return ThreatProtectionPolicy
   */
  @RequestLine("GET /threat-protection-policies/{policyId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ThreatProtectionPolicy threatProtectionPoliciesPolicyIdGet(@Param("policyId") String policyId);
}
