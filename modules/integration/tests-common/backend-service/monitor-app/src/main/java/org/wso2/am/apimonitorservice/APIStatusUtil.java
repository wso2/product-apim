/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.apimonitorservice;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.apimonitorservice.beans.TenantStatus;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.rest.api.APIData;
import org.wso2.carbon.rest.api.APIException;
import org.wso2.carbon.rest.api.service.RestApiAdmin;
import org.wso2.carbon.apimgt.gateway.utils.RESTAPIAdminClient;
import org.wso2.carbon.apimgt.gateway.service.APIGatewayAdmin;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.rmi.RemoteException;
import java.util.Map;

public class APIStatusUtil {
    private static final Log log = LogFactory.getLog(APIStatusUtil.class);
    private RestApiAdmin restApiAdmin;
    private RESTAPIAdminClient restapiAdminClient;
    private APIGatewayAdmin apiGatewayAdmin;

    public APIStatusUtil() throws AxisFault {
        restApiAdmin = new RestApiAdmin();
        apiGatewayAdmin = new APIGatewayAdmin();
    }

    protected void setAdminClient(String apiProviderName, String apiName, String apiVersion)
            throws AxisFault {
        restapiAdminClient = new RESTAPIAdminClient(apiProviderName,apiName,apiVersion);
    }

    public int getDeployedApiStats() throws RemoteException {
        return restApiAdmin.getAPICount();
    }

    public String[] getDeployedApiNames() throws RemoteException {
        return restApiAdmin.getApiNames();
    }

    public boolean isApiExists(String apiName) {
        boolean isApiExists = false;
        try {
            APIData apiData = restApiAdmin.getApiByName(apiName);
            if (apiData.getName() != null) {
                isApiExists = true;
            }
        } catch (NullPointerException e) {
            log.info("Api " + apiName + " not found ");
            isApiExists = false;
        }
        return isApiExists;
    }

    public APIData getAPIDataByName(String apiName) {
        APIData apiData;
        try {
            apiData = restApiAdmin.getApiByName(apiName);
        } catch (NullPointerException e) {
            log.info("Api " + apiName + " not found ");
            apiData = null;
        }

        return apiData;
    }


    public boolean isApiExistsByTenant(String tenantDomain, String apiName)
            throws APIException, AxisFault {
        boolean isApiExists = false;
        try {
            org.wso2.carbon.rest.api.stub.types.carbon.APIData apiData = restapiAdminClient.getApi(tenantDomain);
            if (apiData.getName() != null) {
                isApiExists = true;
            }
        } catch (NullPointerException e) {
            log.info("Api " + apiName + " not found ");
            isApiExists = false;
        }
        return isApiExists;
    }




    public org.wso2.carbon.rest.api.stub.types.carbon.APIData getAPIDataByTenantByName(
            String tenantDomain)
            throws APIException, AxisFault {
        org.wso2.carbon.rest.api.stub.types.carbon.APIData apiData;
        try {
            apiData = restapiAdminClient.getApi(tenantDomain);
        } catch (NullPointerException e) {
            log.info("Api " + " not found ");
            apiData = null;
        }

        return apiData;
    }

    /**
     * Check  the given tenant is loaded.
     *
     * @param tenantDomain Domain name of the tenant
     * @return TenantStatus with current status  information about the tenant.
     */
    protected static TenantStatus getTenantStatus(String tenantDomain) {
        boolean isTenantContextLoaded = false;
        Map<String, ConfigurationContext> tenantConfigServerContexts = getTenantConfigServerContexts();
        if (tenantConfigServerContexts != null) {
            isTenantContextLoaded = tenantConfigServerContexts.containsKey(tenantDomain);
            log.info("Tenant " + tenantDomain + " loaded :" + isTenantContextLoaded);
        }
        return new TenantStatus(isTenantContextLoaded);
    }

    /**
     * Get the configuration contexts of all loaded tenants
     *
     * @return Map that contains the  configuration contexts
     */
    private static Map<String, ConfigurationContext> getTenantConfigServerContexts() {
        return TenantAxisUtils.getTenantConfigurationContexts(getServerConfigurationContext());
    }

    /**
     * Get the server configuration context.
     *
     * @return configuration context of the server.
     */
    private static ConfigurationContext getServerConfigurationContext() {
        ConfigurationContextService configurationContext =
                (ConfigurationContextService) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                        getOSGiService(ConfigurationContextService.class);
        return configurationContext.getServerConfigContext();
    }

}
