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
import org.wso2.am.apiMonitorService.beans.APIStats;
import org.wso2.am.apiMonitorService.beans.APIStatusData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/apiInformation/")
public class ApiInformationService {
    APIStats stats;
    APIStatusData apiStatus;
    APIStatusProvider apiStatusProvider;

    public ApiInformationService() throws AxisFault {
        stats = new APIStats();
        apiStatus = new APIStatusData();
        apiStatusProvider = new APIStatusProvider();
    }


    @Path("api/getApiList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStats getAllDeployedApis() {
        stats.setDeployedApiCount(apiStatusProvider.getDeployedApiCount(null, 0));
        stats.setListOfApiNames(apiStatusProvider.getAllApisDeployed(null, 0));
        return stats;
    }

    @Path("api/{tenatDomain}/{tenantId}/getApiList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStats getAllDeployedApisForTenant(@PathParam("tenatDomain") String tenantDomain, @PathParam("tenantId")
    int tenantId) {
        stats.setDeployedApiCount(apiStatusProvider.getDeployedApiCount(tenantDomain, tenantId));
        stats.setListOfApiNames(apiStatusProvider.getAllApisDeployed(tenantDomain, tenantId));
        return stats;
    }


    @Path("api/getStatus/{apiName}/{version}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStatusData getApiStatus(@PathParam("apiName") String apiName, @PathParam("version") String version) {

        return apiStatusProvider.getApiDataOfApi(null, 0, apiName, version);
    }


    @Path("api/{tenatDomain}/{tenantId}/{apiName}/{version}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStatusData getApiStatusForTenant(@PathParam("tenatDomain") String tenantDomain, @PathParam("tenantId")
    int tenantId, @PathParam("apiName") String apiName, @PathParam("version") String version) {
        return apiStatusProvider.getApiDataOfApi(tenantDomain, tenantId, apiName, version);
    }

}
