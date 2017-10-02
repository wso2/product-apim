package org.wso2.carbon.apimgt.rest.integration.tests.api.publisher;

import org.testng.Assert;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.ApiException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APIIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.EndpointCollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.EndpointIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.*;

import java.util.ArrayList;
import java.util.UUID;

public class TestUtils {

    private final APICollectionApi api = new APICollectionApi();
    private final EndpointCollectionApi endpointCollectionApi = new EndpointCollectionApi();
    private static String apiID;
    private final EndpointIndividualApi endpointIndividualApi = new EndpointIndividualApi();

    public String createApi(String apiName, String version, String context) throws ApiException {
        API body = new API();

        body.setName(apiName);
        body.setContext(context);
        body.setVersion(version);
        body.setDescription("This is the api description");
        body.setProvider("admin");
        body.setLifeCycleStatus("CREATED");
        body.setTransport(new ArrayList<String>() {{
            add("http");
        }});
        body.setCacheTimeout(100);
        body.setPolicies(new ArrayList<String>() {{
            add("Unlimited");
        }});
        body.setGatewayEnvironments("Production and Sandbox");
        body.setVisibility(API.VisibilityEnum.PUBLIC);
        body.setTags(new ArrayList<String>());
        body.setVisibleRoles(new ArrayList<String>());
        body.setVisibleTenants(new ArrayList<String>());
        body.setSequences(new ArrayList<Sequence>());
        body.setBusinessInformation(new APIBusinessInformation());
        body.setCorsConfiguration(new APICorsConfiguration());
        API response = api.apisPost(body);
        this.apiID = response.getId();
        return apiID;
    }

    public void deleteApi() throws ApiException {
        APIIndividualApi apiClient = new APIIndividualApi();
        APIList response = api.apisGet(10, 0, null, null);
        String ifMatch = null;
        String ifUnmodifiedSince = null;
        for(int i=0; i < response.getCount(); i++)
        {
            String apiId = response.getList().get(i).getId();
            apiClient.apisApiIdDelete(apiId, ifMatch, ifUnmodifiedSince);
        }
    }

    public String createEndPoint(String endPointName, String endPointConfig, String username, String password) throws ApiException
    {
        String ifNoneMatch = null;
        String ifModifiedSince = null;
        long MaxTps = 1000;
        EndPointEndpointSecurity endPointEndpointSecurity = new EndPointEndpointSecurity();
        endPointEndpointSecurity.setEnabled(false);
        endPointEndpointSecurity.setType("http");
        endPointEndpointSecurity.setUsername(username);
        endPointEndpointSecurity.setPassword(password);

        EndPoint endPoint = new EndPoint();
        endPoint.setId(UUID.randomUUID().toString());
        endPoint.setName(endPointName);
        endPoint.setEndpointSecurity(endPointEndpointSecurity);
        endPoint.setType("production");
        endPoint.setMaxTps(MaxTps);
        endPoint.setEndpointConfig(endPointConfig);
        EndPoint response = endpointCollectionApi.endpointsPost(endPoint, ifNoneMatch, ifModifiedSince);
        return response.getId();
    }

    public void deleteEndPoint(String endPointId) throws ApiException
    {
        String ifMatch = null;
        String ifUnmodifiedSince = null;
        endpointIndividualApi.endpointsEndpointIdDelete(endPointId, ifMatch, ifUnmodifiedSince);
    }
}
