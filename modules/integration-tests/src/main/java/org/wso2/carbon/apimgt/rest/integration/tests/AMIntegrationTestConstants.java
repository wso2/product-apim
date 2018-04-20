/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests;

/**
 * Constant class for integration tests
 */
public class AMIntegrationTestConstants {
    public static final String DCRM_REST_API_URL = "/api/identity/oauth2/dcr/v1.0/register";
    public static final String TOKEN_REST_API_URL = "/api/auth/oauth2/v1.0/token";
    public static final String SCIM_REST_API_URL = "/api/identity/scim2/v1.0";
    public static final String DEFAULT_SCOPES = "apim:api_view apim:api_create apim:api_update apim:api_delete " +
            "apim:apidef_update apim:api_publish apim:subscription_view apim:subscription_block " +
            "apim:dedicated_gateway apim:external_services_discover apim:subscribe";
    public static final String PUBLISHER_REST_API_URL = "/api/am/publisher/v1.0";
    public static final String STORE_REST_API_URL = "/api/am/store/v1.0";
    public static final String ADMIN_REST_API_URL = "/api/am/admin/v1.0";
    public static final String DEFAULT_LIFE_CYCLE_CHECK_LIST = "Deprecate old versions after publish the API:false,Require " +
            "re-subscription when publish the API:false";
}
