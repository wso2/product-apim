/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.rest.integration.tests.store;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.ApplicationIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.SubscriptionCollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.SubscriptionIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Subscription;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.SubscriptionList;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

public class APISubscriptionTestCaseIT {

    private Application application;
    private Subscription subscription;
    ApplicationIndividualApi applicationIndividualApi;
    SubscriptionIndividualApi subscriptionIndividualApi;

    @BeforeClass
    public void init() throws AMIntegrationTestException {
        applicationIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(ApplicationIndividualApi.class);
        subscriptionIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(SubscriptionIndividualApi.class);
    }

    @Test
    public void testCreateApplication() {
        application = new Application().name("testApplication1").description("This is a Test App").throttlingTier
                ("Unlimited");
        application = applicationIndividualApi.applicationsPost(application);
    }

    @Test(description = "Try to create Application from Creator user")
    public void testCreateApplicationWithInSufficientPermissionNegative() throws AMIntegrationTestException {
        ApplicationIndividualApi applicationIndividualApi = TestUtil.getStoreApiClient("user1", TestUtil.getUser
                ("user1"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(ApplicationIndividualApi.class);
        Application application2 = new Application().name("testApplication1").description("This is a Test App")
                .throttlingTier("Unlimited");
        try {
            applicationIndividualApi.applicationsPost(application2);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test(dependsOnMethods = {"testCreateApplication"})
    public void testCreateSubscription() {
        subscription = new Subscription().apiIdentifier(TestUtil.getApi("baseapi1").getId())
                .applicationId(application.getApplicationId()).policy("Gold");
        subscription = subscriptionIndividualApi.subscriptionsPost(subscription);
    }

    @Test(dependsOnMethods = "testCreateSubscription")
    public void testGetAllSubscriptionsByApplication() throws AMIntegrationTestException {
        SubscriptionCollectionApi subscriptionCollectionApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(SubscriptionCollectionApi.class);
        SubscriptionList subscriptionList = subscriptionCollectionApi.subscriptionsGet("", application
                .getApplicationId(), "", 0, 10, "");
        Assert.assertNotNull(subscriptionList);
        Assert.assertEquals(subscriptionList.getCount().intValue(), 1);
        Subscription subscription = subscriptionList.getList().get(0);
        Assert.assertEquals(subscription.getApiIdentifier(), this.subscription.getApiIdentifier());
        Assert.assertEquals(subscription.getPolicy(), this.subscription.getPolicy());
    }

    @Test(dependsOnMethods = "testCreateSubscription")
    public void testGetAllSubscriptionsByAPIId() throws AMIntegrationTestException {
        SubscriptionCollectionApi subscriptionCollectionApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(SubscriptionCollectionApi.class);
        SubscriptionList subscriptionList = subscriptionCollectionApi.subscriptionsGet(subscription.getApiIdentifier
                (), "", "", 0, 10, "");
        Assert.assertNotNull(subscriptionList);
        Assert.assertEquals(subscriptionList.getCount().intValue(), 1);
        Subscription subscription = subscriptionList.getList().get(0);
        Assert.assertEquals(subscription.getApplicationId(), this.subscription.getApplicationId());
        Assert.assertEquals(subscription.getPolicy(), this.subscription.getPolicy());
    }

    @AfterClass
    public void destroy() {
        if (subscriptionIndividualApi.subscriptionsSubscriptionIdGet(subscription.getSubscriptionId(), "", "") !=
                null) {
            subscriptionIndividualApi.subscriptionsSubscriptionIdDelete(subscription.getSubscriptionId(), "", "");
        }
        if (applicationIndividualApi.applicationsApplicationIdGet(application.getApplicationId(), "", "") != null) {
            applicationIndividualApi.applicationsApplicationIdDelete(application.getApplicationId(), "", "");
        }
    }
}
