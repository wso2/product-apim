package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.ThreatProtectionPolicyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:41:58.538+05:30")
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
