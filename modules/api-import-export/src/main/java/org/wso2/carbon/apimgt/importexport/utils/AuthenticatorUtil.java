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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.apimgt.importexport.APIExportException;
import org.wso2.carbon.apimgt.importexport.APIImportExportConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.List;
import java.util.StringTokenizer;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class provides authentication facility for importing and exporting APIs
 * Basic authentication is used for this purpose
 * Users with admin roles are only eligible for accessing those JAX-RS services
 */
public class AuthenticatorUtil {

    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Basic";
    private static final Log log = LogFactory.getLog(AuthenticatorUtil.class);
    private static String username;
    private static String password;
    public static final String APIM_API_IMPORT_PERMISSION = "/permission/admin/manage/api/import";
    public static final String APIM_API_EXPORT_PERMISSION = "/permission/admin/manage/api/export";

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

    public static Response authorizeUser(HttpHeaders headers, String action) throws APIExportException {
        if (!isValidCredentials(headers)) {
            String message = "Credentials are not provided for authentication";
            log.error(message);
            return Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
        }

        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

            UserStoreManager userstoremanager =
                    CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager();

            AuthorizationManager authorizationManager = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                    .getAuthorizationManager();

            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(username);

            //authenticate user provided credentials
            if (userstoremanager.authenticate(tenantAwareUsername, password)) {
                log.info(username + " user authenticated successfully");
                //Get admin role name of the current domain
                String adminRoleName = CarbonContext
                                       .getThreadLocalCarbonContext()
                                       .getUserRealm()
                                       .getRealmConfiguration()
                                       .getAdminRoleName();

                String[] userRoles = userstoremanager.getRoleListOfUser(tenantAwareUsername);

                //user is only authorized for exporting and importing if he is an admin of his
                // domain
                for (String userRole : userRoles) {
                    if (adminRoleName.equalsIgnoreCase(userRole)) {
                        log.info(username + " is authorized to import and export APIs");
                        return Response.ok().build();
                    }
                }
                if (APIImportExportConstants.IMPORT_ACTION.equals(action) && authorizationManager
                        .isUserAuthorized(tenantAwareUsername, APIM_API_IMPORT_PERMISSION,
                                CarbonConstants.UI_PERMISSION_ACTION)) {
                    log.info(username + " is authorized to import APIs");
                    return Response.ok().build();
                } else if (APIImportExportConstants.EXPORT_ACTION.equals(action) && authorizationManager
                        .isUserAuthorized(tenantAwareUsername, APIM_API_EXPORT_PERMISSION,
                                CarbonConstants.UI_PERMISSION_ACTION)) {
                    log.info(username + " is authorized to export APIs");
                    return Response.ok().build();
                }
                return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized for the " +
                        "performed action.").type(MediaType.APPLICATION_JSON).build();

            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("User Authentication Failed")
                        .type(MediaType.APPLICATION_JSON).build();
            }

        } catch (UserStoreException e) {
            String errorMessage = "Error while accessing user configuration";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    /**
     * Checks whether user has provided non blank username and password for authentication
     *
     * @param headers Http Headers of the request
     * @return boolean Whether a user name and password has been provided for authentication
     * @throws APIExportException If an error occurs while extracting username and password from
     *                            the header
     */
    private static boolean isValidCredentials(HttpHeaders headers) throws APIExportException {

        //Fetch authorization header
        final List<String> authorization = headers.getRequestHeader(AUTHORIZATION_PROPERTY);

        //If no authorization information present; block access
        if (authorization == null || authorization.isEmpty()) {
            return false;
        }

        //Get encoded username and password
        final String encodedUserPassword = authorization.get(0).replaceFirst(AUTHENTICATION_SCHEME + " ", "");

        //Decode username and password
        String usernameAndPassword;
        usernameAndPassword = StringUtils.newStringUtf8(Base64.decodeBase64(encodedUserPassword.getBytes()));

        if (usernameAndPassword != null) {
            //Split username and password tokens
            final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
            username = tokenizer.nextToken();
            password = tokenizer.nextToken();

            if (username != null && password != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieve authenticated user name for the current session
     *
     * @return User name
     */
    public static String getAuthenticatedUserName() {
        return username;
    }
}
