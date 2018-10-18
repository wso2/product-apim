package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.User;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


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
