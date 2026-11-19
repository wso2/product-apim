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

package org.wso2.am.testcontainers;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;

public class DynamicPlatformGatewayContainerTest {

    @DataProvider
    public Object[][] unsetValues() {
        return new Object[][]{{null}, {""}, {"   "}};
    }

    @Test(dataProvider = "unsetValues")
    public void shouldDefaultToHomeWhenWorkDirIsUnset(String configured) {
        Assert.assertEquals(DynamicPlatformGatewayContainer.workDirBase(configured),
                Path.of(System.getProperty("user.home"), ".wso2-pgw-testcontainers"));
    }

    @Test
    public void shouldUseConfiguredWorkDir() {
        Assert.assertEquals(DynamicPlatformGatewayContainer.workDirBase(" /codebuild/output/pgw "),
                Path.of("/codebuild/output/pgw"));
    }
}
