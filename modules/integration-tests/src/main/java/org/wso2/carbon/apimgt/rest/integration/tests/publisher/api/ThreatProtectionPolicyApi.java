package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.ThreatProtectionPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:28:03.315+05:30")
public interface ThreatProtectionPolicyApi extends ApiClient.Api {


  /**
   * Get a threat protection policy
   * 
    * @param policyId The UUID of a Policy  (required)
   * @return ThreatProtectionPolicy
   */
  @RequestLine("GET /threat-protection-policies/{policyId}")
  @Headers({
    "Accept: application/json",
  })
  ThreatProtectionPolicy threatProtectionPoliciesPolicyIdGet(@Param("policyId") String policyId);
}
