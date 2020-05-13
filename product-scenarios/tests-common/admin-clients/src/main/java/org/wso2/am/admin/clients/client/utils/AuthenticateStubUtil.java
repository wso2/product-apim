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
package org.wso2.am.admin.clients.client.utils;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Stub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.CarbonUtils;


public class AuthenticateStubUtil {

    private static final Log log = LogFactory.getLog(AuthenticateStubUtil.class);

    /**
     * Stub authentication method
     *
     * @param stub          valid stub
     * @param sessionCookie session cookie
     */
    public static void authenticateStub(String sessionCookie, Stub stub) {
        long soTimeout = 5 * 60 * 1000; // Three minutes

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setTimeOutInMilliSeconds(soTimeout);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);
        if (log.isDebugEnabled()) {
            log.debug("AuthenticateStub : Stub created with session " + sessionCookie);
        }
    }

    /**
     * Authenticate the given web service stub against the Product user manager. This
     * will make it possible to use the stub for invoking Product admin services.
     *
     * @param stub Axis2 service stub which needs to be authenticated
     */
    public static void authenticateStub(String userName, String password, Stub stub) {
        CarbonUtils.setBasicAccessSecurityHeaders(userName, password, stub._getServiceClient());
    }
}