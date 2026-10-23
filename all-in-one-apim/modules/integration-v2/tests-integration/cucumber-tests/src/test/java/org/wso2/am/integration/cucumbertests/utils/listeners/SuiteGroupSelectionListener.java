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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Selects the {@code <test>} blocks of a product suite by their {@code group} parameter, so CI can run one suite as
 * several parallel jobs. {@code -Dapim.v2.test.groups=group1[,group2...]} keeps only the blocks of the listed groups,
 * in their suite order; without the property the complete suite runs.
 *
 * <p>Every block must declare exactly one {@code group}, checked on every run: a block without a group would
 * otherwise be silently skipped by every partial CI job. A requested group that no block declares fails the run
 * instead of selecting nothing.
 */
public class SuiteGroupSelectionListener implements IAlterSuiteListener {

    static final String GROUPS_PROPERTY = "apim.v2.test.groups";
    static final String GROUP_PARAMETER = "group";
    private static final Log logger = LogFactory.getLog(SuiteGroupSelectionListener.class);

    @Override
    public void alter(List<XmlSuite> suites) {
        select(suites, System.getProperty(GROUPS_PROPERTY));
    }

    static void select(List<XmlSuite> suites, String requestedGroups) {
        Set<String> requested = parse(requestedGroups);
        for (XmlSuite suite : suites) {
            Set<String> declared = new LinkedHashSet<>();
            List<String> ungrouped = new ArrayList<>();
            for (XmlTest test : suite.getTests()) {
                String group = test.getLocalParameters().get(GROUP_PARAMETER);
                if (group == null || group.isBlank()) {
                    ungrouped.add(test.getName());
                } else {
                    declared.add(group.trim());
                }
            }
            if (!ungrouped.isEmpty()) {
                throw new IllegalStateException("Suite '" + suite.getName() + "' has blocks without a '"
                        + GROUP_PARAMETER + "' parameter: " + ungrouped);
            }
            if (requested.isEmpty()) {
                continue;
            }
            Set<String> unknown = new LinkedHashSet<>(requested);
            unknown.removeAll(declared);
            if (!unknown.isEmpty()) {
                throw new IllegalStateException("-D" + GROUPS_PROPERTY + " names groups that suite '"
                        + suite.getName() + "' does not declare: " + unknown + "; declared groups: " + declared);
            }
            List<XmlTest> selected = suite.getTests().stream()
                    .filter(test -> requested.contains(test.getLocalParameters().get(GROUP_PARAMETER).trim()))
                    .collect(Collectors.toList());
            suite.setTests(selected);
            logger.info("Suite '" + suite.getName() + "' runs groups " + requested + ": " + selected.size()
                    + " blocks " + selected.stream().map(XmlTest::getName).collect(Collectors.toList()));
        }
    }

    private static Set<String> parse(String requestedGroups) {
        if (requestedGroups == null || requestedGroups.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(requestedGroups.split(","))
                .map(String::trim)
                .filter(group -> !group.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
