/*
*  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.am.tests.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;

/**
 * Rest client for workflow admin application
 *
 */
public class WorkFlowAdminRestClient {

	 private String backEndUrl;
	    private static final String URL_SURFIX = "/admin-dashboard/site/blocks";
	    private Map<String, String> requestHeaders = new HashMap<String, String>();

	    public WorkFlowAdminRestClient(String backEndUrl) {
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
	    private String getSession(Map<String, String> responseHeaders) {
	        return responseHeaders.get("Set-Cookie");
	    }

	    private String setSession(String session) {
	        return requestHeaders.put("Cookie", session);
	    }

}
