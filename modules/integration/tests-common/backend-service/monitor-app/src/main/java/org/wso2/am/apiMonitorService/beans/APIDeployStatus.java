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

import org.wso2.carbon.rest.api.APIData;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "apiStatus")
public class APIDeployStatus {
    boolean isApiExists = false;
    APIData apiData;

    public void setIsApiExists(boolean isApiExists) {
        this.isApiExists = isApiExists;
    }

    public boolean getIsApiExists() {
        return isApiExists;
    }

    public void setApiData(APIData apiData) {
        this.apiData = apiData;
    }

    public APIData getApiData() {
        return apiData;
    }
}
