package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.ThreatProtectionPolicyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface ThreatProtectionPoliciesApi extends ApiClient.Api {


  /**
   * Get All Threat Protection Policies
   * This can be used to get all defined threat protection policies
   * @return ThreatProtectionPolicyList
   */
  @RequestLine("GET /threat-protection-policies")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ThreatProtectionPolicyList threatProtectionPoliciesGet();
}
