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

package org.wso2.am.integration.cucumbertests.verification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.xml.SuiteXmlParser;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.wso2.am.integration.cucumbertests.utils.listeners.BlockUniquenessLintListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3.1 verification (Type-A, no Docker): proves {@link BlockUniquenessLintListener} fails fast at
 * suite load — before any block executes — when the composite {@code suiteName::testName} is not
 * globally unique (across suites and child suites) or when a {@code <suite>} is unnamed, while a valid
 * uniquely-named set passes through unchanged. The deterministic cases invoke {@code alter()} directly;
 * the final case runs a real nested TestNG with the lint registered and asserts the probe never ran.
 */
public class BlockUniquenessLintVerificationTest {

    private static final Log logger = LogFactory.getLog(BlockUniquenessLintVerificationTest.class);

    private static final String LINT_CLASS = BlockUniquenessLintListener.class.getName();
    private static final String PROBE_CLASS = UniquenessProbe.class.getName();

    @Test
    public void duplicateCompositeAcrossSuitesRejected() {
        List<XmlSuite> suites = new ArrayList<>(List.of(
                blockSuite("FV-3.1-DupSuite", "Block"),
                blockSuite("FV-3.1-DupSuite", "Block")));
        IllegalStateException e = Assert.expectThrows(IllegalStateException.class,
                () -> new BlockUniquenessLintListener().alter(suites));
        Assert.assertTrue(e.getMessage().contains("duplicate block id"),
                "unexpected message: " + e.getMessage());
        logger.info("Duplicate composite across two same-named suites rejected: " + e.getMessage());
    }

    @Test
    public void duplicateCompositeAcrossChildSuiteRejected() {
        XmlSuite parent = blockSuite("FV-3.1-Parent", "Block");
        XmlSuite child = blockSuite("FV-3.1-Parent", "Block");   // same composite, nested under parent
        child.setParentSuite(parent);
        parent.getChildSuites().add(child);
        List<XmlSuite> suites = new ArrayList<>(List.of(parent));
        IllegalStateException e = Assert.expectThrows(IllegalStateException.class,
                () -> new BlockUniquenessLintListener().alter(suites));
        Assert.assertTrue(e.getMessage().contains("duplicate block id"),
                "unexpected message: " + e.getMessage());
        logger.info("Duplicate composite across a child suite rejected (recursion works): "
                + e.getMessage());
    }

    @Test
    public void unnamedSuiteRejected() {
        XmlSuite unnamed = blockSuite("Default Suite", "Block");   // TestNG's default-name fallback
        List<XmlSuite> suites = new ArrayList<>(List.of(unnamed));
        IllegalStateException e = Assert.expectThrows(IllegalStateException.class,
                () -> new BlockUniquenessLintListener().alter(suites));
        Assert.assertTrue(e.getMessage().contains("no explicit name"),
                "unexpected message: " + e.getMessage());
        logger.info("Unnamed/default-named suite rejected: " + e.getMessage());
    }

    @Test
    public void uniqueCompositesAcrossSuitesAndChildrenPass() {
        XmlSuite parent = blockSuite("FV-3.1-SuiteA", "Block");   // SuiteA::Block
        XmlSuite child = blockSuite("FV-3.1-SuiteB", "Block");    // SuiteB::Block — same testName, distinct
        child.setParentSuite(parent);
        parent.getChildSuites().add(child);
        XmlSuite sibling = blockSuite("FV-3.1-SuiteC", "Block");  // SuiteC::Block
        List<XmlSuite> suites = new ArrayList<>(List.of(parent, sibling));
        new BlockUniquenessLintListener().alter(suites);          // must not throw
        logger.info("Distinct composites across suites + a child suite passed the lint unchanged");
    }

    @Test
    public void duplicateCompositeInParsedXmlRejectedAtLoad() throws IOException {
        // Genuine production shape: a real on-disk suite XML (with the lint wired via <listeners>) is
        // parsed by TestNG's own SuiteXmlParser, then the lint's alter() — the exact method TestNG calls
        // at suite-load before instantiating any block — is invoked on the parsed suite. Two same-named
        // <test> blocks collide on the composite, so the lint must reject at load. (A nested programmatic
        // TestNG run is not used: TestNG does not dispatch alter-suite listeners for file-based inner
        // runs, so it would not exercise the lint at all.)
        Path file = Files.createTempFile("fv-3.1-dup", ".xml");
        Files.writeString(file,
                "<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">\n"
                        + "<suite name=\"FV-3.1-ParsedDup\">\n"
                        + "  <listeners><listener class-name=\"" + LINT_CLASS + "\"/></listeners>\n"
                        + "  <test name=\"Block\"><classes>"
                        + "<class name=\"" + PROBE_CLASS + "\"/></classes></test>\n"
                        + "  <test name=\"Block\"><classes>"
                        + "<class name=\"" + PROBE_CLASS + "\"/></classes></test>\n"
                        + "</suite>\n");

        XmlSuite parsed;
        try (InputStream in = Files.newInputStream(file)) {
            parsed = new SuiteXmlParser().parse(file.toString(), in, false);
        }
        Assert.assertTrue(parsed.getTests().size() >= 2,
                "fixture should parse into >=2 <test> blocks to create a duplicate composite (got "
                        + parsed.getTests().size() + ")");

        IllegalStateException e = Assert.expectThrows(IllegalStateException.class,
                () -> new BlockUniquenessLintListener().alter(new ArrayList<>(List.of(parsed))));
        Assert.assertTrue(e.getMessage().contains("duplicate block id"),
                "unexpected message: " + e.getMessage());
        logger.info("Parsed-from-XML suite with a duplicate composite rejected by alter() at load "
                + "(before any block instantiation): " + e.getMessage());
    }

    private XmlSuite blockSuite(String suiteName, String testName) {
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        XmlTest test = new XmlTest(suite);
        test.setName(testName);
        test.setXmlClasses(List.of(new XmlClass(UniquenessProbe.class)));
        return suite;
    }
}
