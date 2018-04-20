package org.wso2.carbon.apimgt.rest.integration.tests.scim.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.UserList;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.UserSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-17T00:32:36.849+05:30")
public interface SearchUsersApi extends ApiClient.Api {


  /**
   * Search users
   * Create a new user 
    * @param body User Search object  (required)
   * @return UserList
   */
  @RequestLine("POST /Users/.search")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  UserList usersSearchPost(UserSearch body);
}
