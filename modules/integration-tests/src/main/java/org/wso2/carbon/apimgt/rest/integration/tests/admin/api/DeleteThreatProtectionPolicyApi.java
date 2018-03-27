package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface DeleteThreatProtectionPolicyApi extends ApiClient.Api {


  /**
   * Delete a threat protection policy
   * Delete a threat protection policy
    * @param threatProtectionPolicyId The UUID of a Policy  (required)
   */
  @RequestLine("DELETE /threat-protection-policies/{threatProtectionPolicyId}")
  @Headers({
    "Accept: application/json",
  })
  void threatProtectionPoliciesThreatProtectionPolicyIdDelete(@Param("threatProtectionPolicyId") String
                                                                      threatProtectionPolicyId);
}
