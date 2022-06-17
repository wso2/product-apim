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

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import com.google.common.io.Files;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.ThrottlePolicyDetailsDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottlePolicyDetailsListDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils.encodeCredentials;

public class GetThrottlingPoliciesTestCase extends APIMIntegrationBaseTest {
    private final String ADMIN1_USERNAME = "admin1";
    private final String PASSWORD = "admin1";
    private final String ADMIN_ROLE = "admin";
    private AdminApiTestHelper adminApiTestHelper;
    private final String getThrottlePoliciesResource = "/throttling/policies/search";
    private final  String appPolicyName="50PerMin";
    private final String subPolicyName ="Gold";
    private final String apiPolicyName="10KPerMin";
    private String getThrottlePoliciesUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public GetThrottlingPoliciesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true) public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();

        getThrottlePoliciesUrl =
                adminURLHttps + APIMIntegrationConstants.REST_API_ADMIN_CONTEXT_FULL_0 + getThrottlePoliciesResource;
    }

    @Test(groups = { "wso2.am" }, description = "Get ThrottlePolicies List and check few default policies")
    public void testThrottlePoliciesGet() throws Exception {
        //construct get Throttle Policies url
        URL getRequest = new URL(getThrottlePoliciesUrl);
        ThrottlePolicyDetailsListDTO policies = getPolicies(user.getUserName(), user.getPassword(), getRequest);
        List<ThrottlePolicyDetailsDTO> policyList = policies.getList();
        Assert.assertTrue(containsPolicy(policyList, appPolicyName), "Doesn't contain app Policy");
        Assert.assertTrue(containsPolicy(policyList, subPolicyName), "Doesn't contain sub Policy");
        Assert.assertTrue(containsPolicy(policyList, apiPolicyName), "Doesn't contain api Policy");
    }

    /**
     * Checks whether a certain policy exists in the policy list
     * @param list throttle policy details list
     * @param name throttle policy name
     * @return true or false on the policy presence in the policy list
     */
    private boolean containsPolicy(List<ThrottlePolicyDetailsDTO> list, String name) {
        return list.stream().anyMatch(o -> o.getPolicyName().equals(name));
    }

    /**
     * Execute GET request to read the data to ThrottlePolicyDetailsListDTo
     * @param getRequest url for get throttling policies
     * @return ThrottlePolicyDetailsListDTo
     * @throws URISyntaxException throws if URI is malformed
     * @throws IOException throws if connection issues occurred
     */
    private ThrottlePolicyDetailsListDTO getPolicies(String username, String password, URL getRequest)
            throws URISyntaxException, IOException {
        //construct export Throttle Policy url
        File TempDir = Files.createTempDir();
        String fileName = "get_policies";
        File policiesFile = new File(TempDir.getAbsolutePath() + File.separator + fileName + ".json");
        CloseableHttpResponse response = getThrottlePoliciesRequest(getRequest, username, password);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (FileOutputStream outStream = new FileOutputStream(policiesFile)) {
                entity.writeTo(outStream);
            }
        }
        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code is not as expected");
        Assert.assertTrue(policiesFile.exists(), "File save was not successful");

        JSONParser parser = new JSONParser();
        Object obj;
        try {
            obj = parser.parse(new FileReader(policiesFile));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = (JSONObject) obj;
        ObjectMapper mapper = new ObjectMapper();
        ThrottlePolicyDetailsListDTO policies = mapper.convertValue(jsonObject, ThrottlePolicyDetailsListDTO.class);
        return policies;
    }

    /**
     * Setting Request Headers and executing get throttle policies request
     * @param getRequest url for get policies
     * @return get policies response
     * @throws IOException throws if connection issues occurred
     * @throws URISyntaxException throws if URI is malformed
     */
    private CloseableHttpResponse getThrottlePoliciesRequest(URL getRequest, String username, String password)
            throws IOException, URISyntaxException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpGet get = new HttpGet(getRequest.toURI());
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(username, password.toCharArray()));
        return client.execute(get);
    }

}
