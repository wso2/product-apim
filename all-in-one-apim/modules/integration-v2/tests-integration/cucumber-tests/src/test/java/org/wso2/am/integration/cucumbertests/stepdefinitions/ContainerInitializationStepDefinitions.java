/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.testcontainers.APIMContainer;
import org.wso2.am.testcontainers.NodeAppServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;

public class ContainerInitializationStepDefinitions {

    private static final Logger logger = LoggerFactory.getLogger(ContainerInitializationStepDefinitions.class);
    String callerModuleDir = ModulePathResolver.getModuleDir(ContainerInitializationStepDefinitions.class);


    @Given("I have initialized the API Manager container with label {string} and deployment toml changes file path at {string}")
    public void initializeAPIMContainer(String label, String tomlChangesDirPath) throws IOException, InterruptedException {

        final String activeProfile = System.getProperty("active.profile", Constants.DEFAULT_PROFILE).toLowerCase();
        // Get the base deployment.toml path based on the active profile
        String baseTomlPath = Constants.MIGRATION_PROFILE.equals(activeProfile)
                ? Paths.get(callerModuleDir, Constants.MIGRATION_TOML_PATH).toString()
                : Paths.get(callerModuleDir, Constants.DEFAULT_TOML_PATH).toString();

        String tomlContent;
        if (StringUtils.isBlank(tomlChangesDirPath)) {
            tomlContent = Files.readString(Path.of(baseTomlPath));
        } else {
            String tomlChangesPath = Paths.get(callerModuleDir, tomlChangesDirPath, "deployment.toml").toString();
            // Make config changes to the base deployment.toml
            tomlContent = Utils.mergeToml(baseTomlPath, tomlChangesPath);
        }

        APIMContainer apimContainer = new APIMContainer(label, tomlContent);
        apimContainer.start();
        TestContext.set("apimContainer", apimContainer);

        // Verifying that the deployment.toml file was copied correctly by logging its content
        String filePathInsideContainer = apimContainer.getContainerTomlPath();
        String fileContents = apimContainer.execInContainer("cat", filePathInsideContainer).getStdout();
        logger.info("Contents of the copied deployment.toml inside the container: \n {}", fileContents);

        TestContext.set("baseUrl", apimContainer.getAPIManagerUrl());
        TestContext.set("baseGatewayUrl", apimContainer.getGatewayUrl());
        TestContext.set("label", label);
    }

    @Then("I stop the API Manager container")
    public void stopCustomAPIMContainer(){

        APIMContainer apimContainer = (APIMContainer) TestContext.get("apimContainer");
        apimContainer.stop();
    }

    @Given("I have initialized the NodeApp server container")
    public void initializeNodeAppServerContainer() {
        NodeAppServer.getInstance();
    }

    @Then("I clear the context")
    public void clearContext(){
        TestContext.clear();
    }
}
