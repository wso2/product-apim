/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;

/**
 * Cucumber lifecycle hooks shared across all step definitions.
 */
public class Hooks {

    /** Records what is registered when a {@code @cleanup} scenario begins (see {@link #cleanUpCreatedResources}). */
    @Before("@cleanup")
    public void beginScenarioCleanupScope() {
        ResourceCleanup.beginScenarioScope();
    }

    /**
     * Per-scenario teardown for scenarios tagged {@code @cleanup}. Deletes every resource the scenario itself
     * registered so the next scenario on the shared container starts clean and does not collide on duplicate
     * resource names (HTTP 409). Resources registered before the scenario began — a {@code _setup_*} feature's
     * fixtures, which later scenarios in the runner still use — stay registered for the per-runner
     * {@code @AfterClass} sweep on {@code BaseBlockRunner} (both delegate to {@link ResourceCleanup}). Tag-scoped,
     * so suites that intentionally persist resources across scenarios are unaffected.
     */
    @After("@cleanup")
    public void cleanUpCreatedResources() {
        ResourceCleanup.deleteScenarioRegisteredResources();
    }

    /** Restores scenario-mutated shared fixtures without deleting the runner-scoped fixture resources. */
    @After("@restore-key-mapping")
    public void restoreKeyMappings() {
        ResourceCleanup.restoreKeyMappings();
    }
}
