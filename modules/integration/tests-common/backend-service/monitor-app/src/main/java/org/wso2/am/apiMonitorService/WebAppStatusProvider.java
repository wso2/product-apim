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

package org.wso2.am.apiMonitorService;


import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.apiMonitorService.beans.WebAppData;
import org.wso2.am.apiMonitorService.beans.WebAppDeployStatus;
import org.wso2.am.apiMonitorService.beans.WebAppStatus;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.eager.TenantEagerLoader;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.webapp.mgt.WebApplication;
import org.wso2.carbon.webapp.mgt.WebApplicationsHolder;
import org.wso2.carbon.webapp.mgt.utils.WebAppUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Util class that contains the service implementation of LazyLoadingInfoService and supportive methods.
 */
public class WebAppStatusProvider {
    private final Log log = LogFactory.getLog(WebAppStatusProvider.class);
    private final String CARBON_WEBAPPS_HOLDER_LIST = "carbon.webapps.holder";
    private final String WEBAPPS = "webapps";
    private final String GHOST_WEB_APP = "GhostWebApp";


    /**
     * Get the server configuration context.
     *
     * @return configuration context of the server.
     */
    private ConfigurationContext getServerConfigurationContext() {
        ConfigurationContextService configurationContext =
                (ConfigurationContextService) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                        .getOSGiService(ConfigurationContextService.class);
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


    protected WebAppDeployStatus getWebAppDeploymentStatus(String webAppName, String tenantDomain, int tenantId) {
        Map<String, WebApplication> startedWebAppMap = new HashMap<>();
        Map<String, WebApplication> faultyWebAppMap = new HashMap<>();
        Map<String, WebApplication> stoppedWebAppMap = new HashMap<>();
        Map<String, WebApplication> allWebAppMap = new HashMap<>();
        WebAppUtils.getAllWebappHolders(getServerConfigurationContext());
        WebApplication selectedWebApp;
        WebAppDeployStatus webAppDeployStatus = new WebAppDeployStatus();

        WebAppData webAppData = new WebAppData();
        WebApplicationsHolder webApplicationsHolder = this.getWebAppHolder(tenantDomain, tenantId);
        startedWebAppMap = webApplicationsHolder.getStartedWebapps();
        faultyWebAppMap = webApplicationsHolder.getFaultyWebapps();
        stoppedWebAppMap = webApplicationsHolder.getStoppedWebapps();
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


    private WebApplicationsHolder getWebAppHolder(String tenantDomain, int tenantId) {
        WebApplicationsHolder webApplicationsHolder = null;
        if (tenantDomain==null) {
            ConfigurationContextService configurationContext =
                    (ConfigurationContextService) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                            getOSGiService(ConfigurationContextService.class, null);

            ConfigurationContext serverConfigurationContext = configurationContext.getServerConfigContext();
            webApplicationsHolder = (WebApplicationsHolder) ((HashMap)
                                                                     serverConfigurationContext.getLocalProperty("carbon.webapps.holderlist")).get("webapps");
        } else {
            try {
                TenantEagerLoader tenantEagerLoader = new TenantEagerLoader();
                tenantEagerLoader.initializeEagerLoadingTenants();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().startTenantFlow();
                PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                privilegedCarbonContext.setTenantId(tenantId);
                privilegedCarbonContext.setTenantDomain(tenantDomain);
                ConfigurationContext tenantConfigurationServerContext = getTenantConfigurationServerContext(tenantDomain);
                if(tenantConfigurationServerContext!= null) {
                    webApplicationsHolder =
                            (WebApplicationsHolder) ((HashMap) tenantConfigurationServerContext.getProperty("carbon.webapps" +
                                                                                                            ".holderlist")).get("webapps");
                }
                else{
                    throw new RuntimeException("Tenant"+ tenantDomain +"Not loded please login with tenat first");
                }
            } catch (Exception e) {
                String msg = "Error while Eager loading tenant : " + tenantDomain;
                throw new RuntimeException(msg, e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return webApplicationsHolder;
    }

    protected WebAppStatus getWebAppStatus(String tenantDomain, int tenantId) {
        Map<String, WebApplication> startedWebAppMap = new HashMap<>();
        Map<String, WebApplication> faultyWebAppMap = new HashMap<>();
        Map<String, WebApplication> stoppedWebAppMap = new HashMap<>();
        WebAppStatus webAppStatus = new WebAppStatus();
        WebApplicationsHolder webApplicationsHolder = this.getWebAppHolder(tenantDomain, tenantId);
        startedWebAppMap = webApplicationsHolder.getStartedWebapps();
        faultyWebAppMap = webApplicationsHolder.getFaultyWebapps();
        stoppedWebAppMap = webApplicationsHolder.getStoppedWebapps();
        if (!(startedWebAppMap.size() <= 0)) {
            webAppStatus.setStartedWebApps((startedWebAppMap.keySet())
                                                   .toArray(new String[startedWebAppMap.keySet().size()]));
        }
        if (stoppedWebAppMap.size() <= 0) {
            webAppStatus.setStoppedWebApps((stoppedWebAppMap.keySet())
                                                   .toArray(new String[startedWebAppMap.keySet().size()]));
        }
        if (!(faultyWebAppMap.size() <= 0)) {
            webAppStatus.setFaultyWebApps((faultyWebAppMap.keySet())
                                                  .toArray(new String[startedWebAppMap.keySet().size()]));
        }
        return webAppStatus;
    }

    protected WebAppStatus getSuperTenantWebAppStatus(String webAppName) {
        WebAppStatus webAppStatus = new WebAppStatus();
        WebApplicationsHolder webApplicationsHolder = (WebApplicationsHolder) ((HashMap)
                                                                                       getServerConfigurationContext().getLocalProperty(CARBON_WEBAPPS_HOLDER_LIST)).get(WEBAPPS);
        Map<String, WebApplication> startedWebAppMap = webApplicationsHolder.getStartedWebapps();
        if (startedWebAppMap != null) {
            WebApplication webApplication = startedWebAppMap.get(webAppName);
            if (webApplication != null) {
                //  webAppStatus.setWebAppStarted(true);
                log.info("Super Tenant  Web-app: " + webAppName + " is available in configuration context.");
                boolean isWebAppGhost = Boolean.parseBoolean((String) webApplication.getProperty(GHOST_WEB_APP));
                log.info("Super Tenant Web-app: " + webAppName + " is in Ghost deployment status :" + isWebAppGhost);
                //webAppStatus.setWebAppGhost(isWebAppGhost);
            } else {
                log.info("Given web-app:" + webAppName + " for super tenant  not found in started state");
            }
        } else {
            log.info("Super Tenant has no started web-apps.");
        }
        return webAppStatus;
    }
}
