package org.wso2.carbon.apimgt.rest.integration.tests.util;

import feign.Response;
import feign.gson.GsonDecoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.core.auth.DCRMServiceStubFactory;
import org.wso2.carbon.apimgt.core.auth.OAuth2ServiceStubs;
import org.wso2.carbon.apimgt.core.auth.dto.DCRClientInfo;
import org.wso2.carbon.apimgt.core.auth.dto.OAuth2TokenInfo;
import org.wso2.carbon.apimgt.core.exception.APIManagementException;
import org.wso2.carbon.apimgt.core.exception.ExceptionCodes;
import org.wso2.carbon.apimgt.core.exception.KeyManagementException;
import org.wso2.carbon.apimgt.core.util.APIMgtConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

public class TestUtil {
    private static Logger logger = LoggerFactory.getLogger(TestUtil.class);

    public static TokenInfo accessTokenInfo;
    public static String clientId;
    private static String clientSecret;
    public static final String TOKEN_ENDPOINT_URL = "https://" + getIpAddressOfContainer() +
            ":9443/api/auth/oauth2/v1.0/token";
    public static final String DYNAMIC_CLIENT_REGISTRATION_ENDPOINT = "https://" + getIpAddressOfContainer() +
            ":9443/api/identity/oauth2/dcr/v1.0/register";
    public static final String username = "admin";
    public static final String password = "admin";
    public static final String scopes = "apim:api_view,apim:api_create, apim:api_update, apim:api_delete, " +
            "apim:apidef_update, apim:api_publish,apim:subscription_view, apim:subscription_block," +
            "apim:dedicated_gateway,apim:external_services_discover";
    public static final String OAUTH2_SECURITY = "OAuth2Security";

    public static TokenInfo getToken(String username, String password) throws AMIntegrationTestException {
        try {

            if (accessTokenInfo != null) {
                if (accessTokenInfo.getExpiryEpochTime() <= System.currentTimeMillis()) {
                    generateToken(username, password, scopes);
                }
            } else {
                generateToken(username, password, scopes);
            }
            return accessTokenInfo;
        } catch (APIManagementException e) {
            throw new AMIntegrationTestException("Couldn't generate Token", e);
        }
    }

    private static void generateToken(String username, String password, String scopes) throws APIManagementException {
        if (StringUtils.isEmpty(clientId) | StringUtils.isEmpty(clientSecret)) {
            generateClient();
        }
        OAuth2ServiceStubs.TokenServiceStub tokenServiceStub = getOauth2Client();
        Response response = tokenServiceStub.generatePasswordGrantAccessToken(username, password, scopes, -1,
                clientId, clientSecret);
        if (response.status() == APIMgtConstants.HTTPStatusCodes.SC_200_OK) {   //200 - Success
            logger.debug("A new access token is successfully generated.");
            try {
                OAuth2TokenInfo oAuth2TokenInfo = (OAuth2TokenInfo) new GsonDecoder().decode(response,
                        OAuth2TokenInfo.class);
                accessTokenInfo = new TokenInfo(oAuth2TokenInfo.getAccessToken(), System.currentTimeMillis() +
                        oAuth2TokenInfo.getExpiresIn());
            } catch (IOException e) {
                throw new KeyManagementException("Error occurred while parsing token response", e,
                        ExceptionCodes.ACCESS_TOKEN_GENERATION_FAILED);
            }
        }
    }

    public static DCRClientInfo generateClient() throws APIManagementException {
        DCRClientInfo dcrClientInfo = new DCRClientInfo();
        dcrClientInfo.setClientName("apim-integration-test");
        dcrClientInfo.setGrantTypes(Arrays.asList(new String[]{"password", "client_credentials"}));
        try {
            Response response = DCRMServiceStubFactory.getDCRMServiceStub(DYNAMIC_CLIENT_REGISTRATION_ENDPOINT,
                    username, password, "wso2carbon").registerApplication(dcrClientInfo);
            DCRClientInfo dcrClientInfoResponse = (DCRClientInfo) new GsonDecoder().decode(response,
                    DCRClientInfo.class);
            clientId = dcrClientInfoResponse.getClientId();
            clientSecret = dcrClientInfoResponse.getClientSecret();
            return dcrClientInfoResponse;
        } catch (APIManagementException | IOException e) {
            logger.error("Couldn't create client", e);
            throw new APIManagementException("Couldn't create client", e);
        }
    }

    private static OAuth2ServiceStubs.TokenServiceStub getOauth2Client() throws APIManagementException {
        return new OAuth2ServiceStubs(TOKEN_ENDPOINT_URL, "", "", "wso2carbon", "admin", "admin").getTokenServiceStub();
    }

    /**
     * Utility for get Docker running host
     *
     * @return docker host
     * @throws URISyntaxException if docker Host url is malformed this will throw
     */
    public static String getIpAddressOfContainer() {
        String ip = "localhost";
        String dockerHost = System.getenv("SERVER_HOST");
        if (!StringUtils.isEmpty(dockerHost)) {
            return dockerHost;
        }
        return ip;
    }
}
