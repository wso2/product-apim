package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.ThreatProtectionPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:59:16.844+05:30")
public interface ThreatProtectionPolicyApi extends ApiClient.Api {


  /**
   * Get one threat protection policy
   * Get one threat protection policy
    * @param threatProtectionPolicyId The UUID of a Policy  (required)
   * @return ThreatProtectionPolicy
   */
  @RequestLine("GET /threat-protection-policies/{threatProtectionPolicyId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ThreatProtectionPolicy threatProtectionPoliciesThreatProtectionPolicyIdGet(@Param("threatProtectionPolicyId") String threatProtectionPolicyId);
}
