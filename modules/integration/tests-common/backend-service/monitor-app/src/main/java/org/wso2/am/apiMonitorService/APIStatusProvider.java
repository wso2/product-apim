/*
*Copyright (c) 2005-2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.apiMonitorService;

import org.apache.axis2.AxisFault;
import org.wso2.am.apiMonitorService.beans.APIStatusData;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.rest.api.stub.RestApiAdminStub;
import org.wso2.carbon.rest.api.stub.types.carbon.APIData;
import org.wso2.carbon.utils.CarbonUtils;

import java.rmi.RemoteException;

public class APIStatusProvider {

    private static final String CARBON_XML_HOSTNAME = "HostName";

    public String[] getAllApisDeployed(String user, String password) {
        try {
            return this.getRestAPIAdmin(user, password).getApiNames();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getDeployedApiCount(String user,String password) {
        try {
            return this.getRestAPIAdmin(user, password).getAPICount();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public APIStatusData getApiDataOfApi(String user,String password, String apiName, String version){
        APIStatusData apistatusData = new APIStatusData();
        String[] apiNameList;
        boolean deployed = false;
        try {
            apiNameList = this.getRestAPIAdmin(user,password).getApiNames();
            for (String currentApi : apiNameList) {
                if (currentApi.contains("--")
                        && currentApi.split("--")[1].equals(apiName+":v"+version)) {
                    APIData apiData =  this.getRestAPIAdmin(user,password).getApiByName(currentApi);
                    apistatusData.setIsApiExists(true);
                    apistatusData.setApiName(apiName);
                    apistatusData.setProviderName(apiData.getFileName());
                    apistatusData.setVersion(version);
                    deployed = true;
                    break;
                } else {
                    continue;
                }
            }
            apistatusData.setIsApiExists(deployed);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return apistatusData;
    }

    private RestApiAdminStub getRestAPIAdmin(String username, String password) throws AxisFault {
        int port = 9443 + getPortOffset();
        RestApiAdminStub restApiAdminStub = new RestApiAdminStub(null, "https://" + getServiceHostname() + ':' + port +
                "/services/RestApiAdmin");
        CarbonUtils.setBasicAccessSecurityHeaders(username, password, true, restApiAdminStub._getServiceClient());
        return restApiAdminStub;
    }

    private static int getPortOffset() {
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = System.getProperty(APIConstants.PORT_OFFSET_SYSTEM_VAR,
                carbonConfig.getFirstProperty(APIConstants.PORT_OFFSET_CONFIG));
        try {
            if ((portOffset != null)) {
                return Integer.parseInt(portOffset.trim());
            } else {
                return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getServiceHostname() {
        String hostname = ServerConfiguration.getInstance().getFirstProperty(CARBON_XML_HOSTNAME);
        if (hostname != null) {
            return hostname;
        }
        return "localhost";
    }
}
