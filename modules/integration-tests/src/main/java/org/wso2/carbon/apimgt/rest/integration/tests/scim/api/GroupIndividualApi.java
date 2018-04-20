package org.wso2.carbon.apimgt.rest.integration.tests.scim.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-17T00:32:36.849+05:30")
public interface GroupIndividualApi extends ApiClient.Api {


  /**
   * delete group
   * Delete a group 
    * @param id Resource Id of User or Group  (required)
   */
  @RequestLine("DELETE /Groups/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void groupsIdDelete(@Param("id") String id);

  /**
   * Get group
   * Get details of a group 
    * @param id Resource Id of User or Group  (required)
   * @return Group
   */
  @RequestLine("GET /Groups/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Group groupsIdGet(@Param("id") String id);

  /**
   * update group
   * Update details of a group 
    * @param id Resource Id of User or Group  (required)
    * @param body Group object that needs to be added  (required)
   * @return Group
   */
  @RequestLine("PUT /Groups/{id}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Group groupsIdPut(@Param("id") String id, Group body);
}
