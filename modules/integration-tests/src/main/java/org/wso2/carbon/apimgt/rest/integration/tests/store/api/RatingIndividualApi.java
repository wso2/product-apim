package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.util.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;


public interface RatingIndividualApi extends ApiClient.Api {


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
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}",
    
    "If-Modified-Since: {ifModifiedSince}"
  })
  Rating apisApiIdRatingsRatingIdGet(@Param("apiId") String apiId, @Param("ratingId") String ratingId, @Param("ifNoneMatch") String ifNoneMatch, @Param("ifModifiedSince") String ifModifiedSince);

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
}
