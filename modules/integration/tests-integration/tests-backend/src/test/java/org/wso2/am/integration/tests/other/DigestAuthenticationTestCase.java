package org.wso2.am.integration.tests.other;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DigestAuthenticationTestCase extends APIMIntegrationBaseTest {

    private final String API_NAME = "TestDigestAuthentication";
    private final String API_CONTEXT = "testDigestAuthentication";
    private final String API_TAGS = "security, digest, authentication";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "TestDigestAuthenticationOfApi";
    private HashMap<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClient;
    private APIStoreRestClient apiStoreClient;
    private String providerName;
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;

    @BeforeClass(alwaysRun = true) public void initialize()
            throws APIManagerIntegrationTestException, XPathExpressionException, RemoteException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClient = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClient = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with admin
        apiPublisherClient.login(user.getUserName(), user.getPassword());

        //Login to API Store with admin
        apiStoreClient.login(user.getUserName(), user.getPassword());
        requestHeaders = new HashMap<String, String>();
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }

    @Test(groups = { "wso2.am" }, description = "Test Digest Auth Mediation") public void testDigestAuthMediation()
            throws Exception {
        String endpointUsername = "give my endpoint username here";
        String endpointPassword = "give my endpoint password here";

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT,
                API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setEndpointType("secured");
        apiCreationRequestBean.setEndpointAuthType("digestAuth");
        apiCreationRequestBean.setEpUsername(endpointUsername);
        apiCreationRequestBean.setEpPassword(endpointPassword);
        apiCreationRequestBean.setTier(APIThrottlingTier.UNLIMITED.getState());
        apiCreationRequestBean.setTiersCollection(APIThrottlingTier.UNLIMITED.getState());
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(APIThrottlingTier.UNLIMITED.getState());

        //create the API
        apiPublisherClient.addAPI(apiCreationRequestBean);

        //publish the API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, providerName,
                APILifeCycleState.PUBLISHED);
        updateRequest.setVersion(apiIdentifier.getVersion());
        apiPublisherClient.changeAPILifeCycleStatus(updateRequest);

        //Create Application
        apiStoreClient.addApplication(APPLICATION_NAME, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-a-test");

        //Subscribe to API
        String provider = user.getUserName();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, provider);
        subscriptionRequest.setApplicationName(APPLICATION_NAME);
        subscriptionRequest.setTier("Gold");
        subscriptionRequest.setVersion(apiIdentifier.getVersion());
        subscriptionRequest.setApplicationName(APPLICATION_NAME);
        apiStoreClient.subscribe(subscriptionRequest);

        //Application key generation
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APPLICATION_NAME);
        String responseString = apiStoreClient.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //Invoke the API
        requestHeaders.put("Accept", "application/xml");
        HttpResponse serviceResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT + "/1.0.0/customers/123"), requestHeaders);
        //This sends a GET request

        //Send POST,PUT,DELETE requests as well can put here at the same place.
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(serviceResponse.getData().contains("<Customer>"), "Response data mismatched when api invocation");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStoreClient.removeApplication(APPLICATION_NAME);
        super.cleanUp();
    }

    //This data provider might not be required
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{ TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

}



