/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.rest.integration.tests.store;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.RestAPIException;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.APIIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.ApplicationIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.SubscriptionIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIInfo;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Subscription;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

import java.util.ArrayList;
import java.util.List;

public class APIVisibilityTestCaseIT {

    private APIInfo apiInfo;
    List<Application> applicationList = new ArrayList<>();

    @Test(description = "user only available in visible group")
    public void testAPIVisibleToUserInRestrictedGroup() throws AMIntegrationTestException {

        Application application = new Application().name("APIVisibilityTestCaseIT1").throttlingTier("Unlimited");
        ApplicationIndividualApi applicationIndividualApi = TestUtil.getStoreApiClient("user7", TestUtil.getUser
                ("user7"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(ApplicationIndividualApi.class);
        SubscriptionIndividualApi subscriptionIndividualApi = TestUtil.getStoreApiClient("user7", TestUtil.getUser
                ("user7"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(SubscriptionIndividualApi.class);
        APICollectionApi apiCollectionApi = TestUtil.getStoreApiClient("user7", TestUtil.getUser("user7"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        APIList apiList = apiCollectionApi.apisGet(3, 0, null, "", null);
        Assert.assertTrue(apiList.getCount() > 0);
        APIInfo retrievedAPIInfo = null;
        for (APIInfo apiInfo : apiList.getList()) {
            if ("restrictedapi1".equals(apiInfo.getName())) {
                retrievedAPIInfo = apiInfo;
                break;
            }
        }
        Assert.assertNotNull(retrievedAPIInfo);
        Assert.assertEquals(retrievedAPIInfo.getName(), "restrictedapi1");
        Assert.assertEquals(retrievedAPIInfo.getVersion(), "1.0.0");
        Assert.assertEquals(retrievedAPIInfo.getLifeCycleStatus(), "Published");
        Assert.assertEquals(retrievedAPIInfo.getContext(), "/restrictedapi1/1.0.0");
        APIIndividualApi apiIndividualApi = TestUtil.getStoreApiClient("user7", TestUtil.getUser("user7"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        API api = apiIndividualApi.apisApiIdGet(retrievedAPIInfo.getId(), null, null);
        Assert.assertNotNull(api);
        // Able to subscribe
        application = applicationIndividualApi.applicationsPost(application);
        applicationList.add(application);
        Subscription subscription = new Subscription().policy("Unlimited").applicationId(application.getApplicationId
                ()).apiIdentifier(api.getId());
        subscriptionIndividualApi.subscriptionsPost(subscription);
        this.apiInfo = retrievedAPIInfo;
    }

    @Test(description = "user available in multiple groups with restricted group", dependsOnMethods =
            {"testAPIVisibleToUserInRestrictedGroup"})
    public void testAPIVisibleToUserInMultipleGroup() throws AMIntegrationTestException {

        APICollectionApi apiCollectionApi = TestUtil.getStoreApiClient("user6", TestUtil.getUser("user6"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        ApplicationIndividualApi applicationIndividualApi = TestUtil.getStoreApiClient("user6", TestUtil.getUser
                ("user6"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(ApplicationIndividualApi.class);
        SubscriptionIndividualApi subscriptionIndividualApi = TestUtil.getStoreApiClient("user6", TestUtil.getUser
                ("user6"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(SubscriptionIndividualApi.class);

        APIList apiList = apiCollectionApi.apisGet(3, 0, null, "", null);
        Assert.assertTrue(apiList.getCount() > 0);
        APIInfo retrievedAPIInfo = null;
        for (APIInfo apiInfo : apiList.getList()) {
            if ("restrictedapi1".equals(apiInfo.getName())) {
                retrievedAPIInfo = apiInfo;
                break;
            }
        }
        Assert.assertNotNull(retrievedAPIInfo);
        Assert.assertEquals(retrievedAPIInfo.getName(), "restrictedapi1");
        Assert.assertEquals(retrievedAPIInfo.getVersion(), "1.0.0");
        Assert.assertEquals(retrievedAPIInfo.getLifeCycleStatus(), "Published");
        Assert.assertEquals(retrievedAPIInfo.getContext(), "/restrictedapi1/1.0.0");
        APIIndividualApi apiIndividualApi = TestUtil.getStoreApiClient("user6", TestUtil.getUser("user6"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        API api = apiIndividualApi.apisApiIdGet(retrievedAPIInfo.getId(), null, null);
        Assert.assertNotNull(api);
        // Able to subscribe
        Application application = new Application().name("APIVisibilityTestCaseIT").throttlingTier("Unlimited");
        application = applicationIndividualApi.applicationsPost(application);
        applicationList.add(application);
        Subscription subscription = new Subscription().policy("Unlimited").applicationId(application.getApplicationId
                ()).apiIdentifier(api.getId());
        subscriptionIndividualApi.subscriptionsPost(subscription);

    }

    @Test(description = "user available in not restricted group", dependsOnMethods =
            {"testAPIVisibleToUserInRestrictedGroup"})
    public void testAPIVisibleOnNotSelectedGroup() throws AMIntegrationTestException {

        APICollectionApi apiCollectionApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser("user4"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        ApplicationIndividualApi applicationIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(ApplicationIndividualApi.class);
        SubscriptionIndividualApi subscriptionIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(SubscriptionIndividualApi.class);

        APIList apiList = apiCollectionApi.apisGet(3, 0, null, "", null);
        Assert.assertTrue(apiList.getCount() > 0);
        APIInfo retrievedAPIInfo = null;
        for (APIInfo apiInfo : apiList.getList()) {
            if ("restrictedapi1".equals(apiInfo.getName())) {
                retrievedAPIInfo = apiInfo;
                break;
            }
        }
        Assert.assertNull(retrievedAPIInfo);
        APIIndividualApi apiIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser("user4"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        try {
            apiIndividualApi.apisApiIdGet(this.apiInfo.getId(), null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof RestAPIException);
            RestAPIException restAPIException = (RestAPIException) e.getCause();
            Assert.assertEquals(restAPIException.getCode(), 404);
        }
        // not Able to subscribe
        Application application = new Application().name("APIVisibilityTestCaseIT").throttlingTier("Unlimited");
        application = applicationIndividualApi.applicationsPost(application);
        applicationList.add(application);
        Subscription subscription = new Subscription().policy("Unlimited").applicationId(application.getApplicationId
                ()).apiIdentifier(apiInfo.getId());
        try {
            subscriptionIndividualApi.subscriptionsPost(subscription);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof RestAPIException);
            RestAPIException restAPIException = (RestAPIException) e.getCause();
            Assert.assertEquals(restAPIException.getCode(), 404);
        }
    }

    @Test(description = "user available in not restricted group", dependsOnMethods =
            {"testAPIVisibleOnNotSelectedGroup"})
    public void testGetApiInAnonymousView() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getStoreApiClientWithoutUser().buildClient(APIIndividualApi.class);
        try {
            apiIndividualApi.apisApiIdGet(apiInfo.getId(), "", "");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getCause() instanceof RestAPIException);
            RestAPIException restAPIException = (RestAPIException)ex.getCause();
            Assert.assertEquals(restAPIException.getCode(), 404);

        }
    }

    @AfterClass
    public void destroy() throws AMIntegrationTestException {

        for (Application application : applicationList) {
            ApplicationIndividualApi applicationIndividualApi = TestUtil.getStoreApiClient(application.getSubscriber
                    (), TestUtil.getUser(application.getSubscriber()), AMIntegrationTestConstants.DEFAULT_SCOPES)
                    .buildClient
                            (ApplicationIndividualApi.class);
            applicationIndividualApi.applicationsApplicationIdDelete(application.getApplicationId(), "", "");
        }
    }

}
