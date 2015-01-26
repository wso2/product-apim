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

package org.wso2.carbon.am.tests.util.bean;


/**
 * action=updateStatus&name=YoutubeFeeds&version=1.0.0&provider=provider1&status=PUBLISHED&publishToGateway=true
 */
public class APILifeCycleStateRequest extends AbstractRequest {
    private String name;
    private String status;
    private String provider;
    private String version = "1.0.0";
    private String publishToGateway = "true";

    public APILifeCycleStateRequest(String apiName, String provider, APILifeCycleState status) {
        this.name = apiName;
        this.status = status.getState();
        this.provider = provider;
    }
    @Override
    public void setAction() {
        setAction("updateStatus");
    }

    @Override
    public void init() {
        addParameter("name",name);
        addParameter("status", status);
        addParameter("provider", provider);

        addParameter("version", version);
        addParameter("publishToGateway", publishToGateway);
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return status;
    }

    public void setState(APILifeCycleState status) {
        this.status = status.getState();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProvider() {
        return provider;
    }

     public boolean getPublishToGateway() {
        return Boolean.valueOf(publishToGateway);
    }

    public void setPublishToGateway(boolean publishToGateway) {
        this.publishToGateway = String.valueOf(publishToGateway);
    }
}
