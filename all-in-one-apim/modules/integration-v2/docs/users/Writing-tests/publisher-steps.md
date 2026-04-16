# Writing Steps Related to Publisher

This guide explains how to write Cucumber integration tests using the step definitions provided in `PublisherStepDefinitions.java`.  
These steps allow you to create, update, manage, and validate APIs in the Publisher portal.

---

Integration tests are located in `tests-integration/cucumber-tests`.

## Structure

- Step definitions: `src/test/java/org/wso2/am/integration/cucumbertests/stepdefinitions/`
- Feature files: `src/test/resources/features/`

## 1. Initialize the Publisher REST API Client

```gherkin
When I initialize the Publisher REST API client with username "<username>", password "<password>" and tenant "<tenant>"
```
- **Parameters:**
  - `<username>`: Publisher username (e.g., `admin`)
  - `<password>`: Publisher password (e.g., `admin`)
  - `<tenant>`: Tenant domain (e.g., `carbon.super`)

---

## 2. Create an API

```gherkin
When I create an API with the following details
  | name            | MyAPI           |
  | context         | /myapi          |
  | version         | 1.0.0           |
  | apiEndpointURL  | /myendpoint     |
  | tiersCollection | Unlimited       |
  | tier            | Gold            |
```
- **Parameters (table):**
  - `name` (required): API name
  - `context` (required): API context path
  - `version` (required): API version
  - `apiEndpointURL` (optional): Endpoint postfix (default: `jaxrs_basic/services/customers/customerservice/`)
  - `tiersCollection` (optional): Throttling tiers
  - `tier` (optional): Default tier

---

## 3. Update an API

```gherkin
When I update API of id "<apiId>" with the following details
  | name        | NewAPIName      |
  | version     | 2.0.0           |
  | context     | /newcontext     |
  | description | Updated API     |
  | tags        | tag1,tag2       |
  | defaultVersion | true          |
  | businessOwner | John Doe       |
  | businessOwnerEmail | john@example.com |
  | technicalOwner | Jane Doe      |
  | technicalOwnerEmail | jane@example.com |
  | securitySchemes | oauth2,api_key |
  | scopes      | scope1,scope2   |
  | operations  | [{"target":"/resource","verb":"GET"}] |
```
- **Parameters (table):**
  - `name`, `version`, `context`, `description`, `tags`, `defaultVersion`, `businessOwner`, `businessOwnerEmail`, `technicalOwner`, `technicalOwnerEmail`, `securitySchemes`, `scopes`, `operations` (all optional, as needed)

---

## 4. Add an Operation to an API

```gherkin
When I add an operation with the following details to the created API with id "<apiId>"
  | target           | /resource      |
  | verb             | GET            |
  | authType         | Application & Application User |
  | throttlingPolicy | Unlimited      |
  | scopes           | scope1,scope2  |
```
- **Parameters (table):**
  - `target` (required): Resource path
  - `verb` (required): HTTP verb (GET, POST, etc.)
  - `authType` (optional): Auth type
  - `throttlingPolicy` (optional): Throttling policy
  - `scopes` (optional): Comma-separated scopes

---

## 5. Set Custom API Key Header

```gherkin
When I update the API with id "<apiId>" to use API key header "<headerName>"
```
- **Parameters:**
  - `<apiId>`: API ID or context variable
  - `<headerName>`: Custom header name

---

## 6. Create and Add Scopes

```gherkin
When I create scope "<scopeName>" with roles "<role1>,<role2>"
When I add scopes "<scope1>,<scope2>" to the created API with id "<apiId>"
```
- **Parameters:**
  - `<scopeName>`: Scope name
  - `<roles>`: Comma-separated roles
  - `<apiId>`: API ID

---

## 7. Deploy, Publish, Block, Deprecate, or Delete an API

```gherkin
When I deploy a revision of the API with id "<apiId>"
When I publish the API with id "<apiId>"
When I block the API with id "<apiId>"
When I deprecate the API with id "<apiId>"
When I delete the API with id "<apiId>"
```
- **Parameters:** `<apiId>`: API ID

---

## 8. Create a New API Version

```gherkin
When I create a new version "<newVersion>" from API with id "<apiId>"
```
- **Parameters:** `<newVersion>`, `<apiId>`

---

## 9. Assertions and Validations

```gherkin
Then I should be able to retrieve the API with id "<apiId>"
Then The lifecycle status of API "<apiId>" should be "<status>"
```
- **Parameters:** `<apiId>`, `<status>` (e.g., `Published`)

---

## 10. Other Operations

- Add, update, or delete documents
- Import Swagger, GraphQL, or WSDL files
- Deploy/undeploy API revisions
- Upload endpoint certificates
- Validate endpoints
- Delete shared scopes

Refer to the step method names and parameters in `PublisherStepDefinitions.java` for the full list and usage.

---

### Tips

- Use context variables (e.g., `<createdApiId>`) to pass values between steps.
- Use DataTables for steps that require multiple parameters.
- All IDs can be referenced directly or via context variables.

---

**Example Feature:**

```gherkin
Feature: API Publisher actions

  Scenario: Create and publish an API
    When I initialize the Publisher REST API client with username "admin", password "admin", and tenant "carbon.super"
    And I create an API with the following details
      | name    | SampleAPI |
      | context | /sample   |
      | version | 1.0.0     |
    And I publish the API with id "<createdApiId>"
    Then I should be able to retrieve the API with id "<createdApiId>"
```