# Code Structure

- **tests-common/clients/**: Java API clients (Publisher, Store, Admin, Gateway)
- **tests-common/integration-test-utils/**: Helper classes for API and test logic
- **tests-common/testcontainers/**: Containers implementation for APIM
- **tests-integration/cucumber-tests/**: Test code and feature files

Each client has its own `README.md` and auto-generated API docs.

## APIM Testcontainers: Detailed Overview  (tests-common/testcontainers)

This document provides a detailed explanation of the container classes implemented in the framework under [`tests-common/testcontainers`](../../tests-common/testcontainers). These containers are used to spin up and manage WSO2 API Manager and backend services for integration testing, leveraging the [Testcontainers](https://www.testcontainers.org/) Java library.

---

### 1. **BaseAPIMContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/BaseAPIMContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/BaseAPIMContainer.java)

**Description:**  
This is the abstract base class for all API Manager containers. It extends `GenericContainer` from Testcontainers and provides common configuration and utility methods for APIM containers.

**Key Features:**
- Uses a base Docker image (e.g., `openjdk:11-jre-slim`).
- Exposes the required ports for APIM and Gateway (9443, 9763, 8243, 8280).
- Copies the API Manager distribution into the container.
- Sets up the container command and waits for the APIM server to be ready.
- Provides utility methods to get the API Manager URL and mapped ports.

**Important Methods:**
- `getAPIManagerUrl()`: Returns the HTTPS URL for the API Manager.
- `getHttpsPort()`, `getHttpPort()`: Return the mapped HTTPS/HTTP ports.

---

### 2. **DefaultAPIMContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/DefaultAPIMContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/DefaultAPIMContainer.java)

**Description:**  
This class extends `BaseAPIMContainer` and represents a WSO2 API Manager instance with the default `deployment.toml` configuration. It is typically used as the baseline APIM instance for tests.

**Key Features:**
- No custom configuration is applied; uses the default APIM pack.
- Suitable for scenarios where you want to test against the standard APIM behavior.

**Usage Example:**
```java
DefaultAPIMContainer apimContainer = DefaultAPIMContainer.getInstance();
apimContainer.start();
```

---

### 3. **CustomAPIMContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/CustomAPIMContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/CustomAPIMContainer.java)

**Description:**  
This class extends `BaseAPIMContainer` and allows you to start an APIM container with a custom `deployment.toml` configuration.

**Key Features:**
- Accepts a label and a path to a custom `deployment.toml` file.
- Copies the custom configuration into the container before startup.
- Useful for testing APIM with different configurations or features enabled/disabled.

**Constructor:**
```java
public CustomAPIMContainer(String containerLabel, String deploymentTomlPath)
```

**Usage Example:**
```java
CustomAPIMContainer customContainer = new CustomAPIMContainer("custom1", "/path/to/deployment.toml");
customContainer.start();
```

---

### 4. **TomcatServerContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/TomcatServerContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/TomcatServerContainer.java)

**Description:**  
This container is used to spin up a Tomcat server as a backend for API invocation tests.

**Key Features:**
- Deploys sample WAR files (e.g., `jaxrs_basic.war`) as backend services.
- Exposes the standard Tomcat port (8080).
- Useful for simulating real backend services for API Manager.

**Usage Example:**
```java
TomcatServerContainer tomcat = TomcatServerContainer.getInstance();
tomcat.start();
```

---

### 5. **NodeAppServerContainer**

**Location:**  
[`tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/NodeAppServerContainer.java`](../../tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/NodeAppServerContainer.java)

**Description:**  
This container is used to spin up a Node.js application server as a backend for API invocation tests.

**Key Features:**
- Runs a Node.js backend app (from `nodeapps/` resources).
- Exposes the standard Node.js port (8080).
- Useful for testing APIs with different backend technologies.

**Usage Example:**
```java
NodeAppServerContainer nodeApp = NodeAppServerContainer.getInstance();
nodeApp.start();
```

---

### 6. **Other Artifacts and Resources**

- **WAR Files:**  
  The `artifacts/` directory contains various WAR files (e.g., `jaxrs_basic.war`, `am-graphQL-sample.war`) that are deployed to backend containers for testing different API scenarios.
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