/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.samples;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PizzaShackAPITestCase extends APIMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private APICreationRequestBean resorcet;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);


        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "Pizzashack Test")

    public void testPizzashackApiSample() throws Exception {

        List<APIResourceBean> resourceBeanList=new ArrayList<APIResourceBean>();

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean("PizzaAPI", "pizzashack","1.0.0","admin",
                new URL("http://localhost:9766/pizzashack-api-1.0.0/api/"));

        apiCreationRequestBean.setThumbUrl("/home/bhagya/WS/Pizza_Shack_Logo.jpeg");
        apiCreationRequestBean.setDescription("Pizza API:Allows to manage pizza orders (create, update, retrieve orders)");
        apiCreationRequestBean.setTags("pizza, order, pizza-menu");
        apiCreationRequestBean.setResourceCount("4");

        resourceBeanList.add(new APIResourceBean("GET","Application & Application User","Unlimited","/menu"));
        resourceBeanList.add(new APIResourceBean("POST","Application & Application User","Unlimited","/order"));
        resourceBeanList.add(new APIResourceBean("GET","Application & Application User","Unlimited","/order/{orderid}"));
        resourceBeanList.add(new APIResourceBean("GET","Application & Application User","Unlimited","/delivery"));
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        apiCreationRequestBean.setTier("Unlimited");
        apiCreationRequestBean.setTiersCollection("Unlimited");

        apiPublisher.addAPI(apiCreationRequestBean);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest("PizzaAPI", publisherContext
                        .getContextTenant().getContextUser().getUserName(),
                        APILifeCycleState.PUBLISHED
                );
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiStore.addApplication("PizzaShack", "Unlimited", "", "");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("PizzaAPI",
                storeContext.getContextTenant()
                        .getContextUser()
                        .getUserName()
        );
        subscriptionRequest.setApplicationName("PizzaShack");
        apiStore.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("PizzaShack");
                String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        Thread.sleep(2000);

        HttpResponse pizzaShackResponse = HttpRequestUtil.doGet(
                gatewayUrlsWrk.getWebAppURLNhttp() + "pizzashack/1.0.0/menu", requestHeaders);
        assertEquals(pizzaShackResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        System.out.println("My Response Code is "+pizzaShackResponse.getResponseCode());

        assertTrue(pizzaShackResponse.getData().contains("BBQ Chicken Bacon"),
                "Response data mismatched when api invocation");
        assertTrue(pizzaShackResponse.getData().contains("Grilled white chicken"),
                "Response data mismatched when api invocation");
        assertTrue(pizzaShackResponse.getData().contains("Chicken Parmesan"),
                "Response data mismatched when api invocation");
        assertTrue(pizzaShackResponse.getData().contains("Tuscan Six Cheese"),
                "Response data mismatched when api invocation");
        assertTrue(pizzaShackResponse.getData().contains("Asiago and Fontina"),
                "Response data mismatched when api invocation");


    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("PizzaShack");


    }
}