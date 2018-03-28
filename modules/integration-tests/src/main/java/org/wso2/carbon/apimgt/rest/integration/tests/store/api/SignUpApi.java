package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.User;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface SignUpApi extends ApiClient.Api {


  /**
   * Register a new user
   * User self signup API 
    * @param body User object to represent the new user  (required)
   * @return User
   */
  @RequestLine("POST /self-signup")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  User selfSignupPost(User body);
}
