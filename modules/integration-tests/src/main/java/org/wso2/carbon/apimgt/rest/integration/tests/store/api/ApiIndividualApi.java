package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.store.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Document;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.DocumentList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Rating;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.RatingList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:26:55.409+05:30")
public interface ApiIndividualApi extends ApiClient.Api {


  /**
   * Delete an API comment
   * Remove a Comment 
    * @param commentId Comment Id  (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   */
  @RequestLine("DELETE /apis/{apiId}/comments/{commentId}")
  @Headers({
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  void apisApiIdCommentsCommentIdDelete(@Param("commentId") String commentId, @Param("apiId") String apiId, @Param
          ("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Get details of an API comment
   * Get the individual comment given by a username for a certain API. 
    * @param commentId Comment Id  (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Comment
   */
  @RequestLine("GET /apis/{apiId}/comments/{commentId}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Comment apisApiIdCommentsCommentIdGet(@Param("commentId") String commentId, @Param("apiId") String apiId, @Param
          ("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Update an API comment
   * Update a certain Comment 
    * @param commentId Comment Id  (required)
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Comment object that needs to be updated  (required)
    * @param ifMatch Validator for conditional requests; based on ETag.  (optional)
    * @param ifUnmodifiedSince Validator for conditional requests; based on Last Modified header.  (optional)
   * @return Comment
   */
  @RequestLine("PUT /apis/{apiId}/comments/{commentId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-Match: {ifMatch}",
    
    "If-Unmodified-Since: {ifUnmodifiedSince}"
  })
  Comment apisApiIdCommentsCommentIdPut(@Param("commentId") String commentId, @Param("apiId") String apiId, Comment
          body, @Param("ifMatch") String ifMatch, @Param("ifUnmodifiedSince") String ifUnmodifiedSince);

  /**
   * Add an API comment
   * 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Comment object that should to be added  (required)
   * @return Comment
   */
  @RequestLine("POST /apis/{apiId}/comments")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Comment apisApiIdCommentsPost(@Param("apiId") String apiId, Comment body);

  /**
   * Get the content of an API document
   * Downloads a FILE type document/get the inline content or source url of a certain document. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param documentId **Document Identifier**  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/documents/{documentId}/content")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdDocumentsDocumentIdContentGet(@Param("apiId") String apiId, @Param("documentId") String documentId,
                                              @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get a document of an API
   * Get a particular document associated with an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param documentId **Document Identifier**  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Document
   */
  @RequestLine("GET /apis/{apiId}/documents/{documentId}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Document apisApiIdDocumentsDocumentIdGet(@Param("apiId") String apiId, @Param("documentId") String documentId,
                                           @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get a list of API documents
   * Get a list of documents belonging to an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return DocumentList
   */
  @RequestLine("GET /apis/{apiId}/documents?limit={limit}&offset={offset}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  DocumentList apisApiIdDocumentsGet(@Param("apiId") String apiId, @Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get a list of API documents
   * Get a list of documents belonging to an API. 
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
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  DocumentList apisApiIdDocumentsGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded = true) Map<String, Object> queryParams);

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
   * Get details of an API
   * Get details of an API 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return API
   */
  @RequestLine("GET /apis/{apiId}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  API apisApiIdGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get API ratings
   * Get the rating of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
   * @return RatingList
   */
  @RequestLine("GET /apis/{apiId}/ratings?limit={limit}&offset={offset}")
  @Headers({
    "Accept: application/json",
  })
  RatingList apisApiIdRatingsGet(@Param("apiId") String apiId, @Param("limit") Integer limit, @Param("offset") Integer offset);

  /**
   * Get API ratings
   * Get the rating of an API. 
   * Note, this is equivalent to the other <code>apisApiIdRatingsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisApiIdRatingsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return RatingList
   */
  @RequestLine("GET /apis/{apiId}/ratings?limit={limit}&offset={offset}")
  @Headers({
  "Accept: application/json",
  })
  RatingList apisApiIdRatingsGet(@Param("apiId") String apiId, @QueryMap(encoded = true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisApiIdRatingsGet</code> method in a fluent style.
   */
  public static class ApisApiIdRatingsGetQueryParams extends HashMap<String, Object> {
    public ApisApiIdRatingsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApisApiIdRatingsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get a single API rating
   * Get a specific rating of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ratingId Rating Id  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Rating
   */
  @RequestLine("GET /apis/{apiId}/ratings/{ratingId}")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Rating apisApiIdRatingsRatingIdGet(@Param("apiId") String apiId, @Param("ratingId") String ratingId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Generate a SDK for an API 
   * This operation can be used to generate SDKs (System Development Kits), for the APIs available in the API Store, for a requested development language. 
    * @param apiId ID of the specific API for which the SDK is required.  (required)
    * @param language Programming language of the SDK that is required.  (required)
   */
  @RequestLine("GET /apis/{apiId}/sdks/{language}")
  @Headers({
    "Accept: application/zip",
  })
  void apisApiIdSdksLanguageGet(@Param("apiId") String apiId, @Param("language") String language);

  /**
   * Get API swagger definition
   * Get the swagger of an API 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/swagger")
  @Headers({
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdSwaggerGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Add or update logged in user&#39;s rating for an API
   * Adds or updates a rating 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param body Rating object that should to be added  (required)
   * @return Rating
   */
  @RequestLine("PUT /apis/{apiId}/user-rating")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Rating apisApiIdUserRatingPut(@Param("apiId") String apiId, Rating body);

  /**
   * Get API WSDL definition
   * This operation can be used to retrieve the swagger definition of an API. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/wsdl")
  @Headers({
    "Accept: application/octet-stream",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdWsdlGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);
}
