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

import org.wso2.carbon.apimgt.api.model.*;

import java.util.HashSet;
import java.util.Set;

public class APIBean extends API {
    private String tags;
    private String availableTiers;
    public APIBean(APIIdentifier id) {
        super(id);
    }

    public void setAvailableTiers(String availableTiers) {
        this.availableTiers = availableTiers;
    }

    public void setTags(String tags) {
        this.tags = tags;
        Set<String> stringSet = new HashSet<String>();
        String[] strings =tags.split(",");
        for (String str :strings){
          stringSet.add(str);
        }
        super.addTags(stringSet);
    }
}
