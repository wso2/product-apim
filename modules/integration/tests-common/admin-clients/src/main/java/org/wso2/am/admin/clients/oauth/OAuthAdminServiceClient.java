/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.admin.clients.oauth;

import org.apache.axis2.AxisFault;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceIdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceStub;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;

import java.rmi.RemoteException;

public class OAuthAdminServiceClient {

    String service = "OAuthAdminService";
    private OAuthAdminServiceStub oAuthAdminServiceStub;

    /**
     * create Remote claim client
     *
     * @param backEndUrl    - url to log
     * @param sessionCookie - session cookie
     * @throws AxisFault
     */
    public OAuthAdminServiceClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + service;
        oAuthAdminServiceStub = new OAuthAdminServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, oAuthAdminServiceStub);
    }

    /**
     * Create admin client using username and password
     *
     * @param backEndUrl
     * @param userName
     * @param password
     * @throws AxisFault
     */
    public OAuthAdminServiceClient(String backEndUrl, String userName, String password)
            throws AxisFault {

        String endPoint = backEndUrl + service;
        oAuthAdminServiceStub = new OAuthAdminServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, oAuthAdminServiceStub);
    }

    public void updateScope(String scope, String[] addedClaims, String[] removedClaims)
            throws OAuthAdminServiceIdentityOAuthAdminException, RemoteException {

        oAuthAdminServiceStub.updateScope(scope, addedClaims, removedClaims);
    }

    public OAuthConsumerAppDTO getOAuthApplicationData(String consumerKey)
            throws OAuthAdminServiceIdentityOAuthAdminException, RemoteException {
        return oAuthAdminServiceStub.getOAuthApplicationData(consumerKey);
    }

    public void registerOAuthApplicationData(OAuthConsumerAppDTO application) throws RemoteException, OAuthAdminServiceIdentityOAuthAdminException {
        oAuthAdminServiceStub.registerOAuthApplicationData(application);
    }

    public OAuthConsumerAppDTO getOAuthAppByName(String applicationName) throws Exception {
        return oAuthAdminServiceStub.getOAuthApplicationDataByAppName(applicationName);
    }

}
