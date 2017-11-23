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
import org.wso2.carbon.apimgt.samples.utils.ThrottlingUtils;
import org.wso2.carbon.apimgt.samples.utils.UserManagementUtils;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.ThrottleLimit;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is used to populate sample data to represent the sample nine business scenario.
 */
public class CreateSampleNineRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";

    /**
     * This main method will be called when running sample nine.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args)
            throws ApiException, IOException, org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException,
            InterruptedException, UserAdminUserAdminException {

        String tenantDomain = "finance.abc.com";
        String tenantAdminUsername = "John";
        String tenantAdminPassword = "123123";

        // Create a tenant
        TenantUtils.createTenant(tenantAdminUsername, tenantAdminPassword, tenantDomain, tenantAdminUsername, "Smith",
                serviceEndpoint);

        // Create advance throttle policies for super tenants.
        ThrottlingUtils
                .addAdvanceThrottlePolicyForTenants("5KPerMin", "5KPerMin", "Allows 5000 requests per minute", "min", 1,
                        100000L, ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT, 0, null, tenantDomain, tenantAdminUsername,
                        tenantAdminPassword);


        // Create advance throttle policies for super tenants.
        ThrottlingUtils
                .addAdvanceThrottlePolicy("5KPerMin", "5KPerMin", "Allows 5000 requests per minute", "min", 1, 100000L,
                        ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT, 0, null);
        String apiIdOne = SampleUtils
                .createApiForTenant("Salary_details_API", "1.0.0", "/t/" + tenantDomain + "/stocks",
                        API.VisibilityEnum.PUBLIC, new ArrayList<>(), new ArrayList<>(),
                        API.SubscriptionAvailabilityEnum.CURRENT_TENANT, hostname, port, new ArrayList<>(),
                        tenantDomain, tenantAdminUsername, tenantAdminPassword);

        SampleUtils.publishAPI(apiIdOne, tenantDomain, tenantAdminUsername, tenantAdminPassword);

        // Create advance throttle policies for super tenants.
        ThrottlingUtils
                .addAdvanceThrottlePolicy("100KPerMin", "100KPerMin", "Allows 100000 requests per minute", "min", 1,
                        100000L, ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT, 0, null);

        ThrottlingUtils
                .addAdvanceThrottlePolicy("100KKBPerMin", "100KKBPerMin", "Allows 100000 kilo bytes per minute", "min",
                        1, 0, ThrottleLimit.TypeEnum.BANDWIDTHLIMIT, 100000L, "KB");
        // Create the API.
        String apiIdTwo = SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stocks", new ArrayList<>(), new ArrayList<>(),
                        API.SubscriptionAvailabilityEnum.CURRENT_TENANT, hostname, port, new ArrayList<>());
        // Publish the API.
        SampleUtils.publishAPI(apiIdTwo);

        UserManagementUtils
                .addUser("tom", "123123", serviceEndpoint, new String[] { "Internal/subscriber" }, "admin", "admin");

    }

}