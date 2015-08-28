/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.am.integration.tests.other;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import waffle.apache.NegotiateAuthenticator;
import waffle.util.Base64;
import waffle.windows.auth.IWindowsCredentialsHandle;
import waffle.windows.auth.impl.WindowsAccountImpl;
import waffle.windows.auth.impl.WindowsCredentialsHandleImpl;
import waffle.windows.auth.impl.WindowsSecurityContextImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertNotNull;

public class NTLMTestCase extends APIMIntegrationBaseTest {
    private APIStoreRestClient apiStore;
    static NegotiateAuthenticator _authenticator = null;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    @Test(groups = {"wso2.am"}, description = "This test method only runs on windows environment.We exchange NTLM " +
            "token for access_token")
    public void testGenerateNTLMTokensTestCase() throws Exception {

        String securityPackage = "Negotiate";
        _authenticator = new NegotiateAuthenticator();
        String clientToken;
        IWindowsCredentialsHandle clientCredentials;
        WindowsSecurityContextImpl clientContext;
        // client credentials handle
        clientCredentials = WindowsCredentialsHandleImpl.getCurrent(securityPackage);
        clientCredentials.initialize();
        // initial client security context
        clientContext = new WindowsSecurityContextImpl();
        clientContext.setPrincipalName(WindowsAccountImpl.getCurrentUsername());

        clientContext.setCredentialsHandle(clientCredentials.getHandle());
        clientContext.setSecurityPackage(securityPackage);
        clientContext.initialize(null, null, WindowsAccountImpl.getCurrentUsername());
        //NTLM token.
        clientToken = Base64.encode(clientContext.getToken());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiStore.addApplication("NTLM", "Unlimited", "some_url", "NewApp");

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("NTLM");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);

        // get Consumer Key and Consumer Secret
        String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        Thread.sleep(4000);
        HttpClient httpclient = new DefaultHttpClient();
        byte[] userKey = (consumerKey + ":" + consumerSecret).getBytes();
        String encoding = Base64Utils.encode(userKey);
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        HttpPost httppost = new HttpPost(String.valueOf(tokenEndpointURL));
        httppost.setHeader("Authorization", "Basic " + encoding);
        List<NameValuePair> paramVals = new ArrayList<NameValuePair>();
        paramVals.add(new BasicNameValuePair("grant_type", "iwa:ntlm"));
        paramVals.add(new BasicNameValuePair("windows_token", clientToken));
        HttpResponse response;
        httppost.setEntity(new UrlEncodedFormEntity(paramVals, "UTF-8"));
        response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        responseString = EntityUtils.toString(entity, "UTF-8");
        JSONObject accessTokenGenerationResponse = new JSONObject(responseString);
        String accessToken = accessTokenGenerationResponse.getString("access_token");
        assertNotNull(accessToken, "Error while getting access token.. access token is empty");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                gatewayContext.getContextTenant().getContextUser().getPassword(),
                storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
    }
}