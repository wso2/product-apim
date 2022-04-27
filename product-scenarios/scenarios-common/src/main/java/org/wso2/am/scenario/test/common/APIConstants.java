/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.test.common;

/**
 * Class that defines API related constant field values 
 * 
 */
public class APIConstants {
    
    public static class DefaultVersion {
        public static final String ENABLED = "default_version";
    }

    public static class StoreVisibility {
        public static final String RESTRICTED = "restricted";
    }

    public static class ResponseCaching {
        public static final String  ENABLED = "enabled";
        public static final String  DISABLED = "disabled";
    }

    public static class SubscriptionAvailability {
        public static final String ALL_TENANTS = "all_tenants";
    }

    public static class TRANSPORT {
        public static final String HTTP = "http";
        public static final String HTTPS = "https";
    }
}
