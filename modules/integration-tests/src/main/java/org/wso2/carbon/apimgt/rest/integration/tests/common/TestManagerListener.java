/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.rest.integration.tests.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestManagerListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestManagerListener.class);

    @Override
    public void onTestStart(ITestResult iTestResult) {

        log.info("Running the test method --- " + iTestResult.getTestClass().getName() + "." + iTestResult.getMethod
                ().getMethodName() + " ----");

    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {

        log.info("On test success...");

    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {

        log.info("On test failure...");
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {

        log.info("On test skipped...");

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    @Override
    public void onStart(ITestContext iTestContext) {

    }

    @Override
    public void onFinish(ITestContext iTestContext) {

    }
}
