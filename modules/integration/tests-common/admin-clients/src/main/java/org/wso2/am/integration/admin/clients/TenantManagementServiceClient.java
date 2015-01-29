/*
*Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.admin.clients;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStubUtil;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TenantManagementServiceClient {
    private TenantMgtAdminServiceStub tenantMgtAdminServiceStub;
    private static final Log log = LogFactory.getLog(TenantManagementServiceClient.class);

    public TenantManagementServiceClient(String backEndURL, String sessionCookie) throws AxisFault {
        String serviceName = "TenantMgtAdminService";
        String endPoint = backEndURL + serviceName;
        tenantMgtAdminServiceStub = new TenantMgtAdminServiceStub(endPoint);
        AuthenticateStubUtil.authenticateStub(sessionCookie, tenantMgtAdminServiceStub);
    }

    /**
     * @param domainName domain of the tenant
     * @param password   password of the tenant admin user
     * @param firstName  first name of the tenant admin user
     * @param usagePlan  Usage plan of the tenant
     * @throws RemoteException
     * @throws TenantMgtAdminServiceExceptionException
     *
     */
    public void addTenant(String domainName, String password, String firstName, String usagePlan)
            throws RemoteException, TenantMgtAdminServiceExceptionException {
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        tenantInfoBean.setActive(true);
        tenantInfoBean.setEmail(firstName + "@" + domainName);
        tenantInfoBean.setAdmin(firstName);
        tenantInfoBean.setAdminPassword(password);
        tenantInfoBean.setUsagePlan(usagePlan);
        tenantInfoBean.setLastname(firstName + "wso2automation");
        tenantInfoBean.setSuccessKey("true");
        tenantInfoBean.setCreatedDate(calendar);
        tenantInfoBean.setTenantDomain(domainName);
        tenantInfoBean.setFirstname(firstName);
        TenantInfoBean tenantInfoBeanGet;
        try {
            tenantInfoBeanGet = tenantMgtAdminServiceStub.getTenant(domainName);
            if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() != 0) {
                tenantMgtAdminServiceStub.activateTenant(domainName);
                log.info("Tenant domain " + domainName + " Activated successfully");
            } else if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() == 0) {
                tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
                tenantMgtAdminServiceStub.activateTenant(domainName);
                log.info("Tenant domain " + domainName + " created and activated successfully");
            } else {
                log.info("Tenant domain " + domainName + " already registered");
            }
        } catch (RemoteException e) {
            log.error("RemoteException thrown while adding user/tenants : ", e);
            throw new RemoteException("RemoteException thrown while adding user/tenants : ", e);
        }
    }

    /**
     * @param domainName domain name of the tenant
     */
    public void deleteTenant(String domainName) {
        try {
            tenantMgtAdminServiceStub.deactivateTenant(domainName);
            //https://wso2.org/jira/browse/TA-915 no need to delete tenant
            //tenantMgtAdminServiceStub.deleteTenant(domainName);
        } catch (RemoteException e) {
            log.error("Error while reach the tenant");
        } catch (TenantMgtAdminServiceExceptionException e) {
            log.error("No such tenant found");
        }
    }

    /**
     * deactivate tenant
     * @param domainName
     * @throws RemoteException
     * @throws TenantMgtAdminServiceExceptionException
     */
    public void deactivateTenant(String domainName) throws RemoteException, TenantMgtAdminServiceExceptionException {
        try {
            tenantMgtAdminServiceStub.deactivateTenant(domainName);
        } catch (RemoteException e) {
            log.error("Error while reach the tenant");
            throw new RemoteException("RemoteException thrown while retrieving user/tenants : ", e);
        } catch (TenantMgtAdminServiceExceptionException e) {
            log.error("No such tenant found");
            throw new TenantMgtAdminServiceExceptionException("RemoteException thrown while" +
                                                              " deactivating tenant : ", e);
        }
    }

    public TenantInfoBean getTenant(String tenantDomain)
            throws TenantMgtAdminServiceExceptionException, RemoteException {
        TenantInfoBean getTenantBean;
        try {
            getTenantBean = tenantMgtAdminServiceStub.getTenant(tenantDomain);
            assert getTenantBean == null : "Domain Name not found";
        } catch (RemoteException e) {
            log.error("RemoteException thrown while retrieving user/tenants : ", e);
            throw new RemoteException("RemoteException thrown while retrieving user/tenants : ", e);
        }
        return getTenantBean;
    }

    public void updateTenant(TenantInfoBean infoBean)
            throws TenantMgtAdminServiceExceptionException, RemoteException {
        try {
            tenantMgtAdminServiceStub.updateTenant(infoBean);
        } catch (RemoteException e) {
            log.error("RemoteException thrown while retrieving user/tenants : ", e);
            throw new RemoteException("RemoteException thrown while retrieving user/tenants : ", e);
        }
    }
}
