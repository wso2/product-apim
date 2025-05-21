# Writing Steps Related to Container Initialisation

This guide explains how to use the step definitions in `ContainorIntitialisationStepDefinitions.java` for initializing and managing containers in your Cucumber integration tests.

Integration tests are located in `tests-integration/cucumber-tests`.

## Structure

- Step definitions: `src/test/java/org/wso2/am/integration/cucumbertests/stepdefinitions/`
- Feature files: `src/test/resources/features/`

---

## 1. Initialize the Default API Manager Container

```gherkin
Given I have initialized the Default API Manager container
```
- **Description:**  
  Starts the default API Manager container and sets the following context variables:
  - `baseUrl`: The API Manager URL
  - `baseGatewayUrl`: The gateway URL (HTTPS)
  - `label`: `"default"`

---

## 2. Initialize a Custom API Manager Container

```gherkin
Given I have initialized the Custom API Manager container with label "<label>" and deployment toml file path at "<tomlPath>"
```
- **Parameters:**
  - `<label>`: A label for the container instance (e.g., `custom1`)
  - `<tomlPath>`: Path to the `deployment.toml` file (relative to the module directory)
- **Description:**  
  Starts a custom API Manager container with the specified label and configuration file. Sets:
  - `baseUrl`: The API Manager URL
  - `baseGatewayUrl`: The gateway URL (HTTPS)
  - `label`: The provided label

---

## 3. Stop the Custom API Manager Container

```gherkin
Then I stop the Custom API Manager container
```
- **Description:**  
  Stops and closes the custom API Manager container started in the scenario.

---

## 4. Initialize the Tomcat Server Container

```gherkin
Given I have initialized the Tomcat server container
```
- **Description:**  
  Starts a Tomcat backend server container and sets:
  - `serviceBaseUrl`: `http://tomcatbackend:8080/`

---

## 5. Initialize the NodeApp Server Container

```gherkin
Given I have initialized the NodeApp server container
```
- **Description:**  
  Starts a Node.js backend server container and sets:
  - `serviceBaseUrl`: `http://nodebackend:8080/`

---

## 6. Clear the Test Context

```gherkin
Then I clear the context
```
- **Description:**  
  Clears all variables from the test context.

---

### Example Usage in a Feature File

```gherkin
Feature: Container Initialization

  Scenario: Start default APIM and Tomcat containers
    Given I have initialized the Default API Manager container
    And I have initialized the Tomcat server container

    Given I have initialized the Custom API Manager container with label "custom1" and deployment toml file path at "/configs/deployment.toml"
    Then I stop the Custom API Manager container
    Then I clear the context
```

**Tip:**  
Use these steps at the beginning of your scenarios to ensure the required containers and context variables are set up for your API integration tests.

## Writing Steps for Custom Test Instance Initialization

This section explains how to use the step definition for initializing a test instance with a custom configuration using a Cucumber DataTable. This is useful for running tests against any deployment—local, Docker/Testcontainers, or remote (e.g., Helm charts on Kubernetes).

---

### Step Definition

```gherkin
Given I have initialized test instance with the following configuration
  | baseUrl         | http://localhost:9443/   |
  | baseGatewayUrl  | https://localhost:8243   |
  | serviceBaseUrl  | http://nodebackend:8080/ |
  | label           | local                    |
```

- **Parameters (table):**
  - `baseUrl`: The base URL of the API Manager Publisher/Store (e.g., `http://localhost:9443/`)
  - `baseGatewayUrl`: The base URL of the API Gateway (e.g., `https://localhost:8243`)
  - `serviceBaseUrl`: The base URL of the backend service (e.g., `http://nodebackend:8080/`)
  - `label`: A label for this test instance (e.g., `local`, `dev`, `staging`)

**All fields are optional and have sensible defaults.**

---

### Example Usage in a Feature File

```gherkin
Feature: Custom Test Instance Initialization

  Scenario: Initialize test instance with custom configuration
    Given I have initialized test instance with the following configuration
      | baseUrl         | http://localhost:9443/   |
      | baseGatewayUrl  | https://localhost:8243   |
      | serviceBaseUrl  | http://nodebackend:8080/ |
      | label           | local                    |
```

---

### Running Tests Against Any Deployment

You can use this step to point your tests to any running WSO2 API Manager instance, regardless of how it was deployed:

#### 1. **Extracted ZIP Running Locally**
- Start the APIM server from the extracted zip.
- Use the default URLs in the DataTable (`http://localhost:9443/`, etc.).
- Backend services (e.g., NodeApp) should also be running and accessible.

#### 2. **Helm Charts on Kubernetes**
- Deploy APIM using the official Helm charts.
- Find the service URLs (e.g., via `kubectl get svc`).
- Use those URLs in the DataTable:
  ```gherkin
  | baseUrl         | https://<k8s-apim-service>:9443/   |
  | baseGatewayUrl  | https://<k8s-gateway-service>:8243 |
  | serviceBaseUrl  | http://<k8s-backend-service>:8080/ |
  | label           | k8s                                |
  ```

#### 3. **Testcontainers**
- If using the framework's Testcontainers steps, you usually don't need this step, as containers are started and configured automatically.
- However, you can still override URLs if needed for advanced scenarios.

#### 4. **Remote or Cloud Deployments**
- Use the public or internal URLs of your APIM and backend services in the DataTable.

---

### Tips

- This step is ideal for **integration testing against any environment**—local, CI, staging, or production-like.
- Make sure the backend service (`serviceBaseUrl`) is reachable from where the tests are running.
- You can use different labels to distinguish between environments in your test context.

---

**Summary:**  
Use the `Given I have initialized test instance with the following configuration` step to flexibly target any APIM deployment for your integration tests by specifying the correct URLs and label in the DataTable.


**Tip:**  
Use these steps at the beginning of your scenarios to ensure the required containers and context variables are set up for your API integration tests.