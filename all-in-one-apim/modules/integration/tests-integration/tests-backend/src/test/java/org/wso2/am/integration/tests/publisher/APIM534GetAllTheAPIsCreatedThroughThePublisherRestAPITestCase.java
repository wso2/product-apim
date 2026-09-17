/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoAdditionalPropertiesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoAdditionalPropertiesMapDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Get all the APIs created through the publisher REST API
 * APIM-534 / APIM-542
 *
 * Also covers the expandProperties query parameter of the listing: additionalProperties and
 * additionalPropertiesMap stay empty unless the caller opts in with expandProperties=true, and when it does
 * opt in the values must match what GET /apis/{apiId} returns.
 */

public class APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase extends
        APIMIntegrationBaseTest {

    private static final Log log = LogFactory.
            getLog(APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase.class);
    private static final String apiNameTest1 = "APIM534PublisherTest1";
    private static final String apiNameTest2 = "APIM534PublisherTest2";
    private static final String apiNameTest3 = "APIM534PublisherTest3";
    private static final String apiVersion = "1.0.0";
    private static String apiProviderName;
    private static String apiTest1EndPointUrl;
    private static String apiTest2EndPointUrl;
    private static String apiTest3EndPointUrl;
    private static String id;
    List<String> idList = new ArrayList<String>();

    private static final String PLAIN_PROPERTY_NAME = "dept";
    private static final String PLAIN_PROPERTY_VALUE = "finance";
    private static final String DISPLAYABLE_PROPERTY_NAME = "owner";
    private static final String DISPLAYABLE_PROPERTY_VALUE = "jane";
    private static final String DISPLAY_SUFFIX = "__display";

    private static final long INDEXING_TIMEOUT = 60000L;
    private static final long INDEXING_POLL_INTERVAL = 2000L;

    private String propertiesApiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                //                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @DataProvider(name = "createAPI")
    public static Object[][] createAppWithValidDataProvider() throws Exception {

        return new Object[][]{
                {apiNameTest1, "apim534PublisherTest1API", apiVersion, apiProviderName,
                        new URL(apiTest1EndPointUrl)},
                {apiNameTest2, "apim534PublisherTest2API", apiVersion, apiProviderName,
                        new URL(apiTest2EndPointUrl)},
                {apiNameTest3, "apim534PublisherTest3API", apiVersion, apiProviderName,
                        new URL(apiTest3EndPointUrl)}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiTest1EndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";
        String apiTest2EndpointPostfixUrl = "name-check1/";
        String apiTest3EndpointPostfixUrl = "pizzashack-api-1.0.0/api/";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiTest1EndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiTest1EndpointPostfixUrl;
        apiTest2EndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiTest2EndpointPostfixUrl;
        apiTest3EndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiTest3EndpointPostfixUrl;

        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(dataProvider = "createAPI", description = "Create an API using valid data and get all " +
            "the API through Publisher Rest API")
    public void testGetAllTheAPICreatedThroughThePublisherRestAPI
            (String apiName, String context, String version, String provider,
             URL endpointUrl) throws Exception {


        APIRequest apiRequest = new APIRequest(apiName, context, endpointUrl);
        apiRequest.setVersion(version);
        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiRequest);//(apiCreationRequestBean);
        id = apiCreationResponse.getData(); 
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertNotNull(id, "APIs are not created as expected");
        idList.add(id);
        
        HttpResponse resp = restAPIPublisher.getAPI(id);
        assertEquals(resp.getResponseCode(), Response.Status.OK.getStatusCode(), "API does not exist");
        //Create a API list with newly added APIs
        List<String> apiList = Arrays.asList(apiNameTest1, apiNameTest2, apiNameTest3);
        log.info("My API List :" + apiList);

        //Check the availability of API in Publisher
        Thread.sleep(5000);
        APIListDTO response = restAPIPublisher.getAllAPIs();
        Gson gson = new Gson();
        String json = gson.toJson(response);
        JSONObject jsonObject = new JSONObject(json);
        JSONArray jsonArray = jsonObject.getJSONArray("list");
        List<String> allApiList = new ArrayList<String>();


        for (int i = 0; i < jsonArray.length(); i++) {
            String name = jsonArray.getJSONObject(i).getString("name");
            allApiList.add(name);
        }
        log.info("All API List :" + allApiList);

    }

    @Test(groups = {"wso2.am"}, description = "Validate if an API exists through the publisher " +
            "REST API", dependsOnMethods = "testGetAllTheAPICreatedThroughThePublisherRestAPI")
    public void testCheckIfAnAPIExistsThroughThePublisherRestAPI() throws Exception {

        //Trying to Create an API with an existing API Name
        APIRequest apiRequest = new APIRequest(apiNameTest1, "apim534PublisherTest1API", new URL(apiTest1EndPointUrl));
        apiRequest.setVersion(apiVersion);
        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        //response is null if addAPI fails with same api name
        assertNull("Added same api again", response);

    }

    // The expandProperties tests below reuse the first API created above instead of creating one of their own,
    // and are chained with dependsOnMethods so that one broken expectation is reported once instead of
    // repeating as a failure in every test that follows it.
    @Test(groups = {"wso2.am"}, description = "Add additional properties to an API so that the "
            + "expandProperties behaviour of the API listing can be verified",
            dependsOnMethods = "testCheckIfAnAPIExistsThroughThePublisherRestAPI")
    public void testAddAdditionalPropertiesToAnAPI() throws Exception {

        propertiesApiId = idList.get(0);

        // One plain property and one displayable property, so that both branches of the __display handling
        // are exercised by the listing.
        APIInfoAdditionalPropertiesDTO plainProperty = new APIInfoAdditionalPropertiesDTO();
        plainProperty.setName(PLAIN_PROPERTY_NAME);
        plainProperty.setValue(PLAIN_PROPERTY_VALUE);
        plainProperty.setDisplay(false);

        APIInfoAdditionalPropertiesDTO displayableProperty = new APIInfoAdditionalPropertiesDTO();
        displayableProperty.setName(DISPLAYABLE_PROPERTY_NAME);
        displayableProperty.setValue(DISPLAYABLE_PROPERTY_VALUE);
        displayableProperty.setDisplay(true);

        List<APIInfoAdditionalPropertiesDTO> additionalProperties = new ArrayList<APIInfoAdditionalPropertiesDTO>();
        additionalProperties.add(plainProperty);
        additionalProperties.add(displayableProperty);

        // The properties are supplied through the list only. fromDTOtoAPI applies the list first and then the
        // map, so the map is cleared to keep this the single source of the expected values.
        APIDTO apiDto = restAPIPublisher.getAPIByID(propertiesApiId);
        apiDto.setAdditionalProperties(additionalProperties);
        apiDto.setAdditionalPropertiesMap(new HashMap<String, APIInfoAdditionalPropertiesMapDTO>());
        restAPIPublisher.updateAPI(apiDto, propertiesApiId);

        APIDTO updatedApiDto = restAPIPublisher.getAPIByID(propertiesApiId);
        assertEquals(updatedApiDto.getAdditionalProperties().size(), 2,
                "Both additional properties should have been stored against the API");

        waitForAPIToAppearInListing();
    }

    @Test(groups = {"wso2.am"}, description = "Additional properties stay empty in the API listing when "
            + "expandProperties is not requested", dependsOnMethods = "testAddAdditionalPropertiesToAnAPI")
    public void testAPIListingWithoutExpandPropertiesReturnsEmptyAdditionalProperties() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs());

        assertTrue(apiInfo.getAdditionalProperties().isEmpty(),
                "additionalProperties must stay empty when expandProperties is not requested");
        assertTrue(apiInfo.getAdditionalPropertiesMap().isEmpty(),
                "additionalPropertiesMap must stay empty when expandProperties is not requested");
    }

    @Test(groups = {"wso2.am"}, description = "Additional properties stay empty in the API listing when "
            + "expandProperties is explicitly false",
            dependsOnMethods = "testAPIListingWithoutExpandPropertiesReturnsEmptyAdditionalProperties")
    public void testAPIListingWithExpandPropertiesFalseReturnsEmptyAdditionalProperties() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs(false));

        assertTrue(apiInfo.getAdditionalProperties().isEmpty(),
                "additionalProperties must stay empty when expandProperties is false");
        assertTrue(apiInfo.getAdditionalPropertiesMap().isEmpty(),
                "additionalPropertiesMap must stay empty when expandProperties is false");
    }

    @Test(groups = {"wso2.am"}, description = "Additional properties are returned in the API listing when "
            + "expandProperties is true",
            dependsOnMethods = "testAPIListingWithExpandPropertiesFalseReturnsEmptyAdditionalProperties")
    public void testAPIListingWithExpandPropertiesReturnsAdditionalProperties() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs(true));

        // Index the returned list by name so that a missing property cannot be masked by a duplicate of another.
        List<APIInfoAdditionalPropertiesDTO> additionalProperties = apiInfo.getAdditionalProperties();
        Map<String, APIInfoAdditionalPropertiesDTO> propertiesByName =
                new HashMap<String, APIInfoAdditionalPropertiesDTO>();
        for (APIInfoAdditionalPropertiesDTO property : additionalProperties) {
            Assert.assertNull(propertiesByName.put(property.getName(), property),
                    "Additional property " + property.getName() + " was returned more than once");
        }
        assertEquals(propertiesByName.size(), 2, "Exactly the two additional properties should be returned");
        assertTrue(propertiesByName.containsKey(PLAIN_PROPERTY_NAME),
                "The plain additional property should be returned");
        assertTrue(propertiesByName.containsKey(DISPLAYABLE_PROPERTY_NAME),
                "The displayable additional property should be returned");

        APIInfoAdditionalPropertiesDTO plainProperty = propertiesByName.get(PLAIN_PROPERTY_NAME);
        assertEquals(plainProperty.getValue(), PLAIN_PROPERTY_VALUE);
        assertFalse(plainProperty.isDisplay(), "A property without the __display suffix is not displayable");

        APIInfoAdditionalPropertiesDTO displayableProperty = propertiesByName.get(DISPLAYABLE_PROPERTY_NAME);
        assertEquals(displayableProperty.getValue(), DISPLAYABLE_PROPERTY_VALUE);
        assertTrue(displayableProperty.isDisplay(), "A property with the __display suffix is displayable");

        Map<String, APIInfoAdditionalPropertiesMapDTO> additionalPropertiesMap = apiInfo.getAdditionalPropertiesMap();
        assertEquals(additionalPropertiesMap.size(), 2, "Both additional properties should be in the map");

        APIInfoAdditionalPropertiesMapDTO plainMapProperty = additionalPropertiesMap.get(PLAIN_PROPERTY_NAME);
        assertEquals(plainMapProperty.getName(), PLAIN_PROPERTY_NAME);
        assertEquals(plainMapProperty.getValue(), PLAIN_PROPERTY_VALUE);
        assertFalse(plainMapProperty.isDisplay(), "A property without the __display suffix is not displayable");

        // The map is keyed by the raw property name, so the displayable property keeps its __display suffix in the
        // key while its name is stripped. display is always false for map entries - fromDTOtoAPI rebuilds the stored
        // key as <mapKey> + "__display" when isDisplay() is true, so a true value here would round-trip the property
        // back as "owner__display__display".
        APIInfoAdditionalPropertiesMapDTO displayableMapProperty =
                additionalPropertiesMap.get(DISPLAYABLE_PROPERTY_NAME + DISPLAY_SUFFIX);
        assertEquals(displayableMapProperty.getName(), DISPLAYABLE_PROPERTY_NAME,
                "The map entry name is stripped of the __display suffix");
        assertEquals(displayableMapProperty.getValue(), DISPLAYABLE_PROPERTY_VALUE);
        assertFalse(displayableMapProperty.isDisplay(),
                "display must stay false so that fromDTOtoAPI does not append __display twice");
    }

    @Test(groups = {"wso2.am"}, description = "The expanded API listing reports the same additional properties "
            + "as the individual API retrieval",
            dependsOnMethods = "testAPIListingWithExpandPropertiesReturnsAdditionalProperties")
    public void testExpandedListingMatchesIndividualAPIRetrieval() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs(true));
        APIDTO apiDto = restAPIPublisher.getAPIByID(propertiesApiId);

        assertEquals(toComparableProperties(apiInfo.getAdditionalProperties()),
                toComparableProperties(apiDto.getAdditionalProperties()),
                "additionalProperties of the listing and the individual API retrieval must match");

        assertEquals(apiInfo.getAdditionalPropertiesMap().keySet(), apiDto.getAdditionalPropertiesMap().keySet(),
                "additionalPropertiesMap keys of the listing and the individual API retrieval must match");
        for (Map.Entry<String, APIInfoAdditionalPropertiesMapDTO> entry :
                apiInfo.getAdditionalPropertiesMap().entrySet()) {
            APIInfoAdditionalPropertiesMapDTO expected = apiDto.getAdditionalPropertiesMap().get(entry.getKey());
            assertEquals(entry.getValue().getName(), expected.getName());
            assertEquals(entry.getValue().getValue(), expected.getValue());
            assertEquals(entry.getValue().isDisplay(), expected.isDisplay());
        }
    }

    /**
     * Picks the API carrying the additional properties out of a publisher listing response.
     *
     * @param apiListDTO the listing response
     * @return the APIInfoDTO of the API under test
     */
    private APIInfoDTO getApiFromListing(APIListDTO apiListDTO) {

        Assert.assertNotNull(apiListDTO, "API listing response should not be null");
        APIInfoDTO apiInfo = findApiInListing(apiListDTO);
        Assert.assertNotNull(apiInfo, "API " + apiNameTest1 + " was not found in the publisher API listing");
        return apiInfo;
    }

    /**
     * Looks up the API carrying the additional properties in a publisher listing response without asserting on
     * the outcome.
     *
     * @param apiListDTO the listing response, which may be null when no APIs are returned
     * @return the APIInfoDTO of the API under test, or null when it is not present
     */
    private APIInfoDTO findApiInListing(APIListDTO apiListDTO) {

        if (apiListDTO == null || apiListDTO.getList() == null) {
            return null;
        }
        for (APIInfoDTO apiInfo : apiListDTO.getList()) {
            if (propertiesApiId.equals(apiInfo.getId())) {
                return apiInfo;
            }
        }
        return null;
    }

    /**
     * Polls the publisher listing until the API carrying the additional properties becomes visible, so that a
     * slow registry indexing run does not fail the assertions of the tests that follow.
     * <p>
     * Waiting for visibility alone is sufficient. The search index only decides which artifacts match, while the
     * additional properties of each match are read from a live registry resource, so they already reflect the
     * preceding update by the time the API appears here.
     *
     * @throws Exception if the API does not appear within {@link #INDEXING_TIMEOUT}
     */
    private void waitForAPIToAppearInListing() throws Exception {

        long deadline = System.currentTimeMillis() + INDEXING_TIMEOUT;
        while (System.currentTimeMillis() < deadline) {
            if (findApiInListing(restAPIPublisher.getAllAPIs()) != null) {
                return;
            }
            Thread.sleep(INDEXING_POLL_INTERVAL);
        }
        Assert.fail("API " + apiNameTest1 + " was not indexed into the publisher API listing within "
                + INDEXING_TIMEOUT + " ms");
    }

    /**
     * Flattens additional properties into name/value/display strings so that two lists can be compared
     * irrespective of their ordering.
     *
     * @param properties additional properties to flatten
     * @return a sorted list of comparable representations
     */
    private List<String> toComparableProperties(List<APIInfoAdditionalPropertiesDTO> properties) {

        List<String> comparableProperties = new ArrayList<String>();
        for (APIInfoAdditionalPropertiesDTO property : properties) {
            comparableProperties.add(property.getName() + ":" + property.getValue() + ":" + property.isDisplay());
        }
        java.util.Collections.sort(comparableProperties);
        return comparableProperties;
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        for (String id : idList) {
            restAPIPublisher.deleteAPI(id);
        }
        super.cleanUp();
    }


}