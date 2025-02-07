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

import org.apache.commons.codec.binary.Base64;
import org.wso2.am.apiMonitorService.beans.APIStats;
import org.wso2.am.apiMonitorService.beans.APIStatusData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

@Path("/apiInformation/")
public class ApiInformationService {
    APIStats stats;
    APIStatusData apiStatus;
    APIStatusProvider apiStatusProvider;

    public ApiInformationService()  {
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
    public APIStats getAllDeployedApis(@Context HttpHeaders httpHeaders) {
        String authorization =getCredentials(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
        StringTokenizer stringTokenizer = new StringTokenizer(authorization,":");
        String username = stringTokenizer.nextToken();
        String password = stringTokenizer.nextToken();
        stats.setDeployedApiCount(apiStatusProvider.getDeployedApiCount(username,password));
        stats.setListOfApiNames(apiStatusProvider.getAllApisDeployed(username,password));
        return stats;
    }

    @Path("api/{tenatDomain}/{tenantId}/getApiList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStats getAllDeployedApisForTenant(@PathParam("tenatDomain") String tenantDomain, @PathParam("tenantId")
    int tenantId,@Context HttpHeaders httpHeaders) {
        String authorization =getCredentials(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
        StringTokenizer stringTokenizer = new StringTokenizer(authorization,":");
        String username = stringTokenizer.nextToken();
        String password = stringTokenizer.nextToken();
        stats.setDeployedApiCount(apiStatusProvider.getDeployedApiCount(username,password));
        stats.setListOfApiNames(apiStatusProvider.getAllApisDeployed(username,password));
        return stats;
    }


    @Path("api/getStatus/{apiName}/{version}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStatusData getApiStatus(@PathParam("apiName") String apiName, @PathParam("version") String version,
                                      @Context HttpHeaders httpHeaders) {
        String authorization =getCredentials(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
        StringTokenizer stringTokenizer = new StringTokenizer(authorization,":");
        String username = stringTokenizer.nextToken();
        String password = stringTokenizer.nextToken();
        return apiStatusProvider.getApiDataOfApi(username, password, apiName, version);
    }


    @Path("api/{tenatDomain}/{tenantId}/{apiName}/{version}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIStatusData getApiStatusForTenant(@PathParam("tenatDomain") String tenantDomain, @PathParam("tenantId")
            int tenantId, @PathParam("apiName") String apiName, @PathParam("version") String version, @Context
                                                       HttpHeaders httpHeaders) {

            String authorization =getCredentials(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
            StringTokenizer stringTokenizer = new StringTokenizer(authorization,":");
            String username = stringTokenizer.nextToken();
            String password = stringTokenizer.nextToken();
            return apiStatusProvider.getApiDataOfApi(username, password, apiName, version);

    }
    private String getCredentials(String authCredentials)  {

        final String encodedUserPassword = authCredentials.replaceFirst("Basic"
                + " ", "");
        String usernameAndPassword = null;
            byte[] decodedBytes = Base64.decodeBase64(encodedUserPassword.getBytes());
        try {
            usernameAndPassword = new String(decodedBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return usernameAndPassword;
    }

}
