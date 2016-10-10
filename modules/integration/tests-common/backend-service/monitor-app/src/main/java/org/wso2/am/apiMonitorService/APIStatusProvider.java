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

import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.rest.API;
import org.wso2.am.apiMonitorService.beans.APIStatusData;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.rest.api.ConfigHolder;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class APIStatusProvider {

    public String[] getAllApisDeployed(String tenantDomain, int tenantId) {
        List<String> apiNameList = new LinkedList<>();
        Collection<API> apiList = this.getSynapseConfiguration(tenantDomain, tenantId).getAPIs();
        for (API currentApi : apiList) {
            apiNameList.add(currentApi.getAPIName());
        }
        String[] apiArray = new String[apiNameList.size()];
        return apiNameList.toArray(apiArray);
    }

    public int getDeployedApiCount(String tenantDomain, int tenantId) {
        return this.getSynapseConfiguration(tenantDomain, tenantId).getAPIs().size();
    }

    public APIStatusData getApiDataOfApi(String tenantDomain, int tenantId, String apiName, String version) {
        APIStatusData apistatusData = new APIStatusData();
        Collection<API> apiList = this.getSynapseConfiguration(tenantDomain, tenantId).getAPIs();
        boolean deployed = false;
        for (API currentApi : apiList) {
            if (currentApi.getName().contains("--")
                && currentApi.getName().split("--")[1].equals(apiName+":v"+version)) {
                apistatusData.setIsApiExists(true);
                apistatusData.setApiName(currentApi.getAPIName());
                apistatusData.setProviderName(currentApi.getFileName());
                apistatusData.setVersion(currentApi.getVersion());
                deployed = true;
                break;
            } else {
                continue;
            }
        }
        apistatusData.setIsApiExists(deployed);
        return apistatusData;
    }

    private SynapseConfiguration getSynapseConfiguration(String tenantDomain, int tenantId) {
        SynapseEnvironmentService synEnvService = null;
        if (tenantDomain == null) {
            synEnvService =
                    ConfigHolder.getInstance()
                            .getSynapseEnvironmentService(MultitenantConstants.SUPER_TENANT_ID);
        } else {
            try {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().startTenantFlow();
                PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                privilegedCarbonContext.setTenantId(tenantId);
                privilegedCarbonContext.setTenantDomain(tenantDomain);
                synEnvService = ConfigHolder.getInstance().getSynapseEnvironmentService(tenantId);
            } catch (Exception e) {
                String msg = "Error while Eager loading tenant : " + tenantDomain;
                throw new RuntimeException(msg, e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        if(synEnvService == null){
            throw new RuntimeException("Synapse Environment Service is null.");
        }

        SynapseEnvironment synapseEnv = synEnvService.getSynapseEnvironment();

        if(synapseEnv == null){
            throw new RuntimeException("Synapse Environment is null.");
        }

        return synapseEnv.getSynapseConfiguration();
    }

}
