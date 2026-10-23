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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuiteGroupSelectionListenerTest {

    private static XmlSuite suite(String... nameGroupPairs) {
        XmlSuite suite = new XmlSuite();
        suite.setName("Integration Tests v2");
        for (int i = 0; i < nameGroupPairs.length; i += 2) {
            XmlTest test = new XmlTest(suite);
            test.setName(nameGroupPairs[i]);
            if (nameGroupPairs[i + 1] != null) {
                test.setParameters(Map.of(SuiteGroupSelectionListener.GROUP_PARAMETER, nameGroupPairs[i + 1]));
            }
        }
        return suite;
    }

    private static List<String> names(XmlSuite suite) {
        return suite.getTests().stream().map(XmlTest::getName).collect(Collectors.toList());
    }

    @Test
    public void absentPropertyRunsCompleteSuite() {
        XmlSuite suite = suite("A", "group1", "B", "group2", "C", "group1");
        SuiteGroupSelectionListener.select(List.of(suite), null);
        Assert.assertEquals(names(suite), List.of("A", "B", "C"));
    }

    @Test
    public void blankPropertyRunsCompleteSuite() {
        XmlSuite suite = suite("A", "group1", "B", "group2");
        SuiteGroupSelectionListener.select(List.of(suite), "  ");
        Assert.assertEquals(names(suite), List.of("A", "B"));
    }

    @Test
    public void selectsRequestedGroupInSuiteOrder() {
        XmlSuite suite = suite("A", "group1", "B", "group2", "C", "group1", "D", "group2");
        SuiteGroupSelectionListener.select(List.of(suite), "group2");
        Assert.assertEquals(names(suite), List.of("B", "D"));
    }

    @Test
    public void selectsSeveralGroupsInSuiteOrder() {
        XmlSuite suite = suite("A", "group1", "B", "group2", "C", "group3");
        SuiteGroupSelectionListener.select(List.of(suite), " group3 , group1 ");
        Assert.assertEquals(names(suite), List.of("A", "C"));
    }

    @Test
    public void unknownGroupFails() {
        XmlSuite suite = suite("A", "group1", "B", "group2");
        IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                () -> SuiteGroupSelectionListener.select(List.of(suite), "group1,grup2"));
        Assert.assertTrue(failure.getMessage().contains("[grup2]"), failure.getMessage());
        Assert.assertEquals(names(suite), List.of("A", "B"));
    }

    @Test
    public void ungroupedBlockFailsEvenForCompleteSuite() {
        XmlSuite suite = suite("A", "group1", "B", null);
        IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                () -> SuiteGroupSelectionListener.select(List.of(suite), null));
        Assert.assertTrue(failure.getMessage().contains("[B]"), failure.getMessage());
    }
}
