package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:41:58.538+05:30")
public interface ImportConfigurationApi extends ApiClient.Api {


  /**
   * Imports API(s).
   * This operation can be used to import one or more existing APIs. 
    * @param file Zip archive consisting on exported api configuration  (required)
    * @param provider If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to.  (optional)
   * @return APIList
   */
  @RequestLine("POST /import/apis?provider={provider}")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  APIList importApisPost(@Param("file") File file, @Param("provider") String provider);

  /**
   * Imports API(s).
   * This operation can be used to import one or more existing APIs. 
    * @param file Zip archive consisting on exported api configuration  (required)
    * @param provider If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to.  (optional)
   * @return APIList
   */
  @RequestLine("PUT /import/apis?provider={provider}")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  APIList importApisPut(@Param("file") File file, @Param("provider") String provider);
}
