# Code Structure

- **tests-common/integration-test-utils/**: Helper classes for API and test logic
- **tests-common/testcontainers/**: Containers implementation for APIM
- **tests-integration/cucumber-tests/**: Test code and feature files


## APIM Testcontainers: Detailed Overview  (tests-common/testcontainers)

This document provides a detailed explanation of the container classes implemented in the framework under [`tests-common/testcontainers`](../../tests-common/testcontainers). These containers are used to spin up and manage WSO2 API Manager and backend services for integration testing, leveraging the [Testcontainers](https://www.testcontainers.org/) Java library.

---

### 1. **APIMContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/APIMContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/CustomAPIMContainer.java)

**Description:**  
This class allows you to start an APIM container with any given `deployment.toml` configuration.

**Key Features:**
- Uses Ubuntu Linux based Docker image for APIM.
Accepts a label and a path to a custom `deployment.toml` file.
- Exposes the required ports for APIM and Gateway (eg: 9443, 9763, 8243, 8280).
- Copies the API Manager distribution into the container.
- Copies the custom configuration into the container before startup.
- Sets up the container command and waits for the APIM server to be ready.
- Provides utility methods to get the API Manager URL and mapped ports.
- Useful for testing APIM with the default behaviour and also with different configurations or features enabled/disabled.

**Constructor:**
```java
public APIMContainer(String containerLabel, String deploymentTomlPath)
```

---

### 2. **NodeAppServerContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/NodeAppServerContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/NodeAppServerContainer.java)

**Description:**  
This container is used to spin up a Node.js application server as a backend for API invocation tests.

**Key Features:**
- Runs a Node.js backend app (from `nodeapps/` resources).
- Exposes the standard Node.js port (8080).
- Useful for testing APIs with different backend technologies.


---

### 6. **Other Artifacts and Resources**

- **Node Apps:**  
  The `nodeapps/` directory contains Node.js applications used as backend services.

---

## **How Containers Work Together**

- **Testcontainers** dynamically manage the lifecycle of these containers during test execution.
- The framework can start/stop containers as needed, inject custom configurations, and expose required ports to the test environment.
- Containers are isolated per test run, ensuring repeatable and reliable integration testing.

---

## **References**

- [Testcontainers Java Documentation](https://www.testcontainers.org/features/creating_container/)
- [WSO2 API Manager Documentation](https://apim.docs.wso2.com/)

---