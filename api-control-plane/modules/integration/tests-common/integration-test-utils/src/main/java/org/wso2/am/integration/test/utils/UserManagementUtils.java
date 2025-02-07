package org.wso2.am.integration.test.utils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.user.mgt.stub.types.carbon.ClaimValue;

import java.io.IOException;
import java.rmi.RemoteException;

import static org.wso2.am.integration.test.Constants.APPLICATION_JSON;
import static org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils.encodeCredentials;

public class UserManagementUtils {

    private static UserAdminStub userAdminStub;
    private static final String serviceName = "UserAdmin";
    private static final Log log = LogFactory.getLog(UserManagementUtils.class);

    public static void addUser(String userName, String password, String backendURL, String[] roleList,
                               String adminUsername, String adminPassword, String email)
            throws RemoteException, UserAdminUserAdminException {

        String endPoint = backendURL + serviceName;
        userAdminStub = new UserAdminStub(endPoint);
        AuthenticateStub.authenticateStub(adminUsername, adminPassword, userAdminStub);
        ClaimValue claimValue = new ClaimValue();
        claimValue.setClaimURI("email");
        claimValue.setValue(email);

        ClaimValue[] claims = {claimValue};

        userAdminStub.addUser(userName, password, roleList, claims, null);
    }

    /**
     * Self Sign-up a new user with minimal properties
     *
     * @throws IOException
     * @throws APIManagerIntegrationTestException
     */
    public static void signupUser(String userName, String password, String firstName,
                                  String organization)  throws IOException, APIManagerIntegrationTestException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpPost postRequest = new HttpPost("https://localhost:9943/api/identity/user/v1.0/me");
        postRequest.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials("admin", "admin".toCharArray()));
        postRequest.addHeader("Content-Type", APPLICATION_JSON);

        StringEntity payload = new StringEntity(
                "{\"user\":" +
                    "{\"username\": \"" + userName + "\"," +
                    " \"password\": \"" + password + "\"," +
                     "\"claims\": " +
                            "[{\"uri\": \"http://wso2.org/claims/givenname\",\"value\": \""+ firstName +"\" }," +
                             "{\"uri\": \"http://wso2.org/claims/organization\",\"value\": \""+ organization + "\"" +
                     "}]}, " +
                   "\"properties\": []}");
        postRequest.setEntity(payload);
        CloseableHttpResponse response = client.execute(postRequest);

        if (!(response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED)) {
            log.error("Error occurred in self sing up a new user with user name " + userName);
            throw new APIManagerIntegrationTestException("Error occurred in self sign-up a new user. Expected " +
                    "response code " + HttpStatus.SC_CREATED + ", but returned " + response.getStatusLine().getStatusCode());
        }
    }


    /**
     * Self Sign-up a new user with minimal properties
     *
     * @throws IOException
     * @throws APIManagerIntegrationTestException
     */
    public static void signupUser(String userName, String password, String firstName,
                                  String organization, String email)  throws IOException, APIManagerIntegrationTestException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpPost postRequest = new HttpPost("https://localhost:9943/api/identity/user/v1.0/me");
        postRequest.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials("admin", "admin".toCharArray()));
        postRequest.addHeader("Content-Type", APPLICATION_JSON);

        StringEntity payload = new StringEntity(
                "{\"user\":" +
                        "{\"username\": \"" + userName + "\"," +
                        " \"password\": \"" + password + "\"," +
                        "\"claims\": " +
                        "[{\"uri\": \"http://wso2.org/claims/givenname\",\"value\": \""+ firstName +"\" }," +
                        "{\"uri\": \"http://wso2.org/claims/emailaddress\",\"value\": \""+ email +"\" }," +
                        "{\"uri\": \"http://wso2.org/claims/organization\",\"value\": \""+ organization + "\"" +
                        "}]}, " +
                        "\"properties\": []}");
        postRequest.setEntity(payload);
        CloseableHttpResponse response = client.execute(postRequest);

        if (!(response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED)) {
            log.error("Error occurred in self sing up a new user with user name " + userName);
            throw new APIManagerIntegrationTestException("Error occurred in self sign-up a new user. Expected " +
                    "response code " + HttpStatus.SC_CREATED + ", but returned " + response.getStatusLine().getStatusCode());
        }
    }
}
