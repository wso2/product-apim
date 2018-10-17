/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import feign.Response;
import feign.gson.GsonDecoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.core.auth.DCRMServiceStub;
import org.wso2.carbon.apimgt.core.auth.DCRMServiceStubFactory;
import org.wso2.carbon.apimgt.core.auth.OAuth2ServiceStubs;
import org.wso2.carbon.apimgt.core.auth.dto.DCRClientInfo;
import org.wso2.carbon.apimgt.core.auth.dto.OAuth2TokenInfo;
import org.wso2.carbon.apimgt.core.exception.APIManagementException;
import org.wso2.carbon.apimgt.core.util.APIMgtConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APIIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.api.GroupIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.api.GroupsApi;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.api.UserIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.api.UsersApi;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Group;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.GroupList;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.Member;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.User;
import org.wso2.carbon.apimgt.rest.integration.tests.scim.model.UserList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.ApplicationIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for Test
 */
public class TestUtil {

    private static Logger logger = LoggerFactory.getLogger(TestUtil.class);
    public static String clientId;
    private static String clientSecret;
    public static final String APIM_HOST = "https://" + getIpAddressOfContainer();
    public static final String TOKEN_ENDPOINT_URL = APIM_HOST + AMIntegrationTestConstants.TOKEN_REST_API_URL;
    public static final String DYNAMIC_CLIENT_REGISTRATION_ENDPOINT = "https://" + getIpAddressOfContainer() +
            AMIntegrationTestConstants.DCRM_REST_API_URL;
    public static final String username = "admin";
    public static final String password = "admin";
    public static final String KEY_MANAGER_CERT_ALIAS = "wso2carbon";
    public static UserGroups usersMap;
    private static Map<String, User> userMap = new HashMap<>();
    private static Set<Group> groupSet = new HashSet<>();
    private static Map<String, Application> applicationMap = new HashMap<>();
    private static Map<String, API> apiMap = new HashMap<>();

    public static Users users;
    public static Groups groups;
    public static ApplicationList applicationList;
    public static APIList apiList;

    /**
     * This methos used to generate token with username,password and scopes
     *
     * @param username requested username
     * @param password requested password
     * @param scopes   requested scopes
     * @return TokenInfo
     * @throws AMIntegrationTestException if there's any error in token generation
     */
    public static TokenInfo generateToken(String username, String password, String scopes) throws
            AMIntegrationTestException {

        if (StringUtils.isEmpty(clientId) | StringUtils.isEmpty(clientSecret)) {
            try {
                generateClient();
            } catch (APIManagementException e) {
                throw new AMIntegrationTestException(e);
            }
        }
        OAuth2ServiceStubs.TokenServiceStub tokenServiceStub = getOauth2Client();
        Response response = tokenServiceStub.generatePasswordGrantAccessToken(username, password, scopes, -1,
                clientId, clientSecret);
        return getTokenInfo(getTokenInfo(response));
    }

    public static OAuth2TokenInfo generateToken(String clientId, String clientSecret, String username, String password,
                                                String scopes) throws
            AMIntegrationTestException {

        OAuth2ServiceStubs.TokenServiceStub tokenServiceStub = getOauth2Client();
        Response response = tokenServiceStub.generatePasswordGrantAccessToken(username, password, scopes, -1,
                clientId, clientSecret);
        return getTokenInfo(response);
    }

    private static DCRClientInfo generateClient() throws APIManagementException {

        DCRClientInfo dcrClientInfo = new DCRClientInfo();
        dcrClientInfo.setClientName("apim-integration-test");
        dcrClientInfo.setClientName("apim-integration-test");
        dcrClientInfo.setGrantTypes(Arrays.asList(new String[]{"password", "client_credentials"}));
        try {
            Response response = getDcrmServiceStub(username, password).registerApplication(dcrClientInfo);
            DCRClientInfo dcrClientInfoResponse = getDCRClientInfo(response);
            clientId = dcrClientInfoResponse.getClientId();
            clientSecret = dcrClientInfoResponse.getClientSecret();
            return dcrClientInfoResponse;
        } catch (APIManagementException | IOException e) {
            logger.error("Couldn't create client", e);
            throw new APIManagementException("Couldn't create client", e);
        }
    }

    public static DCRMServiceStub getDcrmServiceStub(String username, String password) throws APIManagementException {

        return DCRMServiceStubFactory.getDCRMServiceStub(DYNAMIC_CLIENT_REGISTRATION_ENDPOINT,
                username, password, KEY_MANAGER_CERT_ALIAS);
    }

    public static DCRClientInfo getDCRClientInfo(Response response) throws IOException {

        return (DCRClientInfo) new GsonDecoder().decode(response, DCRClientInfo.class);
    }

    private static OAuth2ServiceStubs.TokenServiceStub getOauth2Client() throws AMIntegrationTestException {

        try {
            return new OAuth2ServiceStubs(TOKEN_ENDPOINT_URL, "", "", "", "wso2carbon", "admin", "admin")
                    .getTokenServiceStub();
        } catch (APIManagementException e) {
            throw new AMIntegrationTestException(e);
        }
    }

    /**
     * Utility for get Docker running host
     *
     * @return docker host
     * @throws URISyntaxException if docker Host url is malformed this will throw
     */
    public static String getIpAddressOfContainer() {

        String ip = "localhost:9443";
        String dockerHost = System.getenv("SERVER_HOST");
        if (!StringUtils.isEmpty(dockerHost)) {
            return dockerHost;
        }
        return ip;
    }

    /**
     * Utility for initialize users.yaml
     *
     * @throws AMIntegrationTestException
     */
    public static void initConfiguration() throws AMIntegrationTestException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            usersMap = objectMapper.readValue(TestUtil.class.getResource("/users.yaml"), UserGroups.class);
            users = objectMapper.readValue(TestUtil.class.getResource("/users.yaml"), Users.class);
            groups = objectMapper.readValue(TestUtil.class.getResource("/users.yaml"), Groups.class);
            applicationList = objectMapper.readValue(TestUtil.class.getResource("/users.yaml"), ApplicationList.class);
            apiList = objectMapper.readValue(TestUtil.class.getResource("/users.yaml"), APIList.class);
        } catch (IOException e) {
            throw new AMIntegrationTestException(e);
        }
        createUsers();
        createInitialApplications();
        createInitialApis();
    }

    private static void createUsers() throws AMIntegrationTestException {

        UsersApi usersApi = new ApiClient(APIM_HOST + AMIntegrationTestConstants.SCIM_REST_API_URL, username,
                password).buildClient(UsersApi.class);
        for (Map.Entry<String, String> user : users.getUsers().entrySet()) {
            UserList userList = usersApi.usersGet(0, 1, EncodingUtils.encode("userName eq " + user.getKey()));
            if (userList.getResources() == null || userList.getResources().isEmpty()) {
                User scimUser = new User().userName(user.getKey()).password(user.getValue());
                scimUser = usersApi.usersPost(scimUser);
                userMap.put(user.getKey(), scimUser);
                logger.info("User Created:" + user.getKey());
            } else {
                logger.info("User Already Exist:" + user.getKey());
                userMap.put(user.getKey(), userList.getResources().get(0));
            }

        }
        GroupsApi groupsApi = new ApiClient(APIM_HOST + AMIntegrationTestConstants.SCIM_REST_API_URL, username,
                password).buildClient(GroupsApi.class);
        GroupIndividualApi groupIndividualApi = new ApiClient(APIM_HOST + AMIntegrationTestConstants
                .SCIM_REST_API_URL, username, password).buildClient(GroupIndividualApi.class);
        groups.getGroups().forEach((group, userList) -> {
            GroupList groupList = groupsApi.groupsGet(0, 1, EncodingUtils.encode("displayName eq " + group));
            Group createdGroup = null;
            if (groupList.getResources() == null || groupList.getResources().isEmpty()) {
                Group group1 = new Group().displayName(group);
                createdGroup = groupsApi.groupsPost(group1);
                logger.info("Group Created" + group);
            } else {
                for (Group group1 : groupList.getResources()) {
                    if (group.equals(group1.getDisplayName())) {
                        logger.info("Group Already Exist" + group);
                        createdGroup = groupIndividualApi.groupsIdGet(group1.getId());
                        break;
                    }
                }
            }
            if (createdGroup != null) {
                for (String user : userList) {
                    boolean userExist = false;
                    if (createdGroup.getMembers() != null) {
                        for (Member member : createdGroup.getMembers()) {
                            if (user.equals(member.getDisplay())) {
                                logger.info("User " + user + " Already Exist in Group: " + group);
                                userExist = true;
                                break;
                            }
                        }
                    }
                    if (!userExist) {
                        logger.info("User " + user + " Added in to Group: " + group);

                        createdGroup.addMembersItem(new Member().value(userMap.get(user).getId()));
                    }
                }
                groupIndividualApi.groupsIdPut(createdGroup.getId(), createdGroup);
                logger.info("Group " + group + " Updated with users");
                groupSet.add(createdGroup);
            }
        });
    }

    public static void cleanupUsers() throws AMIntegrationTestException {

        UserIndividualApi userIndividualApi = new ApiClient(APIM_HOST + AMIntegrationTestConstants.SCIM_REST_API_URL,
                username, password).buildClient(UserIndividualApi.class);
        GroupIndividualApi groupIndividualApi = new ApiClient(APIM_HOST + AMIntegrationTestConstants
                .SCIM_REST_API_URL, username, password).buildClient(GroupIndividualApi.class);
        userMap.values().forEach(user -> {
            if (!"admin".equals(user.getUserName())) {
                userIndividualApi.usersIdDelete(user.getId());
            }
        });
        groupSet.forEach(group -> {
            if (!"admin".equals(group.getDisplayName())) {
                groupIndividualApi.groupsIdDelete(group.getId());
            }
        });
    }

    public static ApiClient getPublisherApiClient(String username, String password, String scopes) throws
            AMIntegrationTestException {

        return new ApiClient(APIM_HOST + AMIntegrationTestConstants.PUBLISHER_REST_API_URL, username, password, scopes);
    }

    public static ApiClient getStoreApiClient(String username, String password, String scopes) throws
            AMIntegrationTestException {

        return new ApiClient(APIM_HOST + AMIntegrationTestConstants.STORE_REST_API_URL, username, password, scopes);
    }

    public static ApiClient getStoreApiClientWithoutUser() throws
            AMIntegrationTestException {

        return new ApiClient(APIM_HOST + AMIntegrationTestConstants.STORE_REST_API_URL);
    }

    public static ApiClient getAdminApiClient(String username, String password, String scopes) throws
            AMIntegrationTestException {

        return new ApiClient(APIM_HOST + AMIntegrationTestConstants.ADMIN_REST_API_URL, username, password, scopes);
    }

    public static String getUser(String username) {

        return users.getUsers().get(username);
    }

    public static List<String> getGroupsOfUser(String username) {

        List<String> groupList = new ArrayList<>();
        groups.getGroups().forEach((group, userList) -> {
            if (userList.contains(username)) {
                groupList.add(group);
            }
        });
        return groupList;
    }

    public static Set<String> getApimUserGroupsOfUser(String username) {

        Set<String> userGroups = new HashSet<>();
        List<String> groupList = getGroupsOfUser(username);
        groupList.forEach(group -> {
            if (usersMap.getAdmin().contains(group)) {
                userGroups.add("admin");
            }
            if (usersMap.getCreator().contains(group)) {
                userGroups.add("creator");
            }
            if (usersMap.getPublisher().contains(group)) {
                userGroups.add("publisher");
            }
            if (usersMap.getSubscriber().contains(group)) {
                userGroups.add("subscriber");
            }
        });
        return userGroups;
    }

    public static void createInitialApplications() throws AMIntegrationTestException {

        for (org.wso2.carbon.apimgt.rest.integration.tests.util.Application application : applicationList
                .getApplications()) {
            ApplicationIndividualApi applicationIndividualApi = getStoreApiClient(application.getUser(), getUser
                    (application.getUser()), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient
                    (ApplicationIndividualApi.class);
            applicationMap.put(application.getName(), applicationIndividualApi.applicationsPost(new Application()
                    .name(application.getName()).description(application.getDescription()).
                            throttlingTier(application.getThrottlingTier())));
        }
    }

    public static void createInitialApis() throws AMIntegrationTestException {

        for (org.wso2.carbon.apimgt.rest.integration.tests.util.API api : apiList.getApis()) {
            APICollectionApi apiCollectionApi = getPublisherApiClient(api.getUser(), getUser(api.getUser()),
                    AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
            API apiToCreate = (API) SampleTestObjectCreator.ApiToCreate(api.getName(), api.getVersion(), api.getContext())
                    .policies(api.getSubscriptionPolicies()).visibleRoles(api
                            .getVisibleRoles()).visibility(API.VisibilityEnum.fromValue(api.getVisibility()))
                    .description(api.getDescription());
            apiToCreate = apiCollectionApi.apisPost(apiToCreate);
            APIIndividualApi apiIndividualApi = getPublisherApiClient(api.getUser(), getUser(api.getUser()),
                    AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
            for (String status : api.getLifecycleStatusChain()) {
                apiIndividualApi.apisChangeLifecyclePost(status, apiToCreate.getId(),
                        AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
            }
            apiMap.put(apiToCreate.getName(), apiToCreate);
        }
    }

    public static void destroyApplications() throws AMIntegrationTestException {

        for (org.wso2.carbon.apimgt.rest.integration.tests.util.Application application : applicationList
                .getApplications()) {
            ApplicationIndividualApi applicationIndividualApi = getStoreApiClient(application.getUser(), getUser
                    (application.getUser()), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient
                    (ApplicationIndividualApi.class);
            if (applicationMap.containsKey(application.getName())) {
                applicationIndividualApi.applicationsApplicationIdDelete(applicationMap.get(application.getName())
                        .getApplicationId(), "", "");
            }
        }
    }

    public static void destroyApis() throws AMIntegrationTestException {

        for (org.wso2.carbon.apimgt.rest.integration.tests.util.API api : apiList.getApis()) {
            APIIndividualApi apiIndividualApi = getPublisherApiClient(api.getUser(), getUser
                    (api.getUser()), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
            if (apiMap.containsKey(api.getName())) {
                apiIndividualApi.apisApiIdDelete(apiMap.get(api.getName()).getId(), "", "");
            }
        }
    }

    public static Application getApplication(String applicationName) {

        return applicationMap.get(applicationName);
    }

    public static API getApi(String apiName) {

        return apiMap.get(apiName);
    }

    public static OAuth2TokenInfo generateToken(String clientId, String clientSecret, String refreshToken, String
            scopes)
            throws AMIntegrationTestException {

        OAuth2ServiceStubs.TokenServiceStub tokenServiceStub = getOauth2Client();
        Response response = tokenServiceStub.generateRefreshGrantAccessToken(refreshToken, scopes, -1, clientId,
                clientSecret);
        return getTokenInfo(response);
    }

    private static TokenInfo getTokenInfo(OAuth2TokenInfo oAuth2TokenInfo) {

        return new TokenInfo(oAuth2TokenInfo.getAccessToken(), System.currentTimeMillis() +
                oAuth2TokenInfo.getExpiresIn(), oAuth2TokenInfo.getRefreshToken());
    }

    private static OAuth2TokenInfo getTokenInfo(Response response) throws AMIntegrationTestException {

        if (response.status() == APIMgtConstants.HTTPStatusCodes.SC_200_OK) {   //200 - Success
            logger.debug("A new access token is successfully generated.");
            try {
                return (OAuth2TokenInfo) new GsonDecoder().decode(response, OAuth2TokenInfo.class);
            } catch (IOException e) {
                throw new AMIntegrationTestException("Error occurred while parsing token response", e);
            }
        } else {
            throw new AMIntegrationTestException("Error occurred while Generating token");
        }
    }

}
