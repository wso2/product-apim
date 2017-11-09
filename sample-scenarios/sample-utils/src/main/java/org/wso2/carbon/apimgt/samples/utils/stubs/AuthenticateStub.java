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

package org.wso2.carbon.apimgt.samples.utils.stubs;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Stub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * This class authenticate stubs with valid session cookie.
 */
public class AuthenticateStub {

    private static final Log log = LogFactory.getLog(AuthenticateStub.class);

    /**
     * Stub authentication method
     *
     * @param stub          valid stub
     * @param sessionCookie session cookie
     */
    public static void authenticateStub(String sessionCookie, Stub stub) {
        // Three minutes
        long soTimeout = 5 * 60 * 1000;

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setTimeOutInMilliSeconds(soTimeout);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);

        if (log.isDebugEnabled()) {
            log.debug("AuthenticateStub : Stub created with session " + sessionCookie);
        }
    }

    public static Stub authenticateStub(Stub stub, String sessionCookie, String backendURL) {
        // Three minutes
        long soTimeout = 5 * 60 * 1000;

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setTimeOutInMilliSeconds(soTimeout);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);

        option.setTo(new EndpointReference(
                backendURL + client.getServiceContext().getAxisService().getName().replaceAll("[^a-zA-Z]", "")));

        if (log.isDebugEnabled()) {
            log.debug("AuthenticateStub : Stub created with session " + sessionCookie);
        }

        return stub;
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