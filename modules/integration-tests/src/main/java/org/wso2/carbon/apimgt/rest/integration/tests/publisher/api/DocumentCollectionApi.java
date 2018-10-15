package org.wso2.carbon.apimgt.rest.integration.tests.publisher.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Document;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.DocumentList;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface DocumentCollectionApi extends ApiClient.Api {


  /**
   * Get a list of documents of an API
   * This operation can be used to retrieve a list of documents belonging to an API by providing the id of the API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return DocumentList
   */
  @RequestLine("GET /apis/{apiId}/documents?limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  DocumentList apisApiIdDocumentsGet(@Param("apiId") String apiId, @Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get a list of documents of an API
   * This operation can be used to retrieve a list of documents belonging to an API by providing the id of the API. 
   * Note, this is equivalent to the other <code>apisApiIdDocumentsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisApiIdDocumentsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return DocumentList
   */
  @RequestLine("GET /apis/{apiId}/documents?limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  DocumentList apisApiIdDocumentsGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisApiIdDocumentsGet</code> method in a fluent style.
   */
  public static class ApisApiIdDocumentsGetQueryParams extends HashMap<String, Object> {
    public ApisApiIdDocumentsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApisApiIdDocumentsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Add a new document to an API
   * This operation can be used to add a new documentation to an API. This operation only adds the metadata of a document. To add the actual content we need to use **Upload the content of an API document ** API once we obtain a document Id by this operation. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Document object that needs to be added  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return Document
   */
  @RequestLine("POST /apis/{apiId}/documents")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  Document apisApiIdDocumentsPost(@Param("apiId") String apiId, Document body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);
}
