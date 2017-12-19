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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.apimgt.samples.utils.Constants;
import org.wso2.carbon.apimgt.samples.utils.HTTPSClientUtils;
import org.wso2.carbon.apimgt.samples.utils.SampleUtils;
import org.wso2.carbon.apimgt.samples.utils.TenantUtils;
import org.wso2.carbon.apimgt.samples.utils.ThrottlingUtils;
import org.wso2.carbon.apimgt.samples.utils.UserManagementUtils;
import org.wso2.carbon.apimgt.samples.utils.WebAppDeployUtils;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.ThrottleLimit;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.ApplicationKey;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.ApplicationKeyGenerateRequest;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.Subscription;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to populate sample data to represent the sample nine business scenario.
 */
public class CreateSampleNineRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String gatewayPort = "8243";
    private static final String gatewayHost = "localhost";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";
    private static final String getGatewayEndpoint = "https://" + gatewayHost + ":" + gatewayPort;
    private static final String warFileName = "sample-data-backend";
    private static final String warFileLocation =
            System.getProperty("user.dir") + File.separator + "resources" + File.separator + "sample-data-backend.war";
    private static String clientTrustStore =
            System.getProperty("user.dir") + File.separator + ".." + File.separator + "repository" + File.separator
                    + "resources" + File.separator + "security" + File.separator + "client-truststore.jks";

    /**
     * This main method will be called when running sample nine.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args)
            throws ApiException, IOException, org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException,
            InterruptedException, UserAdminUserAdminException,
            org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException {

        if (StringUtils.isEmpty(System.getProperty(Constants.JAVAX_NET_SSL_TRUST_STORE))) {
            System.setProperty(Constants.JAVAX_NET_SSL_TRUST_STORE, clientTrustStore);
        }
        if (StringUtils.isEmpty(System.getProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_PASSWORD))) {
            System.setProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_PASSWORD, Constants.WSO2_CARBON);
        }
        if (StringUtils.isEmpty(System.getProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_TYPE))) {
            System.setProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_TYPE, Constants.JKS);
        }

        System.out.println("Deploying sample back end");
        WebAppDeployUtils.deployWebApp(serviceEndpoint, "admin", "admin", warFileLocation, warFileName);

        String tenantDomain = "finance.abc.com";
        String tenantAdminUsername = "John";
        String tenantAdminPassword = "123123";

        // Create a new user
        try {
            System.out.println("Creating user tom");
            UserManagementUtils
                    .addUser("tom", "123123", serviceEndpoint, new String[] { "Internal/subscriber" }, "admin",
                            "admin");
        } catch (UserAdminUserAdminException | RemoteException e) {
            // This exception occurs when the user is previously created. Ignoring the exception to run the sample multiple times to the sam instance.
        }

        // Create a tenant
        System.out.println("Creating tenant finance.abc.com");
        TenantUtils.createTenant(tenantAdminUsername, tenantAdminPassword, tenantDomain, tenantAdminUsername, "Smith",
                serviceEndpoint);

        // Create advance throttle policies for super tenants.
        System.out.println("Creating advance policy 5KPerMin for finance.abc.com");
        ThrottlingUtils
                .addAdvanceThrottlePolicyForTenants("5KPerMin", "5KPerMin", "Allows 5000 requests per minute", "min", 1,
                        100000L, ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT, 0, null, tenantDomain, tenantAdminUsername,
                        tenantAdminPassword);

        // Create advance throttle policies for super tenants.
        System.out.println("Creating advance policy 5KPerMin for super tenant");
        ThrottlingUtils
                .addAdvanceThrottlePolicy("5KPerMin", "5KPerMin", "Allows 5000 requests per minute", "min", 1, 100000L,
                        ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT, 0, null);

        // Create advance throttle policies for super tenants.
        System.out.println("Creating advance policy 100KPerMin for super tenant");
        ThrottlingUtils
                .addAdvanceThrottlePolicy("100KPerMin", "100KPerMin", "Allows 100000 requests per minute", "min", 1,
                        100000L, ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT, 0, null);

        System.out.println("Creating advance policy 100KKBPerMin for super tenant");
        ThrottlingUtils
                .addAdvanceThrottlePolicy("100KKBPerMin", "100KKBPerMin", "Allows 100000 kilo bytes per minute", "min",
                        1, 0, ThrottleLimit.TypeEnum.BANDWIDTHLIMIT, 100000L, "KB");

        System.out.println("Creating Salary_details_API API for finance.abc.com");
        String apiIdOne = SampleUtils
                .createApiForTenant("Salary_details_API", "1.0.0", "/t/" + tenantDomain + "/salaries",
                        API.VisibilityEnum.PUBLIC, new ArrayList<String>(), new ArrayList<String>(),
                        API.SubscriptionAvailabilityEnum.CURRENT_TENANT, hostname, port, new ArrayList<String>(),
                        tenantDomain, tenantAdminUsername, tenantAdminPassword);
        System.out.println("Publishing Salary_details_API API for finance.abc.com");
        SampleUtils.publishAPI(apiIdOne, tenantDomain, tenantAdminUsername, tenantAdminPassword);

        // Create the API.
        System.out.println("Creating Mobile_stock_API API for super tenant");
        String apiIdTwo = SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stocks", new ArrayList<String>(), new ArrayList<String>(),
                        API.SubscriptionAvailabilityEnum.CURRENT_TENANT, hostname, port, new ArrayList<String>());
        // Publish the API.
        System.out.println("Publishing Mobile_stock_API API for super tenant");
        SampleUtils.publishAPI(apiIdTwo);
        System.out.println("Waiting two seconds for API to be deployed to the gateway");
        Thread.sleep(2000);

        // Create Application
        System.out.println("Creating Application One");
        String applicationIdOne = SampleUtils
                .createApplication("Application_one", "This a new application created", "Unlimited");

        // Create grant types
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("refresh_token");
        grantTypes.add("password");
        grantTypes.add("client_credentials");

        // Create allow roles
        ArrayList<String> allowedDomain = new ArrayList<>();
        allowedDomain.add("ALL");

        // Generate Keys for the application one
        System.out.println("Generating keys for Application One");
        ApplicationKey applicationKey = SampleUtils
                .generateKeys(applicationIdOne, "7200", null, ApplicationKeyGenerateRequest.KeyTypeEnum.PRODUCTION,
                        new ArrayList<String>(), allowedDomain, grantTypes);
        String accessToken = applicationKey.getToken().getAccessToken();

        // Create subscription for application one
        System.out.println("Creating a subscription for keys for Application One");
        SampleUtils.createSubscription(apiIdTwo, applicationIdOne, "Unlimited", Subscription.StatusEnum.UNBLOCKED);

        Map<String, String> requestHeadersOne = new HashMap<>();
        requestHeadersOne.put("Authorization", "Bearer " + accessToken);

        // Invoke the API for 20 times with application one
        System.out.print("Invoking API using Application One to get analytics data");
        for (int i = 1; i <= 20; i++) {
            HTTPSClientUtils.doGet(getGatewayEndpoint + "/stocks/1.0.0/stock/1", requestHeadersOne);
            System.out.print(".");
        }
        System.out.println("");

        // Create Application two
        System.out.println("Creating a subscription for keys for Application Two");
        String applicationIdTwo = SampleUtils
                .createApplication("Application_two", "This a new application created", "Unlimited");

        // Generate Keys for the application two
        ApplicationKey applicationKeyTwo = SampleUtils
                .generateKeys(applicationIdTwo, "7200", null, ApplicationKeyGenerateRequest.KeyTypeEnum.PRODUCTION,
                        new ArrayList<String>(), allowedDomain, grantTypes);
        String accessTokenTwo = applicationKeyTwo.getToken().getAccessToken();

        // Create subscription for application one
        System.out.println("Creating a subscription for keys for Application Two");
        SampleUtils.createSubscription(apiIdTwo, applicationIdTwo, "Unlimited", Subscription.StatusEnum.UNBLOCKED);

        Map<String, String> requestHeadersTwo = new HashMap<>();
        requestHeadersTwo.put("Authorization", "Bearer " + accessTokenTwo);

        System.out.print("Invoking API using Application Two to get analytics data");
        // Invoke the API for 20 times with application Two
        for (int i = 1; i <= 20; i++) {
            HTTPSClientUtils.doGet(getGatewayEndpoint + "/stocks/1.0.0/stock/1", requestHeadersTwo);
            System.out.print(".");
        }
    }

}