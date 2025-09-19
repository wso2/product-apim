# Writing Steps Related to API Invocation

This guide explains how to use the step definitions in `APIInvocationStepDefinitions.java` for invoking APIs and validating responses in your Cucumber integration tests.

---

## 1. Invoke API (POST) to Add Customer

```gherkin
When I invoke API of ID "<apiId>" with path "<path>" and method POST using access token "<accessToken>" to add customer with name "<customerName>"
```
- **Parameters:**
  - `<apiId>`: API ID or context variable (e.g., `<createdApiId>`)
  - `<path>`: Resource path (e.g., `/customers`)
  - `<accessToken>`: Access token or context variable (e.g., `<accessToken>`)
  - `<customerName>`: Name to add in the XML payload

- **Description:**  
  Sends a POST request to the specified API and path, with an XML body containing the customer name. The access token is sent in the `Authorization` header as a Bearer token.

---

## 2. Invoke API (GET) with Access Token

```gherkin
When I invoke API of ID "<apiId>" with path "<path>" and method GET using access token "<accessToken>"
```
- **Parameters:**
  - `<apiId>`: API ID or context variable
  - `<path>`: Resource path
  - `<accessToken>`: Access token or context variable

- **Description:**  
  Sends a GET request to the specified API and path, using the access token in the `Authorization` header as a Bearer token.

---

## 3. Invoke API (GET) with Custom Auth Header

```gherkin
When I invoke API of ID "<apiId>" with path "<path>" and method GET using access token "<accessToken>" and header "<headerName>"
```
- **Parameters:**
  - `<apiId>`: API ID or context variable
  - `<path>`: Resource path
  - `<accessToken>`: Access token or context variable
  - `<headerName>`: Custom header name (e.g., `X-API-Key`, `Authorization`)

- **Description:**  
  Sends a GET request with the access token in the specified custom header.

---

## 4. Invoke API (GET) with API Key and Custom Header

```gherkin
When I invoke API of ID "<apiId>" with path "<path>" and method GET using API key "<apiKey>" and header "<headerName>"
```
- **Parameters:**
  - `<apiId>`: API ID or context variable
  - `<path>`: Resource path
  - `<apiKey>`: API key or context variable
  - `<headerName>`: Custom header name (e.g., `X-API-Key`)

- **Description:**  
  Sends a GET request with the API key in the specified header.

---

## 5. Validate API Response Status

```gherkin
Then the API response status should be <statusCode>
```
- **Parameters:**
  - `<statusCode>`: Expected HTTP status code (integer, e.g., `200`, `201`, `401`)

- **Description:**  
  Asserts that the last API invocation response has the expected HTTP status code.

---

## Context Variable Usage

- You can use context variables (e.g., `<createdApiId>`, `<accessToken>`) in place of literal values.  
  The framework will resolve these from the test context.

---

## Example Feature Usage

```gherkin
Feature: API Invocation

  Scenario: Add and retrieve a customer
    When I invoke API of ID "<createdApiId>" with path "/customers" and method POST using access token "<accessToken>" to add customer with name "John Doe"
    Then the API response status should be 201

    When I invoke API of ID "<createdApiId>" with path "/customers" and method GET using access token "<accessToken>"
    Then the API response status should be 200
```

---

**Tips:**
- Always initialize the required context variables (like `<createdApiId>`, `<accessToken>`) in previous steps.
- Use the correct HTTP method and header as required by your API security configuration.
- The response of each invocation is stored in the context as `invokeAPIResponse` for further assertions.

Refer to the method names and parameters in `APIInvocationStepDefinitions.java` for the full list and usage.