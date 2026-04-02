/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.test.utils.base;

/**
 * Constants for WSO2 JWT claim URIs used across integration tests.
 *
 * <p>Use these instead of inline string literals so that claim path changes
 * are fixed in one place and test intent is clear at the call site.</p>
 *
 * <pre>
 *     String given = jwt.getString(JWTClaimConstants.GIVEN_NAME);
 * </pre>
 */
public final class JWTClaimConstants {

    private JWTClaimConstants() {
    }

    // ── Standard WSO2 user claims ────────────────────────────────────────────

    public static final String GIVEN_NAME      = "http://wso2.org/claims/givenname";
    public static final String LAST_NAME       = "http://wso2.org/claims/lastname";
    public static final String EMAIL           = "http://wso2.org/claims/emailaddress";
    public static final String ROLE            = "http://wso2.org/claims/role";
    public static final String FULL_NAME       = "http://wso2.org/claims/fullname";
    public static final String SUBSCRIBER      = "http://wso2.org/claims/subscriber";
    public static final String TIER            = "http://wso2.org/claims/tier";
    public static final String KEY_TYPE        = "http://wso2.org/claims/keytype";
    public static final String USER_TYPE       = "http://wso2.org/claims/usertype";
    public static final String APP_NAME        = "http://wso2.org/claims/applicationname";
    public static final String APP_TIER        = "http://wso2.org/claims/applicationtier";
    public static final String APP_ID          = "http://wso2.org/claims/applicationid";
    public static final String APP_UUID        = "http://wso2.org/claims/applicationUUId";
    public static final String API_CONTEXT     = "http://wso2.org/claims/apicontext";
    public static final String API_VERSION     = "http://wso2.org/claims/version";
    public static final String API_NAME        = "http://wso2.org/claims/apiname";
    public static final String APP_ATTRIBUTES  = "http://wso2.org/claims/applicationAttributes";

    // ── Custom / profile claims used in tests ───────────────────────────────

    public static final String MOBILE          = "mobile";
    public static final String ORGANIZATION    = "organization";
}
