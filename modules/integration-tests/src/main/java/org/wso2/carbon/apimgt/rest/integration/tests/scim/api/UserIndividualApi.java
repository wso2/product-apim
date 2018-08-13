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
public interface UserIndividualApi extends ApiClient.Api {


  /**
   * delete user
   * Delete a user 
    * @param id Resource Id of User or Group  (required)
   */
  @RequestLine("DELETE /Users/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void usersIdDelete(@Param("id") String id);

  /**
   * Get user
   * Get details of a users 
    * @param id Resource Id of User or Group  (required)
   */
  @RequestLine("GET /Users/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void usersIdGet(@Param("id") String id);

  /**
   * update user
   * Update details of a users 
    * @param id Resource Id of User or Group  (required)
    * @param body User object that needs to be added  (required)
   * @return User
   */
  @RequestLine("PUT /Users/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  User usersIdPut(@Param("id") String id, User body);
}
