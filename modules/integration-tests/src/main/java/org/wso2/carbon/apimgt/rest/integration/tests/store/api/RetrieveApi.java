package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CommentList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Document;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.DocumentList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.RatingList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Subscription;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.SubscriptionList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.TagList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Tier;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.TierList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface RetrieveApi extends ApiClient.Api {


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
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Comment apisApiIdCommentsCommentIdGet(@Param("commentId") String commentId, @Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Retrieve API comments
   * Get a list of Comments that are already added to APIs 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
   * @return CommentList
   */
  @RequestLine("GET /apis/{apiId}/comments?limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  CommentList apisApiIdCommentsGet(@Param("apiId") String apiId, @Param("limit") Integer limit, @Param("offset") Integer offset);

  /**
   * Retrieve API comments
   * Get a list of Comments that are already added to APIs 
   * Note, this is equivalent to the other <code>apisApiIdCommentsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link ApisApiIdCommentsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return CommentList
   */
  @RequestLine("GET /apis/{apiId}/comments?limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
  })
  CommentList apisApiIdCommentsGet(@Param("apiId") String apiId, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>apisApiIdCommentsGet</code> method in a fluent style.
   */
  public static class ApisApiIdCommentsGetQueryParams extends HashMap<String, Object> {
    public ApisApiIdCommentsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public ApisApiIdCommentsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }

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
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdDocumentsDocumentIdContentGet(@Param("apiId") String apiId, @Param("documentId") String documentId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

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
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Document apisApiIdDocumentsDocumentIdGet(@Param("apiId") String apiId, @Param("documentId") String documentId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

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
    "Content-Type: application/json",
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
   * Get details of an API
   * Get details of an API 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return API
   */
  @RequestLine("GET /apis/{apiId}")
  @Headers({
    "Content-Type: application/json",
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
    "Content-Type: application/json",
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
  "Content-Type: application/json",
  "Accept: application/json",
  })
  RatingList apisApiIdRatingsGet(@Param("apiId") String apiId, @QueryMap(encoded=true) Map<String, Object> queryParams);

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
   * Get API swagger definition
   * Get the swagger of an API 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   */
  @RequestLine("GET /apis/{apiId}/swagger")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  void apisApiIdSwaggerGet(@Param("apiId") String apiId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Retrieve/Search APIs 
   * Get a list of available APIs qualifying under a given search condition. 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param labels Comma seperated store labels  (optional)
    * @param query **Search condition**.  You can search in attributes by using an **\&quot;attribute:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider, tag **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return APIList
   */
  @RequestLine("GET /apis?limit={limit}&offset={offset}&labels={labels}&query={query}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  APIList apisGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("labels") String labels, @Param("query") String query, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Retrieve/Search APIs 
   * Get a list of available APIs qualifying under a given search condition. 
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
   *   <li>labels - Comma seperated store labels  (optional)</li>
   *   <li>query - **Search condition**.  You can search in attributes by using an **\&quot;attribute:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider, tag **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  (optional)</li>
   *   </ul>
   * @return APIList
   */
  @RequestLine("GET /apis?limit={limit}&offset={offset}&labels={labels}&query={query}")
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
    public ApisGetQueryParams labels(final String value) {
      put("labels", EncodingUtils.encode(value));
      return this;
    }
    public ApisGetQueryParams query(final String value) {
      put("query", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get details of an application
   * Get application details 
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Application
   */
  @RequestLine("GET /applications/{applicationId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Application applicationsApplicationIdGet(@Param("applicationId") String applicationId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get all available policies
   * Get available policies 
    * @param tierLevel List API or Application type policies.  (required)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return List&lt;TierList&gt;
   */
  @RequestLine("GET /policies/{tierLevel}?limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  List<TierList> policiesTierLevelGet(@Param("tierLevel") String tierLevel, @Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get all available policies
   * Get available policies 
   * Note, this is equivalent to the other <code>policiesTierLevelGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link PoliciesTierLevelGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param tierLevel List API or Application type policies.  (required)
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return List&lt;TierList&gt;
   */
  @RequestLine("GET /policies/{tierLevel}?limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  List<TierList> policiesTierLevelGet(@Param("tierLevel") String tierLevel, @Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>policiesTierLevelGet</code> method in a fluent style.
   */
  public static class PoliciesTierLevelGetQueryParams extends HashMap<String, Object> {
    public PoliciesTierLevelGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public PoliciesTierLevelGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get a single policy details
   * Get policy details 
    * @param tierName Tier name  (required)
    * @param tierLevel List API or Application type policies.  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Tier
   */
  @RequestLine("GET /policies/{tierLevel}/{tierName}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Tier policiesTierLevelTierNameGet(@Param("tierName") String tierName, @Param("tierLevel") String tierLevel, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get all subscriptions
   * Get subscription list. The API Identifier or Application Identifier the subscriptions of which are to be returned are passed as parameters. 
    * @param apiId **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)
    * @param applicationId **Application Identifier** consisting of the UUID of the Application.  (required)
    * @param apiType **API Type** filters information pertaining to a specific type of API  (optional)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return SubscriptionList
   */
  @RequestLine("GET /subscriptions?apiId={apiId}&applicationId={applicationId}&apiType={apiType}&offset={offset}&limit={limit}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  SubscriptionList subscriptionsGet(@Param("apiId") String apiId, @Param("applicationId") String applicationId, @Param("apiType") String apiType, @Param("offset") Integer offset, @Param("limit") Integer limit, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get all subscriptions
   * Get subscription list. The API Identifier or Application Identifier the subscriptions of which are to be returned are passed as parameters. 
   * Note, this is equivalent to the other <code>subscriptionsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link SubscriptionsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>apiId - **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  (required)</li>
   *   <li>applicationId - **Application Identifier** consisting of the UUID of the Application.  (required)</li>
   *   <li>apiType - **API Type** filters information pertaining to a specific type of API  (optional)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   </ul>
   * @return SubscriptionList
   */
  @RequestLine("GET /subscriptions?apiId={apiId}&applicationId={applicationId}&apiType={apiType}&offset={offset}&limit={limit}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  SubscriptionList subscriptionsGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>subscriptionsGet</code> method in a fluent style.
   */
  public static class SubscriptionsGetQueryParams extends HashMap<String, Object> {
    public SubscriptionsGetQueryParams apiId(final String value) {
      put("apiId", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams applicationId(final String value) {
      put("applicationId", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams apiType(final String value) {
      put("apiType", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
    public SubscriptionsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Get details of a subscription
   * Get subscription details 
    * @param subscriptionId Subscription Id  (required)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
    * @param ifModifiedSince Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource.  (optional)
   * @return Subscription
   */
  @RequestLine("GET /subscriptions/{subscriptionId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Subscription subscriptionsSubscriptionIdGet(@Param("subscriptionId") String subscriptionId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

  /**
   * Get all tags
   * Get a list of tags that are already added to APIs 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return TagList
   */
  @RequestLine("GET /tags?limit={limit}&offset={offset}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  TagList tagsGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Get all tags
   * Get a list of tags that are already added to APIs 
   * Note, this is equivalent to the other <code>tagsGet</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link TagsGetQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>limit - Maximum size of resource array to return.  (optional, default to 25)</li>
   *   <li>offset - Starting point within the complete list of items qualified.  (optional, default to 0)</li>
   *   </ul>
   * @return TagList
   */
  @RequestLine("GET /tags?limit={limit}&offset={offset}")
  @Headers({
  "Content-Type: application/json",
  "Accept: application/json",
      "If-None-Match: {ifNoneMatch}"
  })
  TagList tagsGet(@Param("ifNoneMatch") String ifNoneMatch, @QueryMap(encoded=true) Map<String, Object> queryParams);

  /**
   * A convenience class for generating query parameters for the
   * <code>tagsGet</code> method in a fluent style.
   */
  public static class TagsGetQueryParams extends HashMap<String, Object> {
    public TagsGetQueryParams limit(final Integer value) {
      put("limit", EncodingUtils.encode(value));
      return this;
    }
    public TagsGetQueryParams offset(final Integer value) {
      put("offset", EncodingUtils.encode(value));
      return this;
    }
  }
}
