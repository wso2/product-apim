package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.wso2.am.integration.cucumbertests.TestContext;
import org.wso2.am.integration.test.utils.ModulePathResolver;
import org.wso2.am.testcontainers.*;
import io.cucumber.datatable.DataTable;
import java.util.Map;

import java.io.IOException;
import java.util.Map;

public class ContainorIntitialisationStepDefinitions {
    String baseUrl;
    String serviceBaseUrl;
    String baseGatewayUrl;
    Integer HTTPS_PORT=8243;
    Integer HTTP_PORT=8280;
    CustomAPIMContainer customApimContainer;
    String callerModuleDir = ModulePathResolver.getModuleDir(ContainorIntitialisationStepDefinitions .class);


    private final TestContext context;

    public ContainorIntitialisationStepDefinitions(TestContext context) {
        this.context = context;
    }

    @Given("I have initialized the Default API Manager container")
    public void initializeDefaultAPIMContainer() {
        DefaultAPIMContainer apimContainer = DefaultAPIMContainer.getInstance();
        baseUrl = apimContainer.getAPIManagerUrl();
        context.set("baseUrl",baseUrl);
        Integer gatewayPort= apimContainer.getMappedPort(HTTPS_PORT);
        String gatewayHost = apimContainer.getHost();
        baseGatewayUrl= String.format("https://%s:%d", gatewayHost, gatewayPort);
        context.set("baseGatewayUrl",baseGatewayUrl);
        context.set("label","default");
    }

    @Given("I have initialized the Custom API Manager container with label {string} and deployment toml file path at {string}")
    public void initializeCustomAPIMContainer(String label,String tomlPath) throws IOException, InterruptedException {
        String fullPath = callerModuleDir+tomlPath;
        customApimContainer = new CustomAPIMContainer(label,fullPath);
        customApimContainer.start();

        // Verifying that the file was copied correctly
        String filePathInsideContainer = "/opt/repository/conf/deployment.toml";
        String fileContents = customApimContainer.execInContainer("cat", filePathInsideContainer).getStdout();
        System.out.println("Contents of the copied deployment.toml inside the container:");
        System.out.println(fileContents);

        baseUrl = customApimContainer.getAPIManagerUrl();
        context.set("baseUrl",baseUrl);
        Integer gatewayPort= customApimContainer.getMappedPort(HTTPS_PORT);
        String gatewayHost = customApimContainer.getHost();
        baseGatewayUrl= String.format("https://%s:%d", gatewayHost, gatewayPort);
        context.set("baseGatewayUrl",baseGatewayUrl);
        context.set("label",label);
    }

    @Then("I stop the Custom API Manager container")
    public void endCustomAPIMContainer(){
//       customApimContainer.stop();
       customApimContainer.close();
    }

    @Given("I have initialized the Tomcat server container")
    public void initializeTomcatServerContainer() {

        TomcatServer.getInstance();
        serviceBaseUrl = "http://tomcatbackend:8080/";
        context.set("serviceBaseUrl",serviceBaseUrl);
    }

    @Given("I have initialized the NodeApp server container")
    public void initializeNodeAppServerContainer() {
        NodeAppServer.getInstance();
        serviceBaseUrl = "http://nodebackend:8080/";
        context.set("serviceBaseUrl",serviceBaseUrl);

    }

    @Given("I have initialized test instance with the following configuration")
    public void initializeAPIMContainerWithDataTable(DataTable dataTable) {
        Map<String, String> config = dataTable.asMap(String.class, String.class);

        String baseUrl = config.getOrDefault("baseUrl", "http://localhost:9443/");
        context.set("baseUrl", baseUrl);
        String baseGatewayUrl = config.getOrDefault("baseGatewayUrl", "https://localhost:8243");
        context.set("baseGatewayUrl", baseGatewayUrl);
        String serviceBaseUrl = config.getOrDefault("serviceBaseUrl", "http://nodebackend:8080/");
        context.set("serviceBaseUrl", serviceBaseUrl);
        String label = config.getOrDefault("label", "local");
        context.set("label", label);
    }

    @Then("I clear the context")
    public void clearContext(){
        context.clear();
    }

}


