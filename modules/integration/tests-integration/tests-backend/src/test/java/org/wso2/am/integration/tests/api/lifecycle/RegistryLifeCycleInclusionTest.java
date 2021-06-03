/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.LifecycleHistoryDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.LifecycleStateDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegistryLifeCycleInclusionTest extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "RegistryLifeCycleInclusionAPI";
    private final String API_CONTEXT = "RegistryLifeCycleInclusionAPI";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String API_VERSION_2_0_0 = "2.0.0";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiID;
    private String copyApiID;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
    }

    @Test(groups = { "wso2.am" }, description = "Test LC tab of an published api")
    public void testAPIInfoLifecycleTabForPublishedAPI() throws Exception {

        //Create and publish API version 1.0.0
        APIDTO apidto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        apiID = apidto.getId();

        //check for LC state change buttons
        LifecycleStateDTO stateDTO = restAPIPublisher.getLifecycleStatusDTO(apiID);
        String[] expectedStates =
                { APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction(), APILifeCycleAction.BLOCK.getAction(),
                        APILifeCycleAction.DEMOTE_TO_CREATE.getAction(), APILifeCycleAction.DEPRECATE.getAction() };
        for (String state : expectedStates) {
            AtomicBoolean found = new AtomicBoolean(false);
            stateDTO.getAvailableTransitions().forEach((transit) -> {
                if (state.equalsIgnoreCase(transit.getEvent())) {
                    found.set(true);
                }
            });
            Assert.assertTrue(found.get());
        }

        //check LC history change
        LifecycleHistoryDTO historyDTO = restAPIPublisher.getLifecycleHistory(apiID);
        AtomicBoolean found = new AtomicBoolean(false);
        historyDTO.getList().forEach((item) -> {
            if (APILifeCycleState.CREATED.getState().equalsIgnoreCase(item.getPreviousState())
                    && APILifeCycleState.PUBLISHED.getState().equalsIgnoreCase(item.getPostState())) {
                found.set(true);
            }
        });
        Assert.assertTrue(found.get());
    }

    @Test(groups = { "wso2.am" }, description = "Test checklist item visibility for new version of an api",
            dependsOnMethods = "testAPIInfoLifecycleTabForPublishedAPI")
    public void testChecklistItemsVisibility() throws Exception {

        //Copy api to version 2.0.0
        APIDTO apidto = copyAPI(apiID, API_VERSION_2_0_0, restAPIPublisher);
        copyApiID = apidto.getId();

        //check for some of the LC state change buttons 
        LifecycleStateDTO stateDTO = restAPIPublisher.getLifecycleStatusDTO(copyApiID);
        Assert.assertEquals(stateDTO.getState(), APILifeCycleState.CREATED.getState());
        String[] expectedStates =
                { APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction(), APILifeCycleAction.PUBLISH.getAction() };
        for (String state : expectedStates) {
            AtomicBoolean found = new AtomicBoolean(false);
            stateDTO.getAvailableTransitions().forEach((transit) -> {
                if (state.equalsIgnoreCase(transit.getEvent())) {
                    found.set(true);
                }
            });
            Assert.assertTrue(found.get());
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test LC state change visibility in the LC tab in publisher",
            dependsOnMethods = "testChecklistItemsVisibility")
    public void testLCStateChangeVisibility() throws Exception {

        restAPIPublisher.changeAPILifeCycleStatus(copyApiID, APILifeCycleAction.PUBLISH.getAction());

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_2_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        LifecycleStateDTO stateDTO = restAPIPublisher.getLifecycleStatusDTO(copyApiID);
        Assert.assertEquals(stateDTO.getState(), APILifeCycleState.PUBLISHED.getState());
        String[] expectedStates =
                { APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction(), APILifeCycleAction.BLOCK.getAction(),
                        APILifeCycleAction.DEMOTE_TO_CREATE.getAction(), APILifeCycleAction.DEPRECATE.getAction() };
        for (String state : expectedStates) {
            AtomicBoolean found = new AtomicBoolean(false);
            stateDTO.getAvailableTransitions().forEach((transit) -> {
                if (state.equalsIgnoreCase(transit.getEvent())) {
                    found.set(true);
                }
            });
            Assert.assertTrue(found.get());
        }

        //check LC history change
        LifecycleHistoryDTO historyDTO = restAPIPublisher.getLifecycleHistory(apiID);
        AtomicBoolean found = new AtomicBoolean(false);
        historyDTO.getList().forEach((item) -> {
            if (APILifeCycleState.CREATED.getState().equalsIgnoreCase(item.getPreviousState())
                    && APILifeCycleState.PUBLISHED.getState().equalsIgnoreCase(item.getPostState())) {
                found.set(true);
            }
        });
        Assert.assertTrue(found.get());

        //change state to blocked
        restAPIPublisher.changeAPILifeCycleStatus(copyApiID, APILifeCycleAction.BLOCK.getAction());
        waitForAPIDeployment();

        //get the info page from the publisher
        stateDTO = restAPIPublisher.getLifecycleStatusDTO(copyApiID);
        Assert.assertEquals(stateDTO.getState(), APILifeCycleState.BLOCKED.getState());
        String[] expectedStates1 =
                { APILifeCycleAction.RE_PUBLISH.getAction(), APILifeCycleAction.DEPRECATE.getAction() };
        for (String state : expectedStates1) {
            AtomicBoolean found1 = new AtomicBoolean(false);
            stateDTO.getAvailableTransitions().forEach((transit) -> {
                if (state.equalsIgnoreCase(transit.getEvent())) {
                    found1.set(true);
                }
            });
            Assert.assertTrue(found1.get());
        }

        //check LC history change
        historyDTO = restAPIPublisher.getLifecycleHistory(copyApiID);
        AtomicBoolean found1 = new AtomicBoolean(false);
        historyDTO.getList().forEach((item) -> {
            if (APILifeCycleState.PUBLISHED.getState().equalsIgnoreCase(item.getPreviousState())
                    && APILifeCycleState.BLOCKED.getState().equalsIgnoreCase(item.getPostState())) {
                found1.set(true);
            }
        });
        Assert.assertTrue(found1.get());

        //change state to Deprecate
        restAPIPublisher.changeAPILifeCycleStatus(copyApiID, APILifeCycleAction.DEPRECATE.getAction());
        waitForAPIDeployment();

        stateDTO = restAPIPublisher.getLifecycleStatusDTO(copyApiID);
        Assert.assertEquals(stateDTO.getState(), APILifeCycleState.DEPRECATED.getState());
        String[] expectedStates2 = { APILifeCycleAction.RETIRE.getAction() };
        for (String state : expectedStates2) {
            AtomicBoolean found2 = new AtomicBoolean(false);
            stateDTO.getAvailableTransitions().forEach((transit) -> {
                if (state.equalsIgnoreCase(transit.getEvent())) {
                    found2.set(true);
                }
            });
            Assert.assertTrue(found2.get());
        }

        //check LC history change
        historyDTO = restAPIPublisher.getLifecycleHistory(copyApiID);
        AtomicBoolean found2 = new AtomicBoolean(false);
        historyDTO.getList().forEach((item) -> {
            if (APILifeCycleState.BLOCKED.getState().equalsIgnoreCase(item.getPreviousState())
                    && APILifeCycleState.DEPRECATED.getState().equalsIgnoreCase(item.getPostState())) {
                found2.set(true);
            }
        });
        Assert.assertTrue(found2.get());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiID);
        super.cleanUp();
    }
}
