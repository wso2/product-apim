/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package beans;

import org.wso2.carbon.apimgt.samples.utils.beans.ApiBean;
import org.wso2.carbon.apimgt.samples.utils.beans.TenantBean;

public class RawDataBean {

    private int id;
    private TenantBean tenant;
    private ApiBean api;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TenantBean getTenant() {
        return tenant;
    }

    public void setTenant(TenantBean tenant) {
        this.tenant = tenant;
    }

    public ApiBean getApi() {
        return api;
    }

    public void setApi(ApiBean api) {
        this.api = api;
    }

}
