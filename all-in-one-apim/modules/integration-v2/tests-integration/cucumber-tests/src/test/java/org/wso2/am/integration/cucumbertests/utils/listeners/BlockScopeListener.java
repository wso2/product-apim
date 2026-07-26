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

package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.wso2.am.integration.cucumbertests.utils.TestContext;

/**
 * Sets the {@link TestContext} scope for each invocation, keying the shared scope by
 * {@code suiteName::testName} (via {@link TestContext#sharedScopeId}) so two {@code <test>} blocks of
 * the same name in different suites cannot merge shared state. This is the parallel-lane replacement
 * for {@code TestNameMdcListener}'s bare-{@code testName} keying; it is a brand-new listener so the
 * legacy listener stays untouched, and it must be registered <b>only</b> in the verification/parallel
 * suites — never in the legacy {@code testng.xml}.
 */
public class BlockScopeListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        String sharedScopeId = TestContext.sharedScopeId(testResult.getTestContext());
        TestContext.setScope(sharedScopeId, buildLocalScopeId(sharedScopeId, testResult));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        TestContext.clearScope();
    }

    private String buildLocalScopeId(String sharedScopeId, ITestResult testResult) {
        Object instance = testResult.getInstance();
        String className = testResult.getTestClass() != null
                ? testResult.getTestClass().getName()
                : "unknown-class";
        int instanceId = instance != null ? System.identityHashCode(instance) : 0;
        return sharedScopeId + "::" + className + "#" + instanceId;
    }
}
