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
import org.wso2.carbon.apimgt.samples.utils.WebAppDeployUtils;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.ApplicationKey;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.ApplicationKeyGenerateRequest;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.Subscription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to populate sample data to represent the sample four business scenario.
 */
public class CreateSampleFourRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";
    private static final String warFileName = "sample-data-backend";
    private static final String warFileLocation =
            System.getProperty("user.dir") + File.separator + "resources" + File.separator + "sample-data-backend.war";
    private static String clientTrustStore =
            System.getProperty("user.dir") + File.separator + ".." + File.separator + "repository" + File.separator
                    + "resources" + File.separator + "security" + File.separator + "client-truststore.jks";
    private static final String gatewayPort = "8243";
    private static final String gatewayHost = "localhost";
    private static final String getGatewayEndpoint = "https://" + gatewayHost + ":" + gatewayPort;

    /**
     * This main method will be called when running sample one.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args)
            throws ApiException, IOException, org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException {

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
        List<String> apiIds = createAPIs();
        publishAPIs(apiIds);

        String accessTokenOne = subscribeToAPI(apiIds.get(0), "Application_one");
        invokeAPI(accessTokenOne, 1, "/salariesSecure/1.0.0/salary/1");

        String accessTokenTwo = subscribeToAPIWithNewScope(apiIds.get(1), "Application_two");
        invokeAPI(accessTokenTwo, 1, "/salariesSecure/1.0.0/salary/1");

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
                .createApi("Salary_details_API", "1.0.0", "/salariesSecure", new ArrayList<String>(), apiOneVisibleTenants,
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, apiOneTags));

        ArrayList<String> apiFiveTags = new ArrayList<String>();
        apiFiveTags.add("phone");
        apiIds.add(SampleUtils.createApi("Phone_prices_API", "1.0.0", "/mobilePrices", new ArrayList<String>(),
                new ArrayList<String>(), API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, apiFiveTags));
        return apiIds;

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


    private static String subscribeToAPI(String apiIdOne, String applicationName)
            throws org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException {
        // Create Application
        System.out.println("Creating Application One");
        String applicationIdOne = SampleUtils
                .createApplication(applicationName, "This a new application created", "Unlimited");

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
        SampleUtils.createSubscription(apiIdOne, applicationIdOne, "Unlimited", Subscription.StatusEnum.UNBLOCKED);
        return accessToken;
    }


    private static String subscribeToAPIWithNewScope(String apiIdOne, String applicationName)
            throws org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException {
        // Create Application
        System.out.println("Creating Application Two");
        String applicationIdOne = SampleUtils
                .createApplication(applicationName, "This a new application created", "Unlimited");

        // Create grant types
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("refresh_token");
        grantTypes.add("password");
        grantTypes.add("client_credentials");

        // Create allow roles
        ArrayList<String> allowedDomain = new ArrayList<>();
        allowedDomain.add("ALL");

        //Allowed scopes
        ArrayList<String> allowedSopes = new ArrayList<>();
        allowedSopes.add("new_scope");

        // Generate Keys for the application one
        System.out.println("Generating keys for Application One");
        ApplicationKey applicationKey = SampleUtils
                .generateKeys(applicationIdOne, "7200", null, ApplicationKeyGenerateRequest.KeyTypeEnum.PRODUCTION,
                        allowedSopes, allowedDomain, grantTypes);
        String accessToken = applicationKey.getToken().getAccessToken();

        // Create subscription for application one
        System.out.println("Creating a subscription for keys for Application Two");
        SampleUtils.createSubscription(apiIdOne, applicationIdOne, "Unlimited", Subscription.StatusEnum.UNBLOCKED);
        return accessToken;
    }


    private static void invokeAPI(String accessToken, int invocationNumber , String apiResourcePath)
            throws IOException {
        Map<String, String> requestHeadersOne = new HashMap<>();
        requestHeadersOne.put("Authorization", "Bearer " + accessToken);

        // Invoke the API for 20 times with application one
        System.out.print("Invoking API using Application One to get analytics data");
        for (int i = 1; i <= invocationNumber; i++) {
            HTTPSClientUtils.doGet(getGatewayEndpoint + apiResourcePath, requestHeadersOne);
            System.out.print(".");
        }
        System.out.println("");
    }
}