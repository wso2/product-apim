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
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is used to populate sample data to represent the sample Seven business scenario - API Lifecycle Management.
 */
public class CreateSampleSevenRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";

    /**
     * This main method will be called when running sample one.
     *
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    public static void main(String[] args) throws ApiException, IOException {
        //Creating API in PUBLISHED state
        String apiIdOne = SampleUtils
                .createApi("Salary_details_API", "1.0.0", "/salaries", new ArrayList<>(), new ArrayList<>(),
                        API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<>());
        SampleUtils.publishAPI(apiIdOne);

        //Creating API in CREATED state
        SampleUtils.createApi("Employee_info_API", "1.0.0", "/empInfo", new ArrayList<>(), new ArrayList<>(),
                API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<>());

        //Creating API in DEPRECATED state
        String apiIdThree = SampleUtils
                .createApi("Mobile_stock_API", "1.0.0", "/stocks", new ArrayList<>(), new ArrayList<>(),
                        API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<>());
        SampleUtils.publishAPI(apiIdThree);
        SampleUtils.deprecateAPI(apiIdThree);

        //Creating the new version of the Mobile stock API which is in PUBLISHED STATE
        String apiIdFour = SampleUtils.createNewAPIVersion("2.0.0", apiIdThree);
        SampleUtils.publishAPI(apiIdFour);

        //Creating a new API for sales_promotions which is in blocking state
        String apiIdFive = SampleUtils
                .createApi("Sales_promotions_API", "2.0.0", "/promo", new ArrayList<>(), new ArrayList<>(),
                        API.SubscriptionAvailabilityEnum.ALL_TENANTS, hostname, port, new ArrayList<>());
        SampleUtils.publishAPI(apiIdFive);
        SampleUtils.blockAPI(apiIdFive);

        //Creating ad API for  for sales_promotions which is in RETIRED state
        String apiIdSix = SampleUtils.createNewAPIVersion("1.0.0", apiIdFive);
        SampleUtils.publishAPI(apiIdSix);
        SampleUtils.rejectAPI(apiIdSix);
    }
}