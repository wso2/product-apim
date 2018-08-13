package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:42:47.879+05:30")
public interface SDKLanguagesApi extends ApiClient.Api {


  /**
   * Get a list of supported SDK languages 
   * This operation will provide a list of programming languages that are supported by the swagger codegen library for generating System Development Kits (SDKs) for APIs available in the API Manager Store 
   */
  @RequestLine("GET /sdk-gen/languages")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void sdkGenLanguagesGet();
}
