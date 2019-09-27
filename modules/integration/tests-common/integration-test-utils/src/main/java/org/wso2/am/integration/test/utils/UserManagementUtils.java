package org.wso2.am.integration.test.utils;

import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.user.mgt.stub.types.carbon.ClaimValue;

import java.rmi.RemoteException;

public class UserManagementUtils {


    private static UserAdminStub userAdminStub;
    private static final String serviceName = "UserAdmin";

    public static void addUser(String userName, String password, String backendURL, String[] roleList,
                               String adminUsername, String adminPassword, String email) throws RemoteException, UserAdminUserAdminException {

        String endPoint = backendURL + serviceName;
        userAdminStub = new UserAdminStub(endPoint);
        AuthenticateStub.authenticateStub(adminUsername, adminPassword, userAdminStub);
        ClaimValue claimValue = new ClaimValue();
        claimValue.setClaimURI("email");
        claimValue.setValue(email);

        ClaimValue[] claims = {claimValue};

        userAdminStub.addUser(userName, password, roleList, claims, null);
    }
}
