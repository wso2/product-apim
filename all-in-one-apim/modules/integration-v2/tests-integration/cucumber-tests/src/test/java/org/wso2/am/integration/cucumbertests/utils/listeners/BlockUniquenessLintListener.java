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

import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Suite-load lint for the parallel-on-shared-container lane: fails fast (before any container boot)
 * if the composite block id {@code suiteName::testName} is not globally unique across all suites and
 * their child suites, or if a {@code <suite>} carries no explicit name.
 *
 * <p>The composite is the same key {@code TestContext.sharedScopeId} derives at runtime, so a
 * duplicate here would silently merge two blocks' shared scope. An unnamed suite is rejected because
 * TestNG's "Default Suite" fallback would collapse the namespace and defeat the per-suite isolation.
 *
 * <p>Registered <b>only</b> in the framework-verification / new-lane suites. The legacy
 * {@link ParallelToggleAlterSuiteListener} is intentionally left untouched and never loads this lint.
 */
public class BlockUniquenessLintListener implements IAlterSuiteListener {

    private static final String DEFAULT_SUITE_NAME = "Default Suite";

    @Override
    public void alter(List<XmlSuite> suites) {
        Set<String> seen = new HashSet<>();
        for (XmlSuite suite : suites) {
            validate(suite, seen);
        }
    }

    private void validate(XmlSuite suite, Set<String> seen) {
        String suiteName = suite.getName();
        if (suiteName == null || suiteName.isBlank()
                || DEFAULT_SUITE_NAME.equalsIgnoreCase(suiteName.trim())) {
            throw new IllegalStateException("BlockUniquenessLint: a <suite> has no explicit name "
                    + "(got '" + suiteName + "'). The parallel lane namespaces shared scope by "
                    + "suiteName::testName, so every <suite> must declare a unique name.");
        }
        for (XmlTest test : suite.getTests()) {
            String composite = suiteName + "::" + test.getName();
            if (!seen.add(composite)) {
                throw new IllegalStateException("BlockUniquenessLint: duplicate block id '" + composite
                        + "'. Each suiteName::testName must be globally unique across suites and child "
                        + "suites — duplicates would merge two blocks' shared scope.");
            }
        }
        for (XmlSuite child : suite.getChildSuites()) {
            validate(child, seen);
        }
    }
}
