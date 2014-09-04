/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.am.tests.util;

import org.wso2.carbon.am.tests.util.bean.APILifeCycleStateRequest;
import org.wso2.carbon.am.tests.util.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.HttpResponse;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIPublisherRestClient {
    private String backEndUrl;
    private static final String URL_SURFIX = "/publisher/site/blocks";
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    public APIPublisherRestClient(String backEndUrl) {
        this.backEndUrl = backEndUrl;
        if (requestHeaders.get("Content-Type") == null) {
            this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        }
    }

    public HttpResponse login(String userName, String password)
            throws Exception {
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/user/login/ajax/login.jag")
                , "action=login&username=" + userName + "&password=" + password + "", requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            String session = getSession(response.getHeaders());
            if (session == null) {
                throw new Exception("No session cookie found with response");
            }
            setSession(session);
            return response;
        } else {
            throw new Exception("User Login failed> " + response.getData());
        }

    }

    public HttpResponse addAPI(APIRequest apiRequest)
            throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/item-add/ajax/add.jag")
                , apiRequest.generateRequestParameters()
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Adding failed> " + response.getData());
        }
    }

    public HttpResponse updateAPI(APIRequest apiRequest)
            throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/item-add/ajax/add.jag")
                , apiRequest.generateRequestParameters("updateAPI")
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Adding failed> " + response.getData());
        }
    }


    public HttpResponse changeAPILifeCycleStatusTo(APILifeCycleStateRequest updateRequest)
            throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/life-cycles/ajax/life-cycles.jag")
                , updateRequest.generateRequestParameters()
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API LifeCycle Updating failed> " + response.getData());
        }

    }

    public HttpResponse getApi(String apiName, String provider)
            throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/listing/ajax/item-list.jag")
                , "action=getAPI&name=" + apiName + "&version=1.0.0&provider=" + provider + ""
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("Get API Information failed> " + response.getData());
        }

        /* name=YoutubeFeeds&version=1.0.0&provider=provider1&status=PUBLISHED&publishToGateway=true&action=updateStatus  */

    }

    public HttpResponse deleteApi(String name,String version,String provider)
            throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/item-add/ajax/remove.jag")
                , "action=removeAPI&name=" + name + "&version="+version+"&provider="+provider
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Deletion failed> " + response.getData());
        }
    }




    public void setHttpHeader(String headerName, String value) {
        this.requestHeaders.put(headerName, value);
    }

    public String getHttpHeader(String headerName) {
        return this.requestHeaders.get(headerName);
    }

    public void removeHttpHeader(String headerName) {
        this.requestHeaders.remove(headerName);
    }

    private String getSession(Map<String, String> responseHeaders) {
        return responseHeaders.get("Set-Cookie");
    }

    private String setSession(String session) {
        return requestHeaders.put("Cookie", session);
    }

    private boolean checkAuthentication() throws Exception {
        if (requestHeaders.get("Cookie") == null) {
            throw new Exception("No Session Cookie found. Please login first");
        }
        return true;
    }

    /**
     * @param apiName  String name of the API that need to remove
     * @param version  String version of the API that need to remove
     * @param provider String provider name of the API that need to remove
     * @return
     * @throws Exception when invocation does ont return 200 response
     */
    public HttpResponse removeAPI(String apiName, String version, String provider) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/item-add/ajax/remove.jag")
                , "action=removeAPI" + "&name=" + apiName + "&version=" + version + "&provider=" + provider
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }


    public HttpResponse addDocument(String apiName, String version, String provider, String docName, String docType, String sourceType, String docUrl, String summary, String docLocation) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
                , "action=addDocumentation" + "&provider=" + provider + "&apiName=" + apiName + "&version=" + version + "&docName=" + docName + "&docType=" + docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl
                + summary + "&docLocation=" + docLocation
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }

    /*
                docDetails.name = request.getParameter("docName");
          docDetails.content = request.getParameter("content");
          result = mod.addInlineContent(apiDetails, docDetails);
     */
    public HttpResponse inlineContent(String apiName, String version, String provider, String docName, String content, String docDetails) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
                , "action=addInlineContent" + "&provider=" + provider + "&apiName=" + apiName + "&version=" + version + "&docName=" + docName + "&content=" + content + "&docDetails=" + docDetails
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }

    public HttpResponse removeDocumentation(String apiName, String version, String provider, String docName, String docType) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
                , "action=removeDocumentation" + "&provider=" + provider + "&apiName=" + apiName + "&version=" + version + "&docName=" + docName + "&docType=" + docType
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }

    public HttpResponse getAccessTokenData(String accessToken) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/tokens/ajax/token.jag")
                , "action=getAccessTokenData" + "&accessToken=" + accessToken
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }

    public HttpResponse revokeAccessToken(String accessToken, String consumerKey, String authUser) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/tokens/ajax/revokeToken.jag")
                , "action=revokeAccessToken" + "&accessToken=" + accessToken+"&authUser="+authUser+"&consumerKey="+consumerKey
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }
    public HttpResponse revokeAccessTokenBySubscriber(String subscriberName) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/tokens/ajax/revokeToken.jag")
                , "action=revokeAccessTokenBySubscriber" + "&subscriberName=" + subscriberName
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }

    public HttpResponse updatePermissions(String tierName, String permissionType, String roles) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/tiers/ajax/tiers.jag")
                , "action=updatePermissions" + "&tierName=" + tierName + "&permissiontype=" + permissionType + "&roles=" + roles
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }


    public HttpResponse createNewAPI(String provider, String apiName, String version, String newVersion) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/overview/ajax/overview.jag")
                , "action=createNewAPI" + "&provider=" + provider + "&apiName=" + apiName + "&version=" + version +"&newVersion="+newVersion
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed> " + response.getData());
        }
    }
}
