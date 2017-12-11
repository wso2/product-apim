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

package org.wso2.carbon.apimgt.samples.utils;

import org.wso2.carbon.apimgt.samples.utils.Clients.LifecycleManagementClient;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceExceptionException;

import java.io.IOException;

public class LifecycleUtils {

    public static void updateLifecycle(String serviceEndpoint, String username, String password, String lifecycle)
            throws IOException, LifeCycleManagementServiceExceptionException {

        LifecycleManagementClient lifecycleManagementClient = new LifecycleManagementClient(serviceEndpoint, username,
                password);
        lifecycleManagementClient.updateLifecycleConfiguration(lifecycle);
    }

}
