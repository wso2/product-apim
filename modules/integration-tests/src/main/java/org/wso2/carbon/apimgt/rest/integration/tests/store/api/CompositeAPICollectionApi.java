package org.wso2.carbon.apimgt.rest.integration.tests.store.api;

import org.wso2.carbon.apimgt.rest.integration.tests.util.ApiClient;

import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CompositeAPI;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CompositeAPIList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-04-16T14:42:47.879+05:30")
public interface CompositeAPICollectionApi extends ApiClient.Api {


  /**
   * Retrieve/Search Composite APIs 
   * This operation provides you a list of available Composite APIs qualifying under a given search condition.  Each retrieved Composite API is represented with a minimal amount of attributes. If you want to get complete details of a Composite API, you need to use **Get details of a Composite API** operation. 
    * @param limit Maximum size of resource array to return.  (optional, default to 25)
    * @param offset Starting point within the complete list of items qualified.  (optional, default to 0)
    * @param query **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match a Composite API if the provider of the Composite API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match a Composite API if the provider of the Composite API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against Composite API Name.  (optional)
    * @param ifNoneMatch Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  (optional)
   * @return CompositeAPIList
   */
  @RequestLine("GET /composite-apis?limit={limit}&offset={offset}&query={query}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
    "If-None-Match: {ifNoneMatch}"
  })
  CompositeAPIList compositeApisGet(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("query") String query, @Param("ifNoneMatch") String ifNoneMatch);

  /**
   * Create a new API
   * This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 
    * @param body API object that needs to be added  (required)
   * @return CompositeAPI
   */
  @RequestLine("POST /composite-apis")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  CompositeAPI compositeApisPost(CompositeAPI body);
}
