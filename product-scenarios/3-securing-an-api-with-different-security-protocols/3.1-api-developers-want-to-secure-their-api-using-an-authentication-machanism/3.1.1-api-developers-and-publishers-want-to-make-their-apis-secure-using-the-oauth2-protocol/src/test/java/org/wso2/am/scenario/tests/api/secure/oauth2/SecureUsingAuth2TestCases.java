/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.scenario.tests.api.secure.oauth2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;

import java.util.ArrayList;
import java.util.List;

public class SecureUsingAuth2TestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private static final Log log = LogFactory.getLog(SecureUsingAuth2TestCases.class);
    private APIPublisherRestClient apiPublisher;
    private final String SUBSCRIBER_USERNAME = "subscriberUser2";
    private final String SUBSCRIBER_PASSWORD = "password@123";
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private final String API_DEVELOPER_USERNAME = "3.1.1-user";
    private final String API_DEVELOPER_PASSWORD = "password@3.1.1-user";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        apiStore = new APIStoreRestClient(storeURL);
        apiPublisher = new APIPublisherRestClient(publisherURL);

        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUserWithPublisherAndCreatorRole(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);

        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);
        apiPublisher.login(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD);
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        applicationsList.clear();
    }

    @Test(description = "1.1.1.1")
    public void testOAuth2Authorization() throws Exception {
        // create and publish sample API
        apiPublisher.developSampleAPI("swaggerFiles/pizzashack-swagger.json", API_DEVELOPER_USERNAME, backendEndPoint,
                true, "public");
        //TO DO
    }
}
