@setup
Feature: Setup base APIs

  Creates the base API fixtures (REST, SOAP, GraphQL, WebSocket) used by publisher and gateway
  scenarios. Asserts nothing; every created id is registered for @After cleanup. All names/contexts are
  unique-by-construction via the ${UNIQUE:...} placeholders in the payload files.

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Create base REST API
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "restApiId" and deployed it

  Scenario: Create base SOAP API
    Given I have created an api from "artifacts/payloads/create_apim_test_soap_api.json" as "soapApiId" and deployed it

  Scenario: Create base GraphQL API
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "graphqlApiId"
    Then The response status code should be 201

  Scenario: Create base WebSocket API
    Given I have created an api from "artifacts/payloads/create_apim_test_websocket_api.json" as "websocketApiId" and deployed it
