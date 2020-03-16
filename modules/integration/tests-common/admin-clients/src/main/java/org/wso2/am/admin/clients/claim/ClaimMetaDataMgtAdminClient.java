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
package org.wso2.am.admin.clients.claim;

import org.apache.axis2.AxisFault;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceStub;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.ClaimDialectDTO;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.ExternalClaimDTO;

import java.rmi.RemoteException;

public class ClaimMetaDataMgtAdminClient {

    private final String service = "ClaimMetadataManagementService";
    private ClaimMetadataManagementServiceStub claimMetadataManagementServiceStub;

    /**
     * create Remote claim client
     *
     * @param backEndUrl    - url to log
     * @param sessionCookie - session cookie
     * @throws AxisFault
     */
    public ClaimMetaDataMgtAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + service;
        claimMetadataManagementServiceStub = new ClaimMetadataManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, claimMetadataManagementServiceStub);
    }

    /**
     * Create admin client using username and password
     *
     * @param backEndUrl
     * @param userName
     * @param password
     * @throws AxisFault
     */
    public ClaimMetaDataMgtAdminClient(String backEndUrl, String userName, String password)
            throws AxisFault {

        String endPoint = backEndUrl + service;
        claimMetadataManagementServiceStub = new ClaimMetadataManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, claimMetadataManagementServiceStub);
    }

    public void addClaimDialect(ClaimDialectDTO claimDialectDTO)
            throws RemoteException, ClaimMetadataManagementServiceClaimMetadataException {

        claimMetadataManagementServiceStub.addClaimDialect(claimDialectDTO);
    }

    public void addExternalClaim(String externalClaimDialectUri, String externalClaimUri, String localClaimUri)
            throws RemoteException, ClaimMetadataManagementServiceClaimMetadataException {
        ExternalClaimDTO externalClaimDTO = new ExternalClaimDTO();
        externalClaimDTO.setExternalClaimDialectURI(externalClaimDialectUri);
        externalClaimDTO.setExternalClaimURI(externalClaimUri);
        externalClaimDTO.setMappedLocalClaimURI(localClaimUri);
        claimMetadataManagementServiceStub.addExternalClaim(externalClaimDTO);
    }

    public void updateExternalClaim(ExternalClaimDTO externalClaimDTO)
            throws RemoteException, ClaimMetadataManagementServiceClaimMetadataException {

        claimMetadataManagementServiceStub.updateExternalClaim(externalClaimDTO);
    }

    public void removeExternalClaim(String externalClaimDialectUri, String externalClaim)
            throws RemoteException, ClaimMetadataManagementServiceClaimMetadataException {

        claimMetadataManagementServiceStub.removeExternalClaim(externalClaimDialectUri, externalClaim);
    }

}
