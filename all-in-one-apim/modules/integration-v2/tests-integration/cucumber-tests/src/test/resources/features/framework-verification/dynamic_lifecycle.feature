@framework
Feature: Framework Verification 1.3 - end-to-end API lifecycle through dynamic ports

  Proves the create -> deploy -> publish -> subscribe -> token -> invoke flow still works when the
  API Manager runs in a DynamicApimContainer on dynamic/ephemeral host ports (no fixed port offset).
  A single linear scenario as super-tenant admin is sufficient to exercise every dynamic-port path.

  Scenario: Deploy and invoke a REST API through dynamic gateway ports
    Given I have initialized the NodeApp server container
    And I have initialized a dynamic API Manager container with label "verify-1.3"
    And The system is ready and I have valid access tokens for current user
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "RestAPIId" and deployed it
    When I publish the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
    And I wait until "apis" "RestAPIId" revision is deployed in the gateway
    When I have set up a application with keys
    And I subscribe to resource "RestAPIId", with "createdAppId" and obtained access token for "restSubscriptionId" with scope ""
    And I invoke the API resource at path "apiTestContext/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And I stop the dynamic API Manager container
