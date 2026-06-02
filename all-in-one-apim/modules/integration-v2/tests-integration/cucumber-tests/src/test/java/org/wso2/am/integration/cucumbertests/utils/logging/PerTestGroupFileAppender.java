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

package org.wso2.am.integration.cucumbertests.utils.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.FileAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes log events to a separate file per TestNG test group, based on the
 * "testName" MDC value set by TestNameMdcListener. Events with no testName MDC
 * are skipped (the unified FILE appender still catches them).
 */
public class PerTestGroupFileAppender extends AppenderSkeleton {

    private String logDir = "logs";
    private final Map<String, FileAppender> appenders = new ConcurrentHashMap<>();

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public String getLogDir() {
        return logDir;
    }

    @Override
    protected void append(LoggingEvent event) {
        Object testNameValue = event.getMDC("testName");
        if (testNameValue == null) {
            return;
        }
        String testName = testNameValue.toString();
        FileAppender target = appenders.computeIfAbsent(testName, this::createAppender);
        if (target != null) {
            target.doAppend(event);
        }
    }

    private FileAppender createAppender(String testName) {
        try {
            FileAppender fa = new FileAppender(getLayout(),
                    logDir + "/" + testName + ".log", false);
            fa.setName("PER_TEST_GROUP_" + testName);
            return fa;
        } catch (IOException e) {
            errorHandler.error("Failed to create per-test-group file appender for " + testName, e, 0);
            return null;
        }
    }

    @Override
    public void close() {
        for (FileAppender fa : appenders.values()) {
            fa.close();
        }
        appenders.clear();
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
}
