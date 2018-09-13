/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests.util;

import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIEndpoint;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndPoint;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndPointEndpointConfig;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndPointEndpointSecurity;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.EndpointConfig;

import java.util.Collections;
import java.util.List;

/**
 * Utility for Sample Test Object creation
 */
public class SampleTestObjectCreator {

    public static API ApiToCreate(String name, String version, String context) {
        API api = new API();
        api.setName(name);
        api.setContext(context);
        api.setVersion(version);
        api.addSecuritySchemeItem("Oauth");
        api.addTransportItem("http");

        EndpointConfig petStoreEndpoint = new EndpointConfig()
                .url("https://localhost:9443/publisher/public/app/petstore/pet/1.json").timeout("1000");
        List<EndpointConfig> endpointConfigList = Collections.singletonList(petStoreEndpoint);
        EndPointEndpointConfig endpointConfig = new EndPointEndpointConfig().endpointType(EndPointEndpointConfig
                .EndpointTypeEnum.SINGLE).list(endpointConfigList);
        EndPoint endPoint = new EndPoint().name(name + "-" + version).endpointSecurity(new EndPointEndpointSecurity()
                .enabled(false)).endpointConfig(endpointConfig);
        APIEndpoint apiEndpoint = new APIEndpoint().type("production").inline(endPoint);
        api.addEndpointItem(apiEndpoint);

        return api;
    }
}