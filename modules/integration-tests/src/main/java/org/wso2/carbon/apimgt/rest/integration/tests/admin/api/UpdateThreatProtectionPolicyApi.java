package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.ThreatProtectionPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface UpdateThreatProtectionPolicyApi extends ApiClient.Api {


  /**
   * post
   * desc
    * @param threatProtectionPolicyId The UUID of a Policy  (required)
    * @param threatProtectionPolicy Threat protection json policy request parameter  (required)
   */
  @RequestLine("POST /threat-protection-policies/{threatProtectionPolicyId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void threatProtectionPoliciesThreatProtectionPolicyIdPost(@Param("threatProtectionPolicyId") String
                                                                    threatProtectionPolicyId, ThreatProtectionPolicy threatProtectionPolicy);
}
