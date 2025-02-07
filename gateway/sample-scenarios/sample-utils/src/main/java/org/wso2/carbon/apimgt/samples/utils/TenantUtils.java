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

    /**
     * This method is used to create tenants.
     *
     * @param username  username of the tenant.
     * @param password  password of the tenant.
     * @param domainName    tenant domain
     * @param firstName tenant admins first name
     * @param lastName  tenant admins last name
     * @param backendUrl    tenant creation server url.
     * @return whether the tenant creation is successful or not.
     */
    public static boolean createTenant(String username, String password, String domainName, String firstName,
            String lastName, String backendUrl) {


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
                System.out.println("Tenant domain " + domainName + " activated successfully");

            } else if (!tenantInfoBeanGet.getActive()) {
                tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
                tenantMgtAdminServiceStub.activateTenant(domainName);
                System.out.println("Tenant domain " + domainName + " created and activated successfully");
                isSuccess = true;
            } else {
                System.out.println("Tenant domain " + domainName + " already registered");
            }
        } catch (RemoteException e) {
            System.out.println("RemoteException thrown while adding user/tenants");

        } catch (TenantMgtAdminServiceExceptionException e) {
            System.out.println("Error connecting to the TenantMgtAdminService");
        }

        return isSuccess;
    }
}
