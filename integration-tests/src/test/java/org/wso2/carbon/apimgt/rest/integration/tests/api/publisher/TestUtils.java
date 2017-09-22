package org.wso2.carbon.apimgt.rest.integration.tests.api.publisher;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.ApiException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APIIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.*;

import java.util.ArrayList;

public class TestUtils {

    private final APICollectionApi api = new APICollectionApi();
    private static String apiID;

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
}
