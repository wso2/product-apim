package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIDefinitionValidationResponse;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-09-11T19:34:51.739+05:30")
public interface APICollectionApi extends ApiClient.Api {


  /**
   * Retrieve/Search APIs 
   * This operation provides you a list of available APIs qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API** operation. 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param query **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return APIList
   */
  @RequestLine("GET /apis?limit={limit}&offset={offset}&query={query}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  APIList apisGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("query") String query, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Retrieve/Search APIs 
   * This operation provides you a list of available APIs qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API** operation. 
   * Note, this is equivalent to the other <code>apisGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   <li>query - **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)</li>
   *   </ul>
   * @return APIList
   */
  @RequestLine("GET /apis?limit={limit}&offset={offset}&query={query}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  APIList apisGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisGet</code> method in a fluent style.
   */
  public static class ApisGetQueryParams extends HashMap<String, Object> {
    public ApisGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApisGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
    public ApisGetQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Check given API attibute name is already exist 
   * Using this operation, you can check a given API context is already used. You need to provide the context name you want to check. 
    * @param query **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   */
  @RequestLine("HEAD /apis?query={query}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  void apisHead(@Param("query") String query, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Check given API attibute name is already exist 
   * Using this operation, you can check a given API context is already used. You need to provide the context name you want to check. 
   * Note, this is equivalent to the other <code>apisHead</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisHeadQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>query - **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)</li>
   *   </ul>
   */
  @RequestLine("HEAD /apis?query={query}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  void apisHead(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisHead</code> method in a fluent style.
   */
  public static class ApisHeadQueryParams extends HashMap<String, Object> {
    public ApisHeadQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Import API Definition
   * This operation can be used to create api from api definition.  API definition can be either Swagger or a WSDL  WSDL can be speficied as a single file or a ZIP archive with WSDLs and reference XSDs etc. When the type is WSDL, it is a **must** to specify additionalProperties with API&#39;s name, version, context and endpoints. See the example for additionalProperties. 
    * @param type Definition type to upload (optional, default to SWAGGER)
    * @param file Definition to uploadas a file (optional)
    * @param url Definition url (optional)
    * @param additionalProperties Additional attributes specified as a stringified JSON with API&#39;s schema (optional)
    * @param implementationType Currently this is only used when creating an API using a WSDL.  If &#39;SOAP&#39; is specified, the API will be created with only one resource &#39;POST /&#39; which is to be used for SOAP operations.  If &#39;HTTP_BINDING&#39; is specified, the API will be created with resources using HTTP binding operations which are extracted from the WSDL.  (optional, default to SOAP)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return API
   */
  @RequestLine("POST /apis/import-definition")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  API apisImportDefinitionPost(@Param("type") String type, @Param("file") File file, @Param("url") String url, @Param("additionalProperties") String additionalProperties, @Param("implementationType") String implementationType, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Create a new API
   * This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 
    * @param body API object that needs to be added  (required)
   * @return API
   */
  @RequestLine("POST /apis")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  API apisPost(API body);

  /**
   * Validate API definition and retrieve a summary
   * This operation can be used to validate a swagger or WSDL definition and retrieve a summary. 
    * @param type Definition type to upload (required)
    * @param file Definition to upload as a file (optional)
    * @param url Definition url (optional)
   * @return APIDefinitionValidationResponse
   */
  @RequestLine("POST /apis/validate-definition")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  APIDefinitionValidationResponse apisValidateDefinitionPost(@Param("type") String type, @Param("file") File file, @Param("url") String url);
}
