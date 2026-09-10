/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.tests.publisher;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoAdditionalPropertiesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoAdditionalPropertiesMapDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Verifies the expandProperties query parameter of the Publisher REST API listing (GET /apis).
 *
 * The listing keeps returning empty additionalProperties and additionalPropertiesMap unless the caller opts in
 * with expandProperties=true, and when it does opt in the values must match what GET /apis/{apiId} returns.
 */
public class AdditionalPropertiesInAPIListingTestCase extends APIMIntegrationBaseTest {

    private static final String API_NAME = "AdditionalPropertiesListingAPI";
    private static final String API_CONTEXT = "additionalPropertiesListing";
    private static final String API_VERSION = "1.0.0";

    private static final String PLAIN_PROPERTY_NAME = "dept";
    private static final String PLAIN_PROPERTY_VALUE = "finance";
    private static final String DISPLAYABLE_PROPERTY_NAME = "owner";
    private static final String DISPLAYABLE_PROPERTY_VALUE = "jane";
    private static final String DISPLAY_SUFFIX = "__display";

    private static final long INDEXING_TIMEOUT = 60000L;
    private static final long INDEXING_POLL_INTERVAL = 2000L;

    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public AdditionalPropertiesInAPIListingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);

        String endpointUrl = gatewayUrlsWrk.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));
        apiRequest.setVersion(API_VERSION);

        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiCreationResponse.getData();
        assertNotNull(apiId, "API was not created as expected");

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

        List<APIInfoAdditionalPropertiesDTO> additionalProperties = new ArrayList<>();
        additionalProperties.add(plainProperty);
        additionalProperties.add(displayableProperty);

        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);
        apiDto.setAdditionalProperties(additionalProperties);
        apiDto.setAdditionalPropertiesMap(new HashMap<>());
        restAPIPublisher.updateAPI(apiDto, apiId);

        // The publisher listing is backed by the registry search index, which is populated asynchronously.
        waitForAPIToAppearInListing();
    }

    @Test(groups = { "wso2.am" }, description = "Additional properties stay empty in the API listing when "
            + "expandProperties is not requested")
    public void testAPIListingWithoutExpandPropertiesReturnsEmptyAdditionalProperties() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs());

        assertTrue(apiInfo.getAdditionalProperties().isEmpty(),
                "additionalProperties must stay empty when expandProperties is not requested");
        assertTrue(apiInfo.getAdditionalPropertiesMap().isEmpty(),
                "additionalPropertiesMap must stay empty when expandProperties is not requested");
    }

    @Test(groups = { "wso2.am" }, description = "Additional properties stay empty in the API listing when "
            + "expandProperties is explicitly false",
            dependsOnMethods = "testAPIListingWithoutExpandPropertiesReturnsEmptyAdditionalProperties")
    public void testAPIListingWithExpandPropertiesFalseReturnsEmptyAdditionalProperties() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs(false));

        assertTrue(apiInfo.getAdditionalProperties().isEmpty(),
                "additionalProperties must stay empty when expandProperties is false");
        assertTrue(apiInfo.getAdditionalPropertiesMap().isEmpty(),
                "additionalPropertiesMap must stay empty when expandProperties is false");
    }

    @Test(groups = { "wso2.am" }, description = "Additional properties are returned in the API listing when "
            + "expandProperties is true",
            dependsOnMethods = "testAPIListingWithExpandPropertiesFalseReturnsEmptyAdditionalProperties")
    public void testAPIListingWithExpandPropertiesReturnsAdditionalProperties() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs(true));

        // Index the returned list by name so that a missing property cannot be masked by a duplicate of another.
        List<APIInfoAdditionalPropertiesDTO> additionalProperties = apiInfo.getAdditionalProperties();
        Map<String, APIInfoAdditionalPropertiesDTO> propertiesByName = new HashMap<>();
        for (APIInfoAdditionalPropertiesDTO property : additionalProperties) {
            assertNull(propertiesByName.put(property.getName(), property),
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

    @Test(groups = { "wso2.am" }, description = "The expanded API listing reports the same additional properties "
            + "as the individual API retrieval",
            dependsOnMethods = "testAPIListingWithExpandPropertiesReturnsAdditionalProperties")
    public void testExpandedListingMatchesIndividualAPIRetrieval() throws Exception {

        APIInfoDTO apiInfo = getApiFromListing(restAPIPublisher.getAllAPIs(true));
        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);

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
     * Picks the API created by this test out of a publisher listing response.
     *
     * @param apiListDTO the listing response
     * @return the APIInfoDTO of the API under test
     */
    private APIInfoDTO getApiFromListing(APIListDTO apiListDTO) {

        assertNotNull(apiListDTO, "API listing response should not be null");
        APIInfoDTO apiInfo = findApiInListing(apiListDTO);
        assertNotNull(apiInfo, "API " + API_NAME + " was not found in the publisher API listing");
        return apiInfo;
    }

    /**
     * Looks up the API created by this test in a publisher listing response without asserting on the outcome.
     *
     * @param apiListDTO the listing response, which may be null when no APIs are returned
     * @return the APIInfoDTO of the API under test, or null when it is not present
     */
    private APIInfoDTO findApiInListing(APIListDTO apiListDTO) {

        if (apiListDTO == null || apiListDTO.getList() == null) {
            return null;
        }
        for (APIInfoDTO apiInfo : apiListDTO.getList()) {
            if (apiId.equals(apiInfo.getId())) {
                return apiInfo;
            }
        }
        return null;
    }

    /**
     * Polls the publisher listing until the API created by this test becomes visible, so that a slow registry
     * indexing run does not fail the assertions of the tests that follow.
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
        org.testng.Assert.fail("API " + API_NAME + " was not indexed into the publisher API listing within "
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

        List<String> comparableProperties = new ArrayList<>();
        for (APIInfoAdditionalPropertiesDTO property : properties) {
            comparableProperties.add(property.getName() + ":" + property.getValue() + ":" + property.isDisplay());
        }
        java.util.Collections.sort(comparableProperties);
        return comparableProperties;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}
