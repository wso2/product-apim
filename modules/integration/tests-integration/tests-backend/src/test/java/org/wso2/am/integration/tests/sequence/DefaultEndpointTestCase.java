package org.wso2.am.integration.tests.sequence;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import javax.activation.DataHandler;
import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class DefaultEndpointTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "AddNewMediationAndInvokeAPITest";
    private final String API_CONTEXT = "AddNewMediationAndInvokeAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AddNewMediationAndInvokeAPI";
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifier;
    private String accessToken;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        String providerName = user.getUserName();



        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName);


        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.LARGE, "", "");
        accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the default endpoint")
    public void testAPIInvocationAfterAddingNewMediation() throws Exception  {
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        String apiUser = gatewayContextMgt.getContextTenant().getContextUser().getUserName();

        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);

        ResourceAdminServiceClient resourceAdminServiceStub =
                new ResourceAdminServiceClient(publisherContext.getContextUrls().getBackEndUrl(), gatewaySessionCookie);

        boolean isResourceAdded = resourceAdminServiceStub.addResource(
                "/_system/governance/apimgt/applicationdata/provider" +
                File.separator + apiUser + File.separator + API_NAME + File.separator + API_VERSION_1_0_0 + File.separator +
                //"/admin/AddNewMediationAndInvokeAPITest/1.0.0/" +
                "in/default_endpoint.xml",
                "application/xml",
                "xml files",
                new DataHandler(new URL("file:///" + getAMResourceLocation() + File.separator + "sequence"
                                        + File.separator + "default_endpoint.xml")));

        assertTrue(isResourceAdded, "Adding Mediation Sequence File failed");

        apiCreationRequestBean.setInSequence("default_endpoint");
        apiPublisherClientUser1.updateAPI(apiCreationRequestBean);

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization" , "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response.getHeaders("Content-Type")[0].getValue(), "application/json; charset=utf-8");
    }


}
