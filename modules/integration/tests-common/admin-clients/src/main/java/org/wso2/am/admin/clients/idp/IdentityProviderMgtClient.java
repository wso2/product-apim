/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.admin.clients.idp;

import org.apache.axis2.AxisFault;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider;
import org.wso2.carbon.idp.mgt.stub.IdentityProviderMgtServiceIdentityProviderManagementExceptionException;
import org.wso2.carbon.idp.mgt.stub.IdentityProviderMgtServiceStub;

import java.rmi.RemoteException;

/**
 * IDP client to create IDP.
 */
public class IdentityProviderMgtClient {

    IdentityProviderMgtServiceStub identityProviderMgtServiceStub;

    /**
     * create Remote claim client
     *
     * @param backEndUrl    - url to log
     * @param sessionCookie - session cookie
     * @throws AxisFault
     */
    public IdentityProviderMgtClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + "IdentityProviderMgtService";
        identityProviderMgtServiceStub = new IdentityProviderMgtServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, identityProviderMgtServiceStub);
    }

    public void addIDP(IdentityProvider identityProvider) throws RemoteException,
            IdentityProviderMgtServiceIdentityProviderManagementExceptionException {

        identityProviderMgtServiceStub.addIdP(identityProvider);
    }

    public void deleteIdp(String idpName) throws RemoteException,
            IdentityProviderMgtServiceIdentityProviderManagementExceptionException {

        identityProviderMgtServiceStub.deleteIdP(idpName);
    }
}
