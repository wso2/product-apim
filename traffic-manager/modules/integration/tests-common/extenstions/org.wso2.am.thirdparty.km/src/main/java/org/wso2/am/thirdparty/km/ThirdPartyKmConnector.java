package org.wso2.am.thirdparty.km;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.AccessTokenRequest;
import org.wso2.carbon.apimgt.api.model.KeyManagerConfiguration;
import org.wso2.carbon.apimgt.api.model.OAuthAppRequest;
import org.wso2.carbon.apimgt.api.model.OAuthApplicationInfo;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;

import java.util.Map;
import java.util.Set;


public class ThirdPartyKmConnector extends AbstractKeyManager {

    @Override
    public OAuthApplicationInfo createApplication(OAuthAppRequest oauthAppRequest) throws APIManagementException {

        return null;
    }

    @Override
    public OAuthApplicationInfo updateApplication(OAuthAppRequest appInfoDTO) throws APIManagementException {

        return null;
    }

    @Override
    public void deleteApplication(String consumerKey) throws APIManagementException {

    }

    @Override
    public OAuthApplicationInfo retrieveApplication(String consumerKey) throws APIManagementException {

        return null;
    }

    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest tokenRequest) throws APIManagementException {

        return null;
    }

    @Override
    public String getNewApplicationConsumerSecret(AccessTokenRequest tokenRequest) throws APIManagementException {

        return null;
    }

    @Override
    public AccessTokenInfo getTokenMetaData(String accessToken) throws APIManagementException {

        return null;
    }

    @Override
    public KeyManagerConfiguration getKeyManagerConfiguration() throws APIManagementException {

        return configuration;
    }

    @Override
    public OAuthApplicationInfo buildFromJSON(String jsonInput) throws APIManagementException {

        return null;
    }

    @Override
    public OAuthApplicationInfo mapOAuthApplication(OAuthAppRequest appInfoRequest) throws APIManagementException {

        return appInfoRequest.getOAuthApplicationInfo();
    }

    @Override
    public void loadConfiguration(KeyManagerConfiguration configuration) throws APIManagementException {
        this.configuration = configuration;
    }

    @Override
    public boolean registerNewResource(API api, Map resourceAttributes) throws APIManagementException {

        return false;
    }

    @Override
    public boolean canHandleToken(String accessToken) throws APIManagementException {
        return false;
    }

    @Override
    public Map getResourceByApiId(String apiId) throws APIManagementException {

        return null;
    }

    @Override
    public boolean updateRegisteredResource(API api, Map resourceAttributes) throws APIManagementException {

        return false;
    }

    @Override
    public void deleteRegisteredResourceByAPIId(String apiID) throws APIManagementException {

    }

    @Override
    public void deleteMappedApplication(String consumerKey) throws APIManagementException {

    }

    @Override
    public Set<String> getActiveTokensByConsumerKey(String consumerKey) throws APIManagementException {

        return null;
    }

    @Override
    public AccessTokenInfo getAccessTokenByConsumerKey(String consumerKey) throws APIManagementException {

        return null;
    }

    @Override
    public Map<String, Set<Scope>> getScopesForAPIS(String apiIdsString) throws APIManagementException {

        return null;
    }

    @Override
    public void registerScope(Scope scope) throws APIManagementException {

    }

    @Override
    public Scope getScopeByName(String name) throws APIManagementException {

        return null;
    }

    @Override
    public Map<String, Scope> getAllScopes() throws APIManagementException {

        return null;
    }

    @Override
    public void deleteScope(String scopeName) throws APIManagementException {

    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {

    }

    @Override
    public boolean isScopeExists(String scopeName) throws APIManagementException {

        return false;
    }

    @Override
    public String getType() {

        return ThirdPartyKMConstants.KEY_MANAGER_TYPE;
    }
}
