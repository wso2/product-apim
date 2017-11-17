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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.samples.utils.stubs.AuthenticateStub;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 */
public class TenantUtils {

    private static final Log log = LogFactory.getLog(TenantUtils.class);

    /**
     *
     * @param username
     * @param password
     * @param domainName
     * @param firstName
     * @param lastName
     * @param backendUrl
     * @return
     */
    public static boolean createTenant(String username, String password, String domainName,
            String firstName, String lastName, String backendUrl) {

        if (StringUtils.isEmpty(System.getProperty(Constants.JAVAX_NET_SSL_TRUST_STORE))) {
            System.setProperty(Constants.JAVAX_NET_SSL_TRUST_STORE,
                    TenantUtils.class.getClassLoader().getResource(Constants.CLIENT_TRUSTORE_JKS).getPath());
        }
        if (StringUtils.isEmpty(System.getProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_PASSWORD))) {
            System.setProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_PASSWORD, Constants.WSO2_CARBON);
        }
        if (StringUtils.isEmpty(System.getProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_TYPE))) {
            System.setProperty(Constants.JAVAX_NET_SSL_TRUST_STORE_TYPE, Constants.JKS);
        }

        boolean isSuccess = false;
        try {
            String endPoint = backendUrl + Constants.TENANT_MGT_ADMIN_SERVICE;
            TenantMgtAdminServiceStub tenantMgtAdminServiceStub = new TenantMgtAdminServiceStub(endPoint);
            AuthenticateStub
                    .authenticateStub(Constants.ADMIN_USERNAME, Constants.ADMIN_PASSWORD, tenantMgtAdminServiceStub);

            Date date = new Date();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);

            TenantInfoBean tenantInfoBean = new TenantInfoBean();
            tenantInfoBean.setActive(true);
            tenantInfoBean.setEmail(username + Constants.CHAR_AT + domainName);
            tenantInfoBean.setAdminPassword(password);
            tenantInfoBean.setAdmin(username);
            tenantInfoBean.setTenantDomain(domainName);
            tenantInfoBean.setCreatedDate(calendar);
            tenantInfoBean.setFirstname(firstName);
            tenantInfoBean.setLastname(lastName);
            tenantInfoBean.setSuccessKey("true");
            tenantInfoBean.setUsagePlan(Constants.USAGE_PLAN_DEMO);
            TenantInfoBean tenantInfoBeanGet;
            tenantInfoBeanGet = tenantMgtAdminServiceStub.getTenant(domainName);

            if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() != 0) {
                tenantMgtAdminServiceStub.activateTenant(domainName);
                log.info("Tenant domain " + domainName + " activated successfully");

            } else if (!tenantInfoBeanGet.getActive()) {
                tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
                tenantMgtAdminServiceStub.activateTenant(domainName);
                log.info("Tenant domain " + domainName + " created and activated successfully");
                isSuccess = true;
            } else {
                log.info("Tenant domain " + domainName + " already registered");
            }
        } catch (RemoteException e) {
            log.error("RemoteException thrown while adding user/tenants : ", e);

        } catch (TenantMgtAdminServiceExceptionException e) {
            log.error("Error connecting to the TenantMgtAdminService : ", e);
        }

        return isSuccess;
    }
}
