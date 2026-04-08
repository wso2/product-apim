/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks resources (APIs, applications, subscriptions) created during a test so that only
 * those specific resources are deleted in teardown — preventing accidental deletion of
 * resources owned by other concurrently-running tests.
 *
 * Usage:
 * <pre>
 *     // In test:
 *     String apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
 *     resourceTracker.trackAPI(apiId);
 *
 *     // In @AfterClass:
 *     resourceTracker.cleanup(restAPIPublisher, restAPIStore);
 * </pre>
 */
public class TestResourceTracker {

    private static final Log log = LogFactory.getLog(TestResourceTracker.class);

    private final List<String> apiIds = new ArrayList<>();
    private final List<String> applicationIds = new ArrayList<>();
    private final List<String> apiProductIds = new ArrayList<>();

    public void trackAPI(String apiId) {
        if (apiId != null) {
            apiIds.add(apiId);
        }
    }

    public void trackApplication(String applicationId) {
        if (applicationId != null) {
            applicationIds.add(applicationId);
        }
    }

    public void trackAPIProduct(String apiProductId) {
        if (apiProductId != null) {
            apiProductIds.add(apiProductId);
        }
    }

    /**
     * Deletes only the resources registered with this tracker, in reverse creation order.
     * Failures are logged but do not abort remaining cleanup steps.
     */
    public void cleanup(RestAPIPublisherImpl publisher, RestAPIStoreImpl store) {
        // Delete applications first (subscriptions are cascade-deleted by the server)
        for (int i = applicationIds.size() - 1; i >= 0; i--) {
            String appId = applicationIds.get(i);
            try {
                store.deleteApplication(appId);
            } catch (Exception e) {
                log.warn("Failed to delete application during tracked cleanup: " + appId, e);
            }
        }

        // Delete API products before APIs (products depend on APIs)
        for (int i = apiProductIds.size() - 1; i >= 0; i--) {
            String productId = apiProductIds.get(i);
            try {
                publisher.deleteApiProduct(productId);
            } catch (Exception e) {
                log.warn("Failed to delete API product during tracked cleanup: " + productId, e);
            }
        }

        // Delete APIs last
        for (int i = apiIds.size() - 1; i >= 0; i--) {
            String apiId = apiIds.get(i);
            try {
                publisher.deleteAPI(apiId);
            } catch (Exception e) {
                log.warn("Failed to delete API during tracked cleanup: " + apiId, e);
            }
        }

        reset();
    }

    /**
     * Clears all tracking state without performing any deletions.
     * Useful when resources were already deleted manually in a test.
     */
    public void reset() {
        apiIds.clear();
        applicationIds.clear();
        apiProductIds.clear();
    }
}
