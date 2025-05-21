# Getting Started

This guide helps you set up and run your first integration test with the APIM Test Framework.

---

## Prerequisites

- Java 11+
- Maven
- Docker (for containerized tests)

---

## 1. Build the Framework

Run the following command to build the framework:

```sh
mvn clean install
```

---

## 2. Running All Tests

Navigate to the test directory and run:

```sh
mvn test
```

---

## 3. Running a Specific Test

To run a specific feature file:

1. Open the runner file:
   ```
   tests-integration/cucumber-tests/src/test/java/org/wso2/am/integration/cucumbertests/runners/groupedTestRunner.java
   ```

2. Modify the `features` option in the `@CucumberOptions` annotation to point to your desired feature file. For example:
   ```java
   @CucumberOptions(
       features = "src/test/resources/features/customHeaderTest", // modify this
       glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
       plugin = {"pretty", "html:target/cucumber-report/groupedtestrunner.html"}
   )
   ```

3. Run the specific test with:
   ```sh
   mvn test -DrunGroupTest=true
   ```

---

See [Writing Integration Tests](writing-tests.md) for more details on creating and customizing your tests.