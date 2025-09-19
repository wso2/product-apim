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
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.ModulePathResolver;
import org.wso2.am.testcontainers.CustomAPIMContainer;
import org.wso2.am.testcontainers.DefaultAPIMContainer;
import org.wso2.am.testcontainers.NodeAppServer;
import org.wso2.am.testcontainers.TomcatServer;
import io.cucumber.datatable.DataTable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import java.io.IOException;

public class ContainerInitializationStepDefinitions {

    private static final Logger logger = LoggerFactory.getLogger(ContainerInitializationStepDefinitions.class);
    String baseUrl;
    String serviceBaseUrl;
    String baseGatewayUrl;
    CustomAPIMContainer customApimContainer;
    String callerModuleDir = ModulePathResolver.getModuleDir(ContainerInitializationStepDefinitions.class);

    private final TestContext context;

    public ContainerInitializationStepDefinitions(TestContext context) {
        this.context = context;
    }

    @Given("I have initialized the Default API Manager container")
    public void initializeDefaultAPIMContainer() {

        DefaultAPIMContainer apimContainer = DefaultAPIMContainer.getInstance();
        baseUrl = apimContainer.getAPIManagerUrl();
        context.set("baseUrl", baseUrl);
        baseGatewayUrl = apimContainer.getGatewayUrl();
        context.set("baseGatewayUrl", baseGatewayUrl);
    }

    @Given("I have initialized the Custom API Manager container with label {string} and deployment toml file path at {string}")
    public void initializeCustomAPIMContainer(String label, String tomlDirPath) throws IOException, InterruptedException {

        final String activeProfile = System.getProperty("active.profile", "default").toLowerCase();
        Path base = Paths.get(callerModuleDir, tomlDirPath);

        // Get deployment.toml path based on the active profile
        Path fullPath = Constants.MIGRATION_PROFILE.equals(activeProfile)
                ? base.resolve("migration").resolve("deployment.toml")
                : base.resolve("deployment.toml");

        logger.info("Full path to deployment.toml of APIM with label {} : {}", label, fullPath);
        customApimContainer = new CustomAPIMContainer(label, fullPath.toString());
        customApimContainer.start();

        // Verifying that the file was copied correctly
        String filePathInsideContainer = customApimContainer.getContainerTomlPath();
        String fileContents = customApimContainer.execInContainer("cat", filePathInsideContainer).getStdout();
        logger.info("Contents of the copied deployment.toml inside the container:");
        logger.info(fileContents);

        baseUrl = customApimContainer.getAPIManagerUrl();
        context.set("baseUrl", baseUrl);
        baseGatewayUrl= customApimContainer.getGatewayUrl();
        context.set("baseGatewayUrl", baseGatewayUrl);
        context.set("label", label);
    }

    @Then("I stop the Custom API Manager container")
    public void endCustomAPIMContainer(){
       customApimContainer.close();
    }

    @Given("I have initialized the Tomcat server container")
    public void initializeTomcatServerContainer() {

        TomcatServer.getInstance();
        serviceBaseUrl = "http://tomcatbackend:8080/";
        context.set("serviceBaseUrl", serviceBaseUrl);
    }

    @Given("I have initialized the NodeApp server container")
    public void initializeNodeAppServerContainer() {
        NodeAppServer.getInstance();
    }

    @Given("I have initialized test instance with the following configuration")
    public void initializeAPIMContainerWithDataTable(DataTable dataTable) {

        Map<String, String> config = dataTable.asMap(String.class, String.class);
        String thisBaseUrl = config.getOrDefault("baseUrl", baseUrl);
        context.set("baseUrl", thisBaseUrl);
        String thisBaseGatewayUrl = config.getOrDefault("baseGatewayUrl", baseGatewayUrl);
        context.set("baseGatewayUrl", thisBaseGatewayUrl);
        String thisServiceBaseUrl = config.getOrDefault("serviceBaseUrl", serviceBaseUrl);
        context.set("serviceBaseUrl", thisServiceBaseUrl);
        String label = config.getOrDefault("label", "local");
        context.set("label", label);
    }

    @Given("The repository directory path is {string}")
    public void setRepositoryDirectoryPath(String repoPath) {

        context.set("repoUrl", repoPath);
    }

    @Then("I clear the context")
    public void clearContext(){
        context.clear();
    }

}
