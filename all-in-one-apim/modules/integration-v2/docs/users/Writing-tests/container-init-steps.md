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