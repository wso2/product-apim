package org.wso2.am.integration.tests.api.revision;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class APIRevisionTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIRevisionTestCase.class);
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = Response.Status.UNAUTHORIZED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE =
            Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS = 429; // Define manually since value is not available in enum
    protected static final int HTTP_RESPONSE_CODE_FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();
    private final String API_NAME = "RevisionTestAPI";
    private final String API_CONTEXT = "revisiontestapi";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId;
    private String revisionUUID;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

    }

    @Test(groups = {"wso2.am"}, description = "API Revision create test case")
    public void testAddingAPIRevision() throws Exception {
        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();

        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiId);

        //Verify the API in API Publisher
        HttpResponse apiDto = restAPIPublisher.getAPI(apiResponse.getData());
        assertTrue(StringUtils.isNotEmpty(apiDto.getData()),
                "Added Api is not available in APi Publisher. API ID " + apiId);

        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");
        //Add the API Revision using the API publisher.
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);

        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Check the availability of API Revision in publisher before deploying.",
            dependsOnMethods = "testAddingAPIRevision")
    public void testGetAPIRevisions() throws Exception {
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision :revisionList) {
            revisionUUID = revision.getString("id");
        }
        assertNotNull(revisionUUID, "Unable to retrieve revision UUID");
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments",
            dependsOnMethods = "testGetAPIRevisions")
    public void testDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("Production and Sandbox");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" +apiRevisionsDeployResponse.getData());
//        List<JSONObject> deploymentList = new ArrayList<>();
//        JSONArray jsonArray = new JSONArray(apiRevisionsDeployResponse.getData());
//
//        for (int i = 0, l = jsonArray.length(); i < l; i++) {
//            deploymentList.add(jsonArray.getJSONObject(i));
//        }
//        String deploymentName = null;
//        for (JSONObject deployment :deploymentList) {
//            deploymentName = deployment.getString("name");
//        }
//        assertNotNull(deploymentName, "Unable to retrieve deployed deployment name");
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision to gateway environments",
            dependsOnMethods = "testDeployAPIRevisions")
    public void testUnDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("Production and Sandbox");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Revisions:" + apiRevisionsUnDeployResponse.getData());

    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API using created API Revision",
            dependsOnMethods = "testUnDeployAPIRevisions")
    public void testRestoreAPIRevision() throws Exception {
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to resotre API Revisions:" + apiRevisionsRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API using created API Revision",
            dependsOnMethods = "testRestoreAPIRevision")
    public void testDeleteAPIRevision() throws Exception {
        HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API Revisions:" + apiRevisionsDeleteResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

    }
}
