@setup
Feature: Setup APIs for configuration tests

  Creates the base APIs the configuration scenarios PATCH against — one of each type (REST, SOAP, GraphQL,
  WebSocket) in BOTH the super tenant and tenant1.com — storing each one's id and retrieved payload under
  type-/tenant-qualified context keys (configApiId, configSoapApiId, configGraphqlApiId, configWsApiId, each
  optionally suffixed @tenant1.com). Asserts nothing. Created as the matching tenant's admin. Listed first in
  the runner; teardown is the runner's per-class AfterClass sweep (intentionally untagged so the per-scenario
  cleanup hook does not delete these between scenarios — they must survive across the runner's scenarios).

  Scenario Outline: Create the REST configuration base API in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "configApiId<tenantSuffix>" and deployed it
    When I retrieve the "apis" resource with id "configApiId<tenantSuffix>"
    And I put the response payload in context as "configApiPayload<tenantSuffix>"

    Examples:
      | tenant       | tenantSuffix   |
      | super        |                |
      | tenant1.com  | @tenant1.com   |

  # SOAP and WebSocket base APIs (standard create-and-deploy step), in both tenants.
  Scenario Outline: Create the <label> configuration base API in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    And I have created an api from "<payloadFile>" as "<apiKey><tenantSuffix>" and deployed it
    When I retrieve the "apis" resource with id "<apiKey><tenantSuffix>"
    And I put the response payload in context as "<payloadKey><tenantSuffix>"

    Examples:
      | label     | payloadFile                                            | apiKey          | payloadKey           | tenant      | tenantSuffix |
      | SOAP      | artifacts/payloads/create_apim_test_soap_api.json      | configSoapApiId | configSoapApiPayload | super       |              |
      | SOAP      | artifacts/payloads/create_apim_test_soap_api.json      | configSoapApiId | configSoapApiPayload | tenant1.com | @tenant1.com |
      | WebSocket | artifacts/payloads/create_apim_test_websocket_api.json | configWsApiId   | configWsApiPayload   | super       |              |
      | WebSocket | artifacts/payloads/create_apim_test_websocket_api.json | configWsApiId   | configWsApiPayload   | tenant1.com | @tenant1.com |

  # GraphQL base API (multipart schema-upload step), in both tenants.
  Scenario Outline: Create the GraphQL configuration base API in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload<tenantSuffix>"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload<tenantSuffix>" as "configGraphqlApiId<tenantSuffix>"
    When I retrieve the "apis" resource with id "configGraphqlApiId<tenantSuffix>"
    And I put the response payload in context as "configGraphqlApiPayload<tenantSuffix>"

    Examples:
      | tenant      | tenantSuffix |
      | super       |              |
      | tenant1.com | @tenant1.com |
