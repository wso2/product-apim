/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.test.utils.generic;

import com.google.gson.Gson;
import org.wso2.am.integration.test.utils.bean.EndpointBean;
import org.wso2.am.integration.test.utils.bean.ProductionEndpointBean;

/**
 * This util class used to provide utility functions for Integration test cases
 */
public class Utils {
    private static final Gson gson = new Gson();

    /**
     * Generate JSON string for production endpoint
     * @param url production endpoint url
     * @param config configurations for production endpoint
     * @param protocol protocol type of the endpoint
     * @return return production endpoint configurations as JSON
     */
    public static String generateProductionEndpoints(String url, String config, String protocol) {

        ProductionEndpointBean productionEndpointBean = new ProductionEndpointBean();
        productionEndpointBean.setUrl(url);
        productionEndpointBean.setConfig(config);
        productionEndpointBean.setEndpoint_type(protocol);

        EndpointBean endpointBean = new EndpointBean();
        endpointBean.setProduction_endpoints(productionEndpointBean);
        String endpointJson = gson.toJson(endpointBean);
        return endpointJson;
    }
}
