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
import org.wso2.carbon.apimgt.samples.utils.LifecycleUtils;
import org.wso2.carbon.apimgt.samples.utils.SampleUtils;
import org.wso2.carbon.apimgt.samples.utils.ServerUtils;
import org.wso2.carbon.apimgt.samples.utils.WebAppDeployUtils;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.ApplicationKeyGenerateRequest;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.Subscription;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceExceptionException;
import org.wso2.carbon.server.admin.stub.ServerAdminException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is used to populate sample data to represent the sample Seven business scenario - API Lifecycle Management.
 */
public class CreateSampleSevenRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";
    private static final String warFileName = "sample-data-backend";
    private static final String warFileLocation =
            System.getProperty("user.dir") + File.separator + "resources" + File.separator + "sample-data-backend.war";
    private static final String clientTrustStore =
            System.getProperty("user.dir") + File.separator + ".." + File.separator + "repository" + File.separator
                    + "resources" + File.separator + "security" + File.separator + "client-truststore.jks";

    /**
     * This main method will be called when running sample seven.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args) throws ApiException, IOException, ServerAdminException, InterruptedException,
            LifeCycleManagementServiceExceptionException,
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

        if (StringUtils.isEmpty(System.getProperty(Constants.CARBON_HOME))) {
            System.setProperty(Constants.CARBON_HOME, Constants.CARBON_HOME_VALUE);
        }

        // Deploying sample backend.
        WebAppDeployUtils.deployWebApp(serviceEndpoint, "admin", "admin", warFileLocation, warFileName);
        System.out.println("Creating Salary_details_API API");

        //Creating API in PUBLISHED state
        String apiIdOne = SampleUtils
                .createApi("Salary_details_API", "1.0.0", "/salary", new ArrayList<String>(), new ArrayList<String>(),
                        API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<String>());
        System.out.println("Publishing Salary_details_API API");
        if (StringUtils.isNotEmpty(apiIdOne))
            SampleUtils.publishAPI(apiIdOne);

        //Creating API in CREATED state
        System.out.println("Creating Employee_info_API API");
        SampleUtils.createApi("Employee_info_API", "1.0.0", "/empInfo", new ArrayList<String>(), new ArrayList<String>(),
                API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<String>());

        //Creating API in DEPRECATED state
        System.out.println("Creating Mobile_stock_API API");
        String apiIdThree = SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stock", new ArrayList<String>(), new ArrayList<String>(),
                        API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<String>());
        System.out.println("Deprecating Mobile_stock_API API");
        if (StringUtils.isNotEmpty(apiIdThree))
            SampleUtils.publishAPI(apiIdThree);
        if (StringUtils.isNotEmpty(apiIdThree))
            SampleUtils.deprecateAPI(apiIdThree);

        //Creating the new version of the Mobile stock API which is in PUBLISHED STATE
        String apiIdFour = null;
        System.out.println("Creating new version of Mobile_stock_API API");
        if (StringUtils.isNotEmpty(apiIdThree)) {
            apiIdFour = SampleUtils.createNewAPIVersion("2.0.0", apiIdThree);
            System.out.println("Publishing new version of Mobile_stock_API API");
            if (StringUtils.isNotEmpty(apiIdFour))
                SampleUtils.publishAPI(apiIdFour);
        }

        //Creating a new API for sales_promotions which is in blocking state
        System.out.println("Creating Sales_promotions_API 2.0.0 API");
        String apiIdFive = SampleUtils
                .createApi("Sales_promotions_API", "2.0.0", "/promo", new ArrayList<String>(), new ArrayList<String>(),
                        API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<String>());
        System.out.println("Blocking Sales_promotions_API 2.0.0 API");
        if (StringUtils.isNotEmpty(apiIdFive))
            SampleUtils.publishAPI(apiIdFive);
        if (StringUtils.isNotEmpty(apiIdFive))
            SampleUtils.blockAPI(apiIdFive);

        System.out.println("Adding custom state(REJECT) to API lifecycle");
        // Updating the lifecycle configuration
        LifecycleUtils.updateLifecycle(serviceEndpoint, Constants.ADMIN_USERNAME, Constants.ADMIN_PASSWORD,
                Constants.API_LIFECYCLE);
        ServerUtils.restartServer(serviceEndpoint, Constants.ADMIN_USERNAME, Constants.ADMIN_PASSWORD);
        ServerUtils.waitForServerStartup(serviceEndpoint, Constants.ADMIN_USERNAME, Constants.ADMIN_PASSWORD);

        //Creating ad API for  for sales_promotions which is in RETIRED state
        System.out.println("Creating Sales_promotions_API 1.0.0 API");
        if (StringUtils.isNotEmpty(apiIdFive)) {
            String apiIdSix = SampleUtils.createNewAPIVersion("1.0.0", apiIdFive);
            System.out.println("Rejecting Sales_promotions_API 1.0.0 API");
            if (StringUtils.isNotEmpty(apiIdSix))
                SampleUtils.publishAPI(apiIdSix);
            if (StringUtils.isNotEmpty(apiIdSix))
                SampleUtils.rejectAPI(apiIdSix);
        }

        System.out.println("Creating Application(Application_one) in store");
        // Create Application
        String applicationIdOne = SampleUtils
                .createApplication("Application_one", "This a new application created", "Unlimited");
        if (StringUtils.isNotEmpty(applicationIdOne)) {

            // Create grant types
            ArrayList<String> grantTypes = new ArrayList<>();
            grantTypes.add("refresh_token");
            grantTypes.add("password");
            grantTypes.add("client_credentials");

            // Create allow roles
            ArrayList<String> allowedDomain = new ArrayList<>();
            allowedDomain.add("ALL");

            // Generate Keys for the application one
            SampleUtils
                    .generateKeys(applicationIdOne, "7200", null, ApplicationKeyGenerateRequest.KeyTypeEnum.PRODUCTION,
                            new ArrayList<String>(), allowedDomain, grantTypes);

            System.out.println("Salary_details_API subscribing to Application_one");
            // Create subscription for application one
            if (StringUtils.isNotEmpty(apiIdOne))
                SampleUtils
                        .createSubscription(apiIdOne, applicationIdOne, "Unlimited", Subscription.StatusEnum.UNBLOCKED);
            System.out.println("Sales_promotions_API subscribing to Application_one");
            if (StringUtils.isNotEmpty(apiIdFour))
                SampleUtils.createSubscription(apiIdFour, applicationIdOne, "Unlimited",
                        Subscription.StatusEnum.UNBLOCKED);
        }

        System.out.println("Sample scenario seven data populated successfully.");
    }
}