# Writing Steps Related to Store

This guide explains how to use the step definitions in `StoreStepDefinitions.java` for writing Cucumber integration tests that interact with the API Store (developer portal).

---

Integration tests are located in `tests-integration/cucumber-tests`.

## Structure

- Step definitions: `src/test/java/org/wso2/am/integration/cucumbertests/stepdefinitions/`
- Feature files: `src/test/resources/features/`

## 1. Initialize the Store REST API Client

```gherkin
When I initialize the Store REST API client with username "<username>", password "<password>" and tenant "<tenant>"
```
- **Parameters:**
  - `<username>`: Store user (e.g., `admin`)
  - `<password>`: User password (e.g., `admin`)
  - `<tenant>`: Tenant domain (e.g., `carbon.super`)

---

## 2. Create an Application

```gherkin
When I create an application with the following details
  | name            | MyApp           |
  | throttlingPolicy| Unlimited       |
  | callbackUrl     | http://cb.com   | # optional
  | description     | My test app     | # optional
```
- **Parameters (table):**
  - `name` (required): Application name
  - `throttlingPolicy` (required): Throttling policy
  - `callbackUrl` (optional): Callback URL
  - `description` (optional): Description

---

## 3. Update an Application

```gherkin
When I update the application with id "<appId>" with the following details
  | name            | NewAppName      |
  | throttlingPolicy| Gold            |
  | description     | Updated desc    |
```
- **Parameters:**
  - `<appId>`: Application ID or context variable
  - Table fields: `name`, `throttlingPolicy`, `description` (all optional)

---

## 4. Delete an Application

```gherkin
When I delete the application with id "<appId>"
```
- **Parameters:** `<appId>`: Application ID or context variable

---

## 5. Subscribe to an API

```gherkin
When I subscribe to API "<apiId>" using application "<appId>" with throttling policy "<policy>"
```
- **Parameters:**
  - `<apiId>`: API ID or context variable
  - `<appId>`: Application ID or context variable
  - `<policy>`: Throttling policy

---

## 6. Retrieve Application or Subscription

```gherkin
Then I should be able to retrieve the application with id "<appId>"
Then I should be able to retrieve the subscription for Api "<apiId>" by Application "<appId>"
```
- **Parameters:** IDs or context variables

---

## 7. Generate Client Credentials

```gherkin
When I generate client credentials for application id "<appId>" with key type "<keyType>"
```
- **Parameters:**
  - `<appId>`: Application ID or context variable
  - `<keyType>`: Key type (`PRODUCTION` or `SANDBOX`)

---

## 8. Request Access Tokens

```gherkin
When I request an access token using grant type "<grantType>" without any scope
When I request an access token using grant type "<grantType>" with scope "<scope>"
```
- **Parameters:**
  - `<grantType>`: OAuth2 grant type (e.g., `password`, `client_credentials`)
  - `<scope>`: Scope string (for the second step)

---

## 9. Check Access Token Availability

```gherkin
Then the access token should be available
```
- **Description:**  
  Asserts that an access token was generated and stored in context.

---

## 10. Generate API Key

```gherkin
When I generate an API key for application "<appId>"
```
- **Parameters:** `<appId>`: Application ID or context variable

---

## 11. List Applications and Subscriptions

```gherkin
Then I should be able to list all applications
Then I should be able to list all subscriptions for application id "<appId>"
```
- **Parameters:** `<appId>`: Application ID or context variable

---

## 12. Get Published APIs

```gherkin
Then I should be able to get the list of published APIs
```

---

## 13. Add/Remove Ratings

```gherkin
When I add a rating of <rating> to API "<apiId>" for tenant "<tenant>"
When I remove the rating for API "<apiId>" in tenant "<tenant>"
```
- **Parameters:**
  - `<rating>`: Integer rating value
  - `<apiId>`: API ID or context variable
  - `<tenant>`: Tenant domain

---

## 14. Add/Edit/Remove Comments

```gherkin
When I add a comment "<comment>" to API "<apiId>" in category "<category>"
When I remove the comment with id "<commentId>" from API "<apiId>"
When I edit comment with id "<commentId>" on API "<apiId>" to "<newContent>"
Then I should be able to get comments for API "<apiId>" in tenant "<tenant>"
```
- **Parameters:**  
  - `<comment>`, `<apiId>`, `<category>`, `<commentId>`, `<newContent>`, `<tenant>`

---

## 15. Get Tags, Search APIs, Get API Details

```gherkin
Then I should be able to get all tags
Then I should be able to search APIs with query "<query>"
Then I should be able to get the API with id "<apiId>"
```
- **Parameters:** `<query>`, `<apiId>`

---

## 16. Clean Up Application

```gherkin
Then I clean up Application of Id "<appId>"
```
- **Parameters:** `<appId>`: Application ID or context variable

---

### Example Feature Usage

```gherkin
Feature: Store Application Management

  Scenario: Create and subscribe an application
    When I initialize the Store REST API client with username "user1", password "pass", and tenant "carbon.super"
    And I create an application with the following details
      | name            | MyApp           |
      | throttlingPolicy| Unlimited       |
    And I subscribe to API "<createdApiId>" using application "<createdAppId>" with throttling policy "Unlimited"
    When I generate client credentials for application id "<createdAppId>" with key type "PRODUCTION"
    When I request an access token using grant type "client_credentials" without any scope
    Then the access token should be available
```

---

**Tips:**
- Use context variables (e.g., `<createdAppId>`, `<createdSubscriptionId>`) to pass values between steps.
- Use DataTables for steps that require multiple parameters.
- All IDs can be referenced directly or via context variables.

Refer to the method names and parameters in `StoreStepDefinitions.java` for the full list and usage.