/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.logging;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.utils.ServerConstants;

public class OOMLogsCheckTest extends APIManagerLifecycleBaseTest {
    @Test(groups = { "wso2.am" }, description = "Test whether a heapdump is generated due to OOM issue")
    public void testAvailabilityOfHeapDumpTestcase() throws Exception {
        String heapdumpFile = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository"
                + File.separator + "logs" + File.separator + "heap-dump.hprof";

        File file = new File(heapdumpFile);
        assertFalse("A Heapdump file is present in " + heapdumpFile + ". This possibly indicates an issue during startup / OSGi resolution.", file.exists());

    }

}
