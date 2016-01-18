/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.apimonitorservice.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "WebAppData")
public class WebAppData {

    String webAppName;
    String webAppState;
    String webAppFile;
    String contextPath;


    public String getWebAppState() {
        return webAppState;
    }

    public void setWebAppState(String webAppState) {
        this.webAppState = webAppState;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getWebAppFile() {
        return webAppFile;
    }

    public void setWebAppFile(String webAppFile) {
        this.webAppFile = webAppFile;
    }

    public String getWebAppName() {
        return webAppName;
    }

    public void setWebAppName(String webAppName) {
        this.webAppName = webAppName;
    }
}
