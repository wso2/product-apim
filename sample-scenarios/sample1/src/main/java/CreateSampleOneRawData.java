/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.wso2.carbon.apimgt.samples.utils.SampleUtils;
import org.wso2.carbon.apimgt.samples.utils.TenantUtils;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to populate sample data to represent the sample one business scenario.
 */
public class CreateSampleOneRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";

    /**
     * This main method will be called when running sample one.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args) throws ApiException, IOException {
        createTenants();
        List<String> apiIds = createAPIs();
        publishAPIs(apiIds);
    }

    /**
     * This method is used to create API's
     *
     * @return A list of API Id's that was created.
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    private static List<String> createAPIs() throws ApiException, IOException {

        List<String> apiIds = new ArrayList<>();

        ArrayList<String> apiOneVisibleTenants = new ArrayList<>();
        apiOneVisibleTenants.add("finance.abc.com");
        ArrayList<String> apiOneTags = new ArrayList<>();
        apiOneTags.add("finance");
        apiIds.add(SampleUtils
                .createApi("Salary_details_API", "1.0.0", "/salaries", new ArrayList<>(), apiOneVisibleTenants,
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, apiOneTags));

        ArrayList<String> apiTwoVisibleTenants = new ArrayList<>();
        apiTwoVisibleTenants.add("core.abc.com");
        ArrayList<String> apiTwoTags = new ArrayList<>();
        apiTwoTags.add("stock");
        apiIds.add(SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stocks", new ArrayList<>(), apiTwoVisibleTenants,
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, apiTwoTags));

        ArrayList<String> apiThreeVisibleTenants = new ArrayList<String>();
        apiThreeVisibleTenants.add("operations.abc.com");
        ArrayList<String> apiThreeTags = new ArrayList<String>();
        apiThreeTags.add("maintenance");
        apiIds.add(SampleUtils
                .createApi("Maintenance_ask_API", "1.0.0", "/tasks", new ArrayList<>(), apiThreeVisibleTenants,
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, apiThreeTags));

        ArrayList<String> apiFourVisibleTenants = new ArrayList<>();
        apiFourVisibleTenants.add("finance.abc.com");
        apiFourVisibleTenants.add("core.abc.com");
        ArrayList<String> apiFourTags = new ArrayList<>();
        apiFourTags.add("employee");
        apiIds.add(SampleUtils
                .createApi("Employee_info_API", "1.0.0", "/empInfo", new ArrayList<>(), apiFourVisibleTenants,
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, apiFourTags));
        ArrayList<String> apiFiveTags = new ArrayList<String>();
        apiFiveTags.add("phone");
        apiIds.add(SampleUtils.createApi("Phone_prices_API", "1.0.0", "/mobilePrices", new ArrayList<>(),
                new ArrayList<String>(), API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, apiFiveTags));
        apiIds.add(SampleUtils.createApi("Pouch_prices_API", "1.0.0", "/pouchPrices", new ArrayList<>(),
                new ArrayList<String>(), API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, apiFiveTags));

        return apiIds;

    }

    /**
     * This method is used to create tenants
     */
    private static void createTenants() {
        TenantUtils.createTenant("john", "123123", "finance.abc.com", " John", "Smith", serviceEndpoint);
        TenantUtils.createTenant("tom", "123123", "core.abc.com", " Tom", "Smith", serviceEndpoint);
        TenantUtils.createTenant("bob", "123123", "operations.abc.com", " Bob", "Len", serviceEndpoint);
    }

    /**
     * This method is used to publish created API's.
     *
     * @param apiIdList A list of API Id's
     * @throws ApiException throws if an exception is thrown when publishing an API.
     */
    private static void publishAPIs(List apiIdList) throws ApiException {
        for (Object apiId : apiIdList) {
            SampleUtils.publishAPI((String) apiId);
        }
    }
}