package org.wso2.carbon.apimgt.rest.integration.tests.scim.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-17T00:32:36.849+05:30")
public interface MeApi extends ApiClient.Api {


  /**
   * Retrieve my user details.
   * Retrieve details of the currently authenticated user. 
   * @return User
   */
  @RequestLine("GET /Me")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  User meGet();
}
