/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.wso2.am.integration.cucumbertests.utils.listeners.ClockStabilityListener;

import java.util.List;

/** Focused verification only: the inner suite deliberately loses its own clock observer and must fail. */
public class ClockStabilityVerificationTest {
    @Test
    public void lostObserverMakesTestNgFail() {
        XmlSuite suite = new XmlSuite();
        suite.setName("Clock observer failure verification");
        suite.addListener(ClockStabilityListener.class.getName());
        XmlTest test = new XmlTest(suite);
        test.setName("Observer loss is an infrastructure failure");
        test.setXmlClasses(List.of(new XmlClass(ClockObserverFailureProbe.class)));
        TestNG execution = new TestNG();
        TestListenerAdapter results = new TestListenerAdapter();
        execution.addListener(results);
        execution.setUseDefaultListeners(false);
        execution.setXmlSuites(List.of(suite));
        try {
            execution.run();
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("clock"), expected.toString());
        }
        Assert.assertEquals(results.getFailedTests().size(), 1,
                "Observer loss must be reported as a failed TestNG method");
        Assert.assertTrue(results.getFailedTests().get(0).getThrowable().getMessage().contains("clock"));
    }
    @Test
    public void observerLossDuringTeardownCannotPass() {
        XmlSuite suite = new XmlSuite();
        suite.setName("Clock observer teardown failure verification");
        suite.addListener(ClockStabilityListener.class.getName());
        XmlTest test = new XmlTest(suite);
        test.setName("Observer loss after final test is an infrastructure failure");
        test.setXmlClasses(List.of(new XmlClass(ClockObserverTeardownFailureProbe.class)));
        TestNG execution = new TestNG();
        TestListenerAdapter results = new TestListenerAdapter();
        execution.addListener(results);
        execution.setUseDefaultListeners(false);
        execution.setXmlSuites(List.of(suite));
        IllegalStateException failure = null;
        try {
            execution.run();
        } catch (IllegalStateException expected) {
            failure = expected;
        }
        Assert.assertNotNull(failure, "Observer loss after the final test must fail the execution");
        Assert.assertTrue(failure.getMessage().contains("clock"), failure.toString());
        Assert.assertEquals(results.getPassedTests().size(), 1);
        Assert.assertEquals(results.getFailedTests().size(), 0);
    }

}
