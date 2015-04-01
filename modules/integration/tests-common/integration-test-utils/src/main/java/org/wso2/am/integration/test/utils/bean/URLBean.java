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

package org.wso2.am.integration.test.utils.bean;

import org.wso2.carbon.automation.engine.context.beans.ContextUrls;

/**
 * this class creates basic URL's that needs to be used
 * Do not hard code any web app names in this class, construct them outside
 * make sure you have enabled nonBlockingTransportEnabled="true"
 *
 */

public class URLBean {

    ContextUrls contextUrls = null;
    private String webAppURLHttp; // http://localhost:9763
    private String webAppURLHttps; // http://localhost:9443
    private String webAppURLNhttp; // http://localhost:8280
    private String webAppURLNhttps; // http://localhost:8243


    /**
     * base constructor
     * @param contextUrls
     */

    public URLBean(ContextUrls contextUrls) {
        this.contextUrls = contextUrls;
        buildUrls();
    }

    private void buildUrls() {

        webAppURLHttp = contextUrls.getWebAppURL();

        webAppURLHttps = contextUrls.getBackEndUrl();

        if (webAppURLHttps.endsWith("/services/")) {
            webAppURLHttps = webAppURLHttps.replace("/services/", "");
        }

        webAppURLNhttp = contextUrls.getServiceUrl();

        if (webAppURLNhttp.endsWith("/services")) {
            webAppURLNhttp = webAppURLNhttp.replace("/services", "");
        }

        webAppURLNhttps = contextUrls.getServiceUrl();

        if (webAppURLNhttps.endsWith("/services")) {
            webAppURLNhttps = webAppURLNhttps.replace("/services", "");
        }
    }


    public String getWebAppURLHttp() {
        return webAppURLHttp;
    }

    public String getWebAppURLHttps() {
        return webAppURLHttps;
    }

    public String getWebAppURLNhttp() {
        return webAppURLNhttp;
    }

    public String getWebAppURLNhttps() {
        return webAppURLNhttps;
    }
}
