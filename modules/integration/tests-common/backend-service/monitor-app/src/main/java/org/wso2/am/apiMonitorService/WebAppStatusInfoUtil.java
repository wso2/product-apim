/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.apimonitorservice;


import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.apimonitorservice.beans.WebAppData;
import org.wso2.am.apimonitorservice.beans.WebAppDeployStatus;
import org.wso2.am.apimonitorservice.beans.WebAppStatus;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.webapp.mgt.WebApplication;
import org.wso2.carbon.webapp.mgt.WebApplicationsHolder;

import java.util.HashMap;
import java.util.Map;


/**
 * Util class that contains the service implementation of LazyLoadingInfoService and supportive methods.
 */
public class WebAppStatusInfoUtil {
    private final Log log = LogFactory.getLog(WebAppStatusInfoUtil.class);
    private final String CARBON_WEBAPPS_HOLDER_LIST = "carbon.webapps.holderlist";
    private final String WEBAPPS = "webapps";

    /**
     * Get the server configuration context.
     *
     * @return configuration context of the server.
     */
    private ConfigurationContext getServerConfigurationContext() {
        ConfigurationContextService configurationContext =
                (ConfigurationContextService) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                        getOSGiService(ConfigurationContextService.class, null);
        return configurationContext.getServerConfigContext();
    }


    /**
     * Get the configuration contexts of all loaded tenants
     *
     * @return Map that contains the  configuration contexts
     */
    private Map<String, ConfigurationContext> getTenantConfigServerContexts() {
        return TenantAxisUtils.getTenantConfigurationContexts(getServerConfigurationContext());
    }


    /**
     * Get the configuration context of given tenant.
     *
     * @param tenantDomain tenant domain name.
     * @return ConfigurationContext of given tenant
     */
    private ConfigurationContext getTenantConfigurationServerContext(String tenantDomain) {
        return getTenantConfigServerContexts().get(tenantDomain);
    }

    protected WebAppDeployStatus getWebAppDeploymentStatus(String webAppName) {
        Map<String, WebApplication> allWebAppMap = new HashMap<>();
        WebApplication selectedWebApp;
        WebAppDeployStatus webAppDeployStatus = new WebAppDeployStatus();
        WebAppData webAppData = new WebAppData();

        WebApplicationsHolder webApplicationsHolder =
                (WebApplicationsHolder) ((HashMap) getServerConfigurationContext().getProperty(
                        CARBON_WEBAPPS_HOLDER_LIST)).get(WEBAPPS);

        Map<String, WebApplication> startedWebAppMap = webApplicationsHolder.getStartedWebapps();
        Map<String, WebApplication> faultyWebAppMap = webApplicationsHolder.getFaultyWebapps();
        Map<String, WebApplication> stoppedWebAppMap = webApplicationsHolder.getStoppedWebapps();

        if (!(startedWebAppMap.size() <= 0)) {
            allWebAppMap.putAll(startedWebAppMap);
        }
        if (stoppedWebAppMap.size() <= 0) {
            allWebAppMap.putAll(startedWebAppMap);
        }
        if (!(faultyWebAppMap.size() <= 0)) {
            allWebAppMap.putAll(faultyWebAppMap);
        }
        selectedWebApp = allWebAppMap.get(webAppName);
        webAppDeployStatus.setIsWebAppExists(allWebAppMap.containsKey(webAppName));
        webAppData.setContextPath(selectedWebApp.getContext().getPath());
        webAppData.setWebAppState(selectedWebApp.getState());
        webAppData.setWebAppName(selectedWebApp.getDisplayName());
        webAppData.setWebAppFile(selectedWebApp.getWebappFile().getAbsolutePath());
        webAppDeployStatus.setWebAppData(webAppData);
        return webAppDeployStatus;
    }


    protected WebAppStatus getWebAppStatus() {
        WebAppStatus webAppStatus = new WebAppStatus();

        WebApplicationsHolder webApplicationsHolder =
                (WebApplicationsHolder) ((HashMap) getServerConfigurationContext().getProperty(
                        CARBON_WEBAPPS_HOLDER_LIST)).get(WEBAPPS);

        Map<String, WebApplication> startedWebAppMap = webApplicationsHolder.getStartedWebapps();
        Map<String, WebApplication> faultyWebAppMap = webApplicationsHolder.getFaultyWebapps();
        Map<String, WebApplication> stoppedWebAppMap = webApplicationsHolder.getStoppedWebapps();

        if (!(startedWebAppMap.size() <= 0)) {
            webAppStatus.setStartedWebApps((startedWebAppMap.keySet()).toArray(new String[startedWebAppMap.keySet().size()]));
        }
        if (stoppedWebAppMap.size() <= 0) {
            webAppStatus.setStoppedWebApps((stoppedWebAppMap.keySet()).toArray(new String[startedWebAppMap.keySet().size()]));
        }
        if (!(faultyWebAppMap.size() <= 0)) {
            webAppStatus.setFaultyWebApps((faultyWebAppMap.keySet()).toArray(new String[startedWebAppMap.keySet().size()]));
        }
        return webAppStatus;
    }
}
