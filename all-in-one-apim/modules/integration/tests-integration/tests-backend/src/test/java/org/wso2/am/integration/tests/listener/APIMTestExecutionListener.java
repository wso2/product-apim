/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.wso2.am.integration.tests.workflow.WorkflowApprovalExecutorTest;

public class APIMTestExecutionListener implements ITestListener {
    private static final Log log = LogFactory.getLog(WorkflowApprovalExecutorTest.class);

    private long startTime;

    @Override
    public void onStart(ITestContext iTestContext) {
        log.info("Starting test group - " + iTestContext.getCurrentXmlTest().getName());
        startTime = System.currentTimeMillis() / 1000;
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        long endTime = System.currentTimeMillis() / 1000;
        log.info("Finished test group - " + iTestContext.getCurrentXmlTest().getName()
                + " (Completed in " + (endTime - startTime) + "s)");
    }

    @Override
    public void onTestStart(ITestResult iTestResult) {

    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {

    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {

    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

}
