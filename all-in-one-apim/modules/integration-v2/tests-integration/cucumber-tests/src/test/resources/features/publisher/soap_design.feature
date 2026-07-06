@cleanup
Feature: Publisher SOAP API Design

  Publisher-plane SOAP (passthrough) API design: create + deploy a SOAP API and publish it. Asserts only
  publisher-plane outcomes — SOAP invocation is covered by gateway/soap-invocation. Self-contained
  scenario, torn down by the per-scenario cleanup hook.

  @cap:publisher @feat:soap-design @type:smoke @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: Create, deploy and publish a SOAP API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_soap_api.json" as "soapApiId" and deployed it
    And The lifecycle status of API "soapApiId" should be "Created"
    When I publish the "apis" resource with id "soapApiId"
    Then The lifecycle status of API "soapApiId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:soap-design @type:negative @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: A subscriber-role user cannot create an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_soap_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
