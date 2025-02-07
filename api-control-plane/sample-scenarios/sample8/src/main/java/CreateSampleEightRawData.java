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
import org.wso2.carbon.apimgt.samples.utils.SampleUtils;
import org.wso2.carbon.apimgt.samples.utils.TenantUtils;
import org.wso2.carbon.apimgt.samples.utils.WebAppDeployUtils;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiResponse;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to populate sample data to represent the sample eight business scenario.
 */
public class CreateSampleEightRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";
    private static final String warFileName = "sample-data-backend";
    private static final String warFileLocation =
            System.getProperty("user.dir") + File.separator + "resources" + File.separator + "sample-data-backend.war";
    private static String clientTrustStore =
            System.getProperty("user.dir") + File.separator + ".." + File.separator + "repository" + File.separator
                    + "resources" + File.separator + "security" + File.separator + "client-truststore.jks";

    /**
     * This main method will be called when running sample eight.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args) throws ApiException, IOException {

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

        String oldApiId = SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stocks", new ArrayList<String>(), new ArrayList<String>(),
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, new ArrayList<String>());
        SampleUtils.publishAPI(oldApiId);
        SampleUtils.deprecateAPI(oldApiId);

        String newApiId =  SampleUtils.createNewAPIVersion("2.0.0", oldApiId);
        SampleUtils.publishAPI(newApiId);
    }

}