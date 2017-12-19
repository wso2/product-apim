/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.samples.utils.Clients;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.apimgt.samples.utils.Constants;
import org.wso2.carbon.apimgt.samples.utils.SampleUtils;
import org.wso2.carbon.apimgt.samples.utils.stubs.AuthenticateStub;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceExceptionException;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceStub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LifecycleManagementClient {

    private final String serviceName = "LifeCycleManagementService";
    private LifeCycleManagementServiceStub lifeCycleManagementServiceStub;

    public LifecycleManagementClient(String backEndUrl, String username, String password) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        lifeCycleManagementServiceStub = new LifeCycleManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(username, password, lifeCycleManagementServiceStub);

    }

    public void updateLifecycleConfiguration(String lifecycle)
            throws IOException, LifeCycleManagementServiceExceptionException {
        String configuration = getCustomLifecycleConfiguration(lifecycle);
        lifeCycleManagementServiceStub.updateLifecycle(lifecycle, configuration);

    }

    private String getCustomLifecycleConfiguration(String lifecycle) throws IOException {
        if (StringUtils.isNotEmpty(lifecycle + Constants.XML)) {
            return IOUtils.toString(SampleUtils.class.getClassLoader().getResourceAsStream(lifecycle + Constants.XML),
                    StandardCharsets.UTF_8.name());
        }
        return null;
    }
}
