/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.wso2.carbon.apimgt.importexport.utils;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.importexport.APIExportException;
import org.wso2.carbon.apimgt.importexport.APIImportExportConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class provides authentication facility for importing and exporting APIs
 * Basic authentication is used for this purpose
 * Users with admin roles are only eligible for accessing those JAX-RS services
 */
public class AuthenticatorUtil {

    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Basic";
    private static final Log log = LogFactory.getLog(AuthenticatorUtil.class);
    private static final String APIM_ADMIN_PERMISSION = "/permission/admin/manage/apim_admin";
    private static final String APIM_LOGIN_PERMISSION = "/permission/admin/login";
    private static final String APIM_API_CREATE_PERMISSION = "/permission/admin/manage/api/create";

    private AuthenticatorUtil() {
    }

    /**
     * Checks whether received credentials for accessing API is authorized for exporting and
     * importing APIs
     *
     * @param headers HTTP headers of the received request
     * @return Response indicating whether authentication and authorization for accessing API got
     * succeeded
     * @throws APIExportException If an error occurs while authorizing current user
     */

    public static Response authorizeUser(HttpHeaders headers) throws APIExportException {

        AuthenticationContext authenticationContext = getAuthenticationContext(headers);
        String username = authenticationContext.getUsername();
        String password = authenticationContext.getPassword();

        if (username == null || password == null) {
            String errorMessage = "No username or password is provided for authentication";
            log.error(errorMessage);
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorMessage)
                    .type(MediaType.APPLICATION_JSON).build();
        }

        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

            APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                    getAPIManagerConfigurationService().getAPIManagerConfiguration();
            String url = config.getFirstProperty(APIConstants.AUTH_MANAGER_URL);

            AuthenticationAdminStub authAdminStub = new AuthenticationAdminStub(null, url +
                    APIImportExportConstants.AUTHENTICATION_ADMIN_SERVICE_ENDPOINT);
            ServiceClient client = authAdminStub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);

            //authenticate user provided credentials
            String host = new URL(url).getHost();
            if (authAdminStub.login(username, password, host)) {
                log.info(username + " user authenticated successfully");

                ServiceContext serviceContext = authAdminStub.
                        _getServiceClient().getLastOperationContext().getServiceContext();
                String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
                String domainAwareUserName = APIUtil.getLoggedInUserInfo(sessionCookie, url).getUserName();
                authenticationContext.setDomainAwareUsername(domainAwareUserName);

                // Validation for the admin user of the domain.
                UserStoreManager userstoremanager =
                        CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager();
                String[] userRoles = userstoremanager.getRoleListOfUser(domainAwareUserName);
                String adminRoleName = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                        .getRealmConfiguration().getAdminRoleName();
                for (String userRole : userRoles) {
                    if (adminRoleName.equalsIgnoreCase(userRole)) {
                        log.info(username + " is authorized to import and export APIs");
                        return Response.ok().entity(authenticationContext).build();
                    }
                }

                // Validation for a user having API-M Admin, API Create and Login permissions.
                AuthorizationManager authorizationManager = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                        .getAuthorizationManager();
                if ((authorizationManager.isUserAuthorized(domainAwareUserName, APIM_ADMIN_PERMISSION,
                        CarbonConstants.UI_PERMISSION_ACTION)) && (authorizationManager.isUserAuthorized
                        (domainAwareUserName, APIM_LOGIN_PERMISSION, CarbonConstants.UI_PERMISSION_ACTION)) &&
                        (authorizationManager.isUserAuthorized(domainAwareUserName, APIM_API_CREATE_PERMISSION,
                                CarbonConstants.UI_PERMISSION_ACTION))) {
                    log.info(username + " is authorized to import and export APIs");
                    return Response.ok().entity(authenticationContext).build();
                }

                return Response.status(Response.Status.FORBIDDEN).entity("User Authorization Failed")
                        .type(MediaType.APPLICATION_JSON).build();

            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("User Authentication Failed")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            // This is to catch the following exceptions that can occur while authenticating, UserStoreException,
            // RemoteException, MalformedURLException, LoginAuthenticationExceptionException, ExceptionException
        } catch (Exception e) {
            String errorMessage = "Error while authenticating the user";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Extracts the user provided username and password for authentication.
     *
     * @param headers Http Headers of the request
     * @return AuthenticationContext including the username and password
     * @throws APIExportException If an error occurs while extracting username and password from the header
     */
    private static AuthenticationContext getAuthenticationContext(HttpHeaders headers) throws APIExportException {

        AuthenticationContext authenticationContext = new AuthenticationContext();
        //Fetch authorization header
        final List<String> authorization = headers.getRequestHeader(AUTHORIZATION_PROPERTY);

        //If no authorization information present, return an empty authentication context
        if (authorization == null || authorization.isEmpty()) {
            return authenticationContext;
        }

        //Get encoded username and password
        final String encodedUserPassword = authorization.get(0).replaceFirst(AUTHENTICATION_SCHEME + " ", "");

        //Decode username and password
        String usernameAndPassword;
        usernameAndPassword = StringUtils.newStringUtf8(Base64.decodeBase64(encodedUserPassword.getBytes()));

        if (usernameAndPassword != null) {
            //Split username and password tokens
            final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
            authenticationContext.setUsername(tokenizer.nextToken());
            authenticationContext.setPassword(tokenizer.nextToken());
        }
        return authenticationContext;
    }
}
