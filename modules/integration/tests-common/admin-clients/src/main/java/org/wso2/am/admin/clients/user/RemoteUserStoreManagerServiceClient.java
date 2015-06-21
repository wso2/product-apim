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

package org.wso2.am.admin.clients.user;

import org.apache.axis2.AxisFault;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceStub;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import java.rmi.RemoteException;

/**
 * this client is used for update user claims, and related operations
 */

public class RemoteUserStoreManagerServiceClient {

    private final String serviceName = "RemoteUserStoreManagerService";
    private RemoteUserStoreManagerServiceStub remoteUserStoreManagerServiceStub;

    /**
     * Create Remote user store admin client
     * @param backEndUrl - url to log
     * @param sessionCookie - session cookie
     * @throws AxisFault
     */
    public RemoteUserStoreManagerServiceClient(String backEndUrl, String sessionCookie) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        remoteUserStoreManagerServiceStub = new RemoteUserStoreManagerServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, remoteUserStoreManagerServiceStub);
    }

    /**
     * Create Remote user store admin client using username and password
     * @param backEndUrl
     * @param userName
     * @param password
     * @throws AxisFault
     */
    public RemoteUserStoreManagerServiceClient(String backEndUrl, String userName, String password)
            throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        remoteUserStoreManagerServiceStub = new RemoteUserStoreManagerServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, remoteUserStoreManagerServiceStub);
    }

    /**
     * add user to user store
     * @param userName
     * @param credential - password
     * @param roleList - permission roles
     * @param claimValues - claim values
     * @param profileName - user profile
     * @param requirePasswordChange
     * @throws UserStoreException
     * @throws RemoteException
     * @throws RemoteUserStoreManagerServiceUserStoreExceptionException
     */
    public void addUser(String userName, String credential, String[] roleList, ClaimValue[] claimValues,
                        String profileName, boolean requirePasswordChange) throws UserStoreException, RemoteException,
            RemoteUserStoreManagerServiceUserStoreExceptionException {

        remoteUserStoreManagerServiceStub.addUser(userName, credential, roleList, claimValues,
                profileName, requirePasswordChange);
    }

    /**
     * get available profiles
     * @param userName
     * @return
     * @throws RemoteException
     * @throws RemoteUserStoreManagerServiceUserStoreExceptionException
     */
    public String[] getProfileNames(String userName) throws RemoteException,
            RemoteUserStoreManagerServiceUserStoreExceptionException {
        return remoteUserStoreManagerServiceStub.getProfileNames(userName);
    }

    /**
     * get user claim values for given claim URL's
     * @param userName
     * @param claims
     * @param profileName
     * @return
     * @throws RemoteException
     * @throws RemoteUserStoreManagerServiceUserStoreExceptionException
     */

    public ClaimValue[] getUserClaimValuesForClaims(String userName, String[] claims, String profileName)
            throws RemoteException, RemoteUserStoreManagerServiceUserStoreExceptionException {
        return remoteUserStoreManagerServiceStub.getUserClaimValuesForClaims(userName, claims, profileName);
    }

    public void setUserClaimValue(String username, String claim, String value, String profile)
            throws RemoteUserStoreManagerServiceUserStoreExceptionException, RemoteException {
        remoteUserStoreManagerServiceStub.setUserClaimValue(username, claim, value, profile);
    }

}
