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
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiResponse;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to populate sample data to represent the sample eight business scenario.
 */
public class CreateSampleEightRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";

    /**
     * This main method will be called when running sample one.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args) throws ApiException, IOException {
        String oldApiId = SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stocks", new ArrayList<>(), new ArrayList<>(),
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, new ArrayList<>());
        SampleUtils.publishAPI(oldApiId);
        SampleUtils.deprecateAPI(oldApiId);

        String newApiId =  SampleUtils.createNewAPIVersion("2.0.0", oldApiId);
        SampleUtils.publishAPI(newApiId);
    }

}