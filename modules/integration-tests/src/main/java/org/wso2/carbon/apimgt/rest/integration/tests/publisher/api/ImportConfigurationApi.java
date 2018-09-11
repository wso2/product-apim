package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-09-11T19:34:51.739+05:30")
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
   * Note, this is equivalent to the other <code>importApisPost</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ImportApisPostQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param file Zip archive consisting on exported api configuration  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>provider - If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to.  (optional)</li>
   *   </ul>
   * @return APIList
   */
  @RequestLine("POST /import/apis?provider={provider}")
  @Headers({
  "Content-Type: multipart/form-data",
  "Accept: application/json",
  })
  APIList importApisPost(@Param("file") File file, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>importApisPost</code> method in a fluent style.
   */
  public static class ImportApisPostQueryParams extends HashMap<String, Object> {
    public ImportApisPostQueryParams provider(final String value) {
      put("provider", EncodingUtils.encode(value));
      return this;
    }
  }

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

  /**
   * Imports API(s).
   * This operation can be used to import one or more existing APIs. 
   * Note, this is equivalent to the other <code>importApisPut</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ImportApisPutQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param file Zip archive consisting on exported api configuration  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>provider - If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to.  (optional)</li>
   *   </ul>
   * @return APIList
   */
  @RequestLine("PUT /import/apis?provider={provider}")
  @Headers({
  "Content-Type: multipart/form-data",
  "Accept: application/json",
  })
  APIList importApisPut(@Param("file") File file, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>importApisPut</code> method in a fluent style.
   */
  public static class ImportApisPutQueryParams extends HashMap<String, Object> {
    public ImportApisPutQueryParams provider(final String value) {
      put("provider", EncodingUtils.encode(value));
      return this;
    }
  }
}
