/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.test.utils.clients;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTierRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AdminDashboardRestClient {
    private static final Log log = LogFactory.getLog(AdminDashboardRestClient.class);
    private String backendURL;
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    public AdminDashboardRestClient(String backendURL) {
        this.backendURL = backendURL;
        if (requestHeaders.get("Content-Type") == null) {
            this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        }
    }

    /**
     * Login to API Admin Portal
     *
     * @param userName - username to login
     * @param password - password to login
     * @return - http response
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if login to 
     * Admin Portal fails
     */
    public HttpResponse login(String userName, String password)
            throws APIManagerIntegrationTestException {
        HttpResponse response;
        log.info("Login to Admin Portal " + backendURL + " as the user " + userName );
        try {
            response = HTTPSClientUtils.doPost(
                    new URL(backendURL + "admin-old/site/blocks/user/login/ajax/login.jag"),
                    "action=login&username=" + userName + "&password=" + password + "",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to login to the store app ", e);
        }

        String session = getSession(response.getHeaders());

        if (session == null) {
            throw new APIManagerIntegrationTestException("No session cookie found with response");
        }

        setSession(session);
        return response;
    }
    
    /**
     * Add/Edit Tier
     * @param throttlingTierRequest
     * @return http response
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse addTier(APIThrottlingTierRequest throttlingTierRequest) 
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "admin-old/site/blocks/tier/edit/ajax/tier-edit.jag"),
                    throttlingTierRequest.generateRequestParameters(), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Add new tier failed", e);
        }
    }

    public HttpResponse addThrottlingPolicy(String throttlingPolicyJSON)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            requestHeaders.put("Accept", "application/json, text/javascript");
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "admin-old/site/blocks/policy/resource/policy-add/ajax/policy-operations.jag"),
                    "action=addApiPolicy&apiPolicy=" + URLEncoder.encode(throttlingPolicyJSON, "UTF-8")
                    , requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Add new policy failed", e);
        }
    }
    
    public HttpResponse addApplicationPolicyWithBandwidthType(String name, int bandwidth, String bandwidthUnit,
            int defaultUnitTime, String defaultTimeUnit) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            requestHeaders.put("Accept", "application/json, text/javascript");
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "admin-old/site/blocks/policy/app/edit/ajax/app-policy-edit.jag"),
                    "action=add&policyName=" + name
                            + "&description=&defaultQuotaPolicy=bandwidthVolume&defaultRequestCount=&defaultBandwidth="
                            + bandwidth + "&defaultBandwidthUnit=" + bandwidthUnit + "&defaultUnitTime="
                            + defaultUnitTime + "&defaultTimeUnit=" + defaultTimeUnit
                            + "&stopOnQuotaReach=false&attributes=%5B%5D",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Add new policy failed", e);
        }
    }
    
    public HttpResponse addSubscriptionPolicyWithBandwidthType(String name, int bandwidth, String bandwidthUnit,
            int defaultUnitTime, String defaultTimeUnit, boolean stopOnQuotaReach, int rateLimit,
            String rateLimitTimeUnit) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            requestHeaders.put("Accept", "application/json, text/javascript");
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            return HTTPSClientUtils.doPost(
                    new URL(backendURL
                            + "admin-old/site/blocks/policy/subscription/edit/ajax/subscription-policy-edit.jag"),
                    "action=add&policyName=" + name
                            + "&description=&defaultQuotaPolicy=bandwidthVolume&defaultRequestCount=&defaultBandwidth="
                            + bandwidth + "&defaultBandwidthUnit=" + bandwidthUnit + "&defaultUnitTime="
                            + defaultUnitTime + "&defaultTimeUnit=" + defaultTimeUnit + "&rateLimitCount=" + rateLimit
                            + "&rateLimitTimeUnit=" + rateLimitTimeUnit + "&stopOnQuotaReach=" + stopOnQuotaReach
                            + "&tierPlan=FREE&monetizationPlan=FixedRate&fixedRate=&billingCycle=week&pricePerRequest=&currencyType=&permissionType=allow&roles=Internal%2Feveryone&attributes=%5B%5D",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Add new policy failed", e);
        }
    }

    public HttpResponse deleteSubscriptionPolicy(String name) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            requestHeaders.put("Accept", "application/json, text/javascript");
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            return HTTPSClientUtils.doPost(
                    new URL(backendURL
                            + "admin-old/site/blocks/policy/subscription/manage/ajax/subscription-policy-manage.jag"),
                    "action=deleteSubscriptionPolicy&policy=" + name, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Delete subscription policy failed", e);
        }
    }
    
    public HttpResponse deleteAPIPolicy(String name) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            requestHeaders.put("Accept", "application/json, text/javascript");
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            return HTTPSClientUtils.doPost(
                    new URL(backendURL
                            + "admin-old/site/blocks/policy/resource/policy-list/ajax/api-policy-manage.jag"),
                    "action=deleteAPIPolicy&policy=" + name, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Delete subscription policy failed", e);
        }
    }
    
    public HttpResponse deleteApplicationPolicy(String name) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            requestHeaders.put("Accept", "application/json, text/javascript");
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            return HTTPSClientUtils.doPost(
                    new URL(backendURL
                            + "admin-old/site/blocks/policy/app/manage/ajax/app-policy-manage.jag"),
                    "action=deleteAppPolicy&policy=" + name, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Delete subscription policy failed", e);
        }
    }
    /**
     * Delete tier
     * @param tierName
     * @return http response
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse deleteTier(String tierName) 
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "admin-old/site/blocks/tier/manage/ajax/tier-manage.jag"),
                    "action=deleteTier&tier=" + tierName, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Delete tier failed", e);
        }        
    }
    
    /**
     * Get all tiers
     * @return http response
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse getAllTiers() 
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "admin-old/site/blocks/tier/manage/ajax/tier-manage.jag"),
                    "action=getAllTiers", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Get all tiers failed", e);
        }        
    }

    /**
     * Get all applications within a specific tenant domain
     * @return http response
     * @throws APIManagerIntegrationTestException
     * @param search
     * @param start
     * @param draw
     * @param offset
     * @param columnId
     * @param sortOrder
     */
    public HttpResponse getapplicationsByTenantId(String search, String start, String draw, String offset,
                                                  String columnId, String sortOrder)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL
                            + "admin-old/site/blocks/application-owner/get-applications/ajax/get-applications.jag"),
                            "action=getApplicationsByTenantIdWithPagination&start=" + start + "&length=" + offset
                            + "&search[value]=" + search + "&draw=" + draw + "&order[0][column]=" + columnId
                            + "&order[0][dir]=" + sortOrder, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Get all applications with pagination failed", e);
        }
    }

    /**
     * Update application owner
     * @return http response
     * @throws APIManagerIntegrationTestException
     * @param newOwner
     * @param oldOwner
     * @param uuid
     * @param appName
     */
    public HttpResponse updateApplicationOwner(String newOwner, String oldOwner, String uuid, String appName)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL
                            + "admin-old/site/blocks/application-owner/change-owner/ajax/change-owner.jag"),
                            "action=changeOwner&newOwner=" + newOwner + "&oldOwner=" + oldOwner
                            + "&applicationUuid=" + uuid + "&applicationName=" + appName, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Updating application owner failed", e);
        }
    }
    
    private String getSession(Map<String, String> responseHeaders) {
        return responseHeaders.get("Set-Cookie");
    }

    private String setSession(String session) {
        return requestHeaders.put("Cookie", session);
    }
    
    /**
     * Check whether the user is logged in
     *
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - If session cookie not found in the request header
     */
    private void checkAuthentication() throws APIManagerIntegrationTestException {
        if (requestHeaders.get("Cookie") == null) {
            throw new APIManagerIntegrationTestException("No Session Cookie found. Please login first");
        }
    }


}
