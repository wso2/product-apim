Feature: Migrated shared scopes for existing and new apis

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Migrated API scope Management
    # Step 1: Create new api
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "RestAPIId" and deployed it
    And I retrieve the "apis" resource with id "RestAPIId"
    And I put the response payload in context as "RestAPIPayload"

    And I wait for deployment of the resource in "RestAPIPayload"
    And I publish the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
    Then The lifecycle status of API "RestAPIId" should be "Published"

    # Step 2: Find migrated api
    When I find the apiUUID of the API created with the name "APIM18PublisherTest" and version "1.0.0" as "migratedAPIId"
    And I retrieve the "apis" resource with id "migratedAPIId"
    Then The response status code should be 200
    And I put the response payload in context as "migratedAPIPayload"

  # Step 3: Create a new shared scope, "scopeID" stored in context
  Scenario: Create a new shared scope
    When I create a new shared scope as "new-shared-scope"
    Then The response status code should be 201
    And The response should contain "new-shared-scope"

 # Step 4: Add migrated and new shared scope to apis
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
      | apiID        |  apiUpdatePayload     | configType               | configValue                   |
      | RestAPIId    |  RestAPIPayload       | scopes                   |  [{"shared":true,"scope":{"displayName":"adp-shared-scope-with-roles","bindings":["ADP_CREATOR", "ADP_SUBSCRIBER"],"name":"adp-shared-scope-with-roles","description":"Shared scope with role mapping","id":null,"usageCount":null}}]      |
      | migratedAPIId|  migratedAPIPayload   | scopes                   |  [{"shared":true,"scope":{"displayName":"adp-shared-scope-with-roles","bindings":["ADP_CREATOR", "ADP_SUBSCRIBER"],"name":"adp-shared-scope-with-roles","description":"Shared scope with role mapping","id":null,"usageCount":null}}, {"shared":true,"scope":{"displayName":"new-shared-scope","bindings":["admin"],"name":"new-shared-scope","description":"This Scope is to test the creation of new scope","id":null,"usageCount":null}}]      |

  # Step 5: Add migrated and new shared scopes to operations
  Scenario Outline: Add Scopes to resources
    When I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"

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
      | migratedAPIId    | migratedAPIPayload                |policies          |       ["Bronze","Gold","Unlimited"]  |
      | RestAPIId        | RestAPIPayload                    |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"GET","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/customers/{id}","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["adp-shared-scope-with-roles"],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |
      | migratedAPIId    | migratedAPIPayload                |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"GET","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/customers/{id}","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["adp-shared-scope-with-roles"],"amznResourceTimeout":null,"authType":"Application & Application User"}, {"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"DELETE","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/customers/{id}","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["new-shared-scope"],"amznResourceTimeout":null,"authType":"Application & Application User"}]  |


 # Step 6: Deploy new revision after applying changes
  Scenario Outline: Deploy new revision
    When I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiPayload>"
    When I deploy the API with id "<apiID>"
    Then The response status code should be 201
    And I wait until "<apiID>" revision is deployed in the gateway

    Examples:
    | apiID         | apiPayload        |
    |RestAPIId      |RestAPIPayload     |
    |migratedAPIId  |migratedAPIPayload |


  # Step 7: Create an application
  Scenario: Create an application
    When I have set up a application with keys
    Then The response status code should be 200


  # Step 8: Verify the behaviour, should give 403 for "adp-shared-scope-with-roles"
  Scenario Outline: Invoking apis
    When I subscribe to resource "<apiID>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope "adp-shared-scope-with-roles, new-shared-scope"
    And I invoke the API resource at path "<resourcePath>" with method "<method>" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be <statusCode>

    When I delete the subscription with id "<subscriptionID>"
    Then The response status code should be 200

    Examples:
      | apiID          | subscriptionID           | resourcePath                            | method   | statusCode |
      |RestAPIId       | restSubscriptionId       | apiTestContext/1.0.0/customers/123/     | GET      | 403        |
      |migratedAPIId   | migratedSubscriptionId   | apiContext/1.0.0/customers/124/         | GET      | 403        |
      |migratedAPIId   | migratedSubscriptionId   | apiContext/1.0.0/customers/125/         | DELETE   | 200        |


  # Step 9: Edit shared scopes
  Scenario:
    When I fetch the shared scope with name "adp-shared-scope-with-roles" into context as "migratedScopeId"
    When I put JSON payload from file "artifacts/payloads/updated_migrated_shared_scope.json" in context as "<scopePayload>"
    And I update shared scope "migratedScopeId" with payload "<scopePayload>"
    Then The response status code should be 200
    And The response should contain "admin"

  # Step 10: Verify the behaviour after update, should give 200 for admin
  Scenario Outline: Invoking apis
    When I subscribe to resource "<apiID>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope "adp-shared-scope-with-roles"
    And I invoke the API resource at path "<resourcePath>" with method "<method>" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    When I delete the subscription with id "<subscriptionID>"
    Then The response status code should be 200

    Examples:
      | apiID          | subscriptionID           | resourcePath                            | method|
      |RestAPIId       | restSubscriptionId       | apiTestContext/1.0.0/customers/123/     | GET   |
      |migratedAPIId   | migratedSubscriptionId   | apiContext/1.0.0/customers/123/         | GET   |


  # Step 11: Delete migrated and new shared scope
  Scenario Outline: Delete scopes
    When I delete shared scope with "<scopeId>"
    Then The response status code should be 409

    Examples:
    | scopeId       |
    |migratedScopeId|
    |scopeID        |

  # Step 12: Delete newy created resources
  Scenario: Delete the API
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
