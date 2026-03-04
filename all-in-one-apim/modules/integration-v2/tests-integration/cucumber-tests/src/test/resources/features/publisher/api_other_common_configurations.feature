Feature: API Other Common Configurations
  This feature validates other configuration updates (Subscription plans, and Custom properties, Resource definitions )
  for an API created.

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Create base APIs
  Scenario Outline: Creating an API
    Given I have created an api from "<payloadFile>" as "<apiID>" and deployed it
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"

    Examples:
      |payloadFile                                            | apiID        | apiUpdatePayload|
      |artifacts/payloads/create_apim_test_api.json           |  RestAPIId   | RestAPIPayload  |
      |artifacts/payloads/create_apim_test_soap_api.json      |  SoapAPIId   | SoapAPIPayload  |
      |artifacts/payloads/create_apim_test_websocket_api.json | AsyncAPIId   | AsyncAPIPayload |

  Scenario: Create GraphQL API
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "GraphQLAPIId"
    And I retrieve the "apis" resource with id "GraphQLAPIId"
    And I put the response payload in context as "graphQLAPIPayload"


  # Step 2: Add custom properties
  Scenario Outline: Configuring Custom properties
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """
    Examples:
      | apiID        | apiUpdatePayload | configType               | configValue                                                    |
      | RestAPIId    |  RestAPIPayload  | additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | RestAPIId    |  RestAPIPayload  | additionalProperties     | []                                                             |
      | SoapAPIId    |  SoapAPIPayload  | additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | GraphQLAPIId | graphQLAPIPayload| additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | AsyncAPIId   | AsyncAPIPayload  | additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|

 # Step 3: Update API Resources
  Scenario Outline: Configuring Resources
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The response should contain "newlyAddedResource"

    Examples:
      | apiID        | apiUpdatePayload | configType               | configValue                                                          |
      | RestAPIId    |RestAPIPayload    | operations               | [{"verb": "POST", "target": "/newlyAddedResource"}]                  |
      | SoapAPIId    |SoapAPIPayload    | operations               | [{"verb": "POST", "target": "/newlyAddedResource"}]                  |
      | GraphQLAPIId |graphQLAPIPayload | operations               | [{"verb": "QUERY", "target": "newlyAddedResource"}]                  |
      | AsyncAPIId   | AsyncAPIPayload  |operations                | [{"verb": "SUBSCRIBE", "target": "/newlyAddedResource"}]             |

  # Step 4: Update subscription Plans
  Scenario Outline: Update Subscription of API
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """
    Examples:
      | apiID        |  apiUpdatePayload | configType               | configValue                   |
      | RestAPIId    |  RestAPIPayload   | policies                 | ["Gold","Unlimited"]          |
      | RestAPIId    |  RestAPIPayload   | policies                 | ["Bronze","Gold","Silver"]    |
      | SoapAPIId    |  SoapAPIPayload   | policies                 | ["Bronze","Gold","Silver"]    |
      | GraphQLAPIId |  graphQLAPIPayload| policies                 | ["Bronze","Gold","Silver"]    |
      | AsyncAPIId   | AsyncAPIPayload   |policies                  | ["Bronze","Gold","Silver"]    |


  # Step 5: Add scopes for resources

  # Step 5.1 : Create a new shared scope, "scopeID" stored in context
  Scenario: Create a new shared scope
    When I create a new shared scope as "new-shared-scope"
    Then The response status code should be 201
    And The response should contain "new-shared-scope"

  # Step 5.2: Add scope to api
  Scenario Outline: Add scope to API
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """
    Examples:
      | apiID        |  apiUpdatePayload | configType               | configValue                   |
      | RestAPIId    |  RestAPIPayload   | scopes                   |  [{"shared":true,"scope":{"displayName":"new-shared-scope","bindings":["admin"],"name":"new-shared-scope","description":"This Scope is to test the creation of new scope","id":null,"usageCount":null}}]      |
      | SoapAPIId    |  SoapAPIPayload   | scopes                   |  [{"shared":true,"scope":{"displayName":"new-shared-scope","bindings":["admin"],"name":"new-shared-scope","description":"This Scope is to test the creation of new scope","id":null,"usageCount":null}}]      |
      | GraphQLAPIId |  graphQLAPIPayload| scopes                   |  [{"shared":true,"scope":{"displayName":"new-shared-scope","bindings":["admin"],"name":"new-shared-scope","description":"This Scope is to test the creation of new scope","id":null,"usageCount":null}}]      |
      | AsyncAPIId   |  AsyncAPIPayload  | scopes                   |  [{"shared":true,"scope":{"displayName":"new-shared-scope","bindings":["admin"],"name":"new-shared-scope","description":"This Scope is to test the creation of new scope","id":null,"usageCount":null}}]      |


  # Step 5.3: Update the api with created scope
  Scenario Outline: Update Scopes
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | apiID            |  apiUpdatePayload                 | configType       | configValue                                                                             |
      | RestAPIId        | RestAPIPayload                    |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"POST","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["new-shared-scope"],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |
      | SoapAPIId        | SoapAPIPayload                    |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"POST","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["new-shared-scope"],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |
      | GraphQLAPIId     | graphQLAPIPayload                 |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"QUERY","uriMapping":null,"throttlingPolicy":"Unlimited","target":"newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["new-shared-scope"],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |
      | AsyncAPIId       | AsyncAPIPayload                   |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"SUBSCRIBE","uriMapping":null,"throttlingPolicy":"Unlimited","target":"newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["new-shared-scope"],"amznResourceTimeout":null,"authType":"Application & Application User"}]|
      | RestAPIId        | RestAPIPayload                    |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"POST","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":[],"amznResourceTimeout":null,"authType":"Application & Application User"}]        |
      | SoapAPIId        | SoapAPIPayload                    |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"POST","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":[],"amznResourceTimeout":null,"authType":"Application & Application User"}]        |
      | GraphQLAPIId     | graphQLAPIPayload                 |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"QUERY","uriMapping":null,"throttlingPolicy":"Unlimited","target":"newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":[],"amznResourceTimeout":null,"authType":"Application & Application User"}]        |
      | AsyncAPIId       | AsyncAPIPayload                   |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"SUBSCRIBE","uriMapping":null,"throttlingPolicy":"Unlimited","target":"newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":[],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |


  # Step 5.4: Delete the created scope
  Scenario: Delete the created shared scope
    When I delete shared scope with "scopeID"
    Then The response status code should be 200

 # Step 6: Remove created APIs
  Scenario Outline: Remove the APIs
    When I delete the "apis" resource with id "<apiID>"
    Then The response status code should be 200

    Examples:
      |   apiID    |
      | RestAPIId  |
      | SoapAPIId  |
      |GraphQLAPIId|
      | AsyncAPIId |
