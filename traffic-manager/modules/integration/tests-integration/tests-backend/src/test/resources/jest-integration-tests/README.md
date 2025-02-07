# Integration test Implementation

# Prerequisites

Require NodeJs 8.0 or higher, and npm 5.0 or higher.You may use [`nvm`](https://github.com/nvm-sh/nvm) for installing/managing Node and NPM tools.

# How to run

* Run in Headless mode

```
npm test
```

* Run in GUI mode

```
npm run gtest

```

Before running either unit tests or integration test, Go to the application root directory where `package.json` file is located (i:e `<carbon-apimgt-root>/features/apimgt/org.wso2.carbon.apimgt.publisher.feature/src/main/resources/publisher/`) and run `npm install` command to download all the dependencies.

These integration test will be automatically executed , when building the product API Manager with tests i:e `mvn clean install`.

### How it works

We have a TestNG test case named
[APIMANAGERUIIntegrationTestRunner](org/wso2/am/integration/tests/UI/APIMANAGERUIIntegrationTestRunner.java) to execute
the `npm` commands. Java test is assert on `npm` exit code.


## Integration tests

Before running the integration tests, Make sure you have started relevant WSO2 API-Manager server in it's default ports(management port`9443`) locally or you have set the `WSO2_PORT_OFFSET` with correct port offset value. Then run the command `npm run test:integration` in the application root directory.

## Where is unit tests ??

Unit tests are located in the [`carbon-apimgt`](https://github.com/wso2/carbon-apimgt/blob/master/features/apimgt/org.wso2.carbon.apimgt.publisher.feature/src/main/resources/publisher/source/Tests/README.md) along with the React component source files.

# Troubleshooting

> Feel free to update this guide , If you able to find better alternatives or or if you find anything that is worth adding here

## No node found for selector: input[name="username"]


## net::ERR_CONNECTION_REFUSED at `URL`
