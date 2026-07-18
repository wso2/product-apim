@cleanup
Feature: Publisher Mandatory Custom API Properties

  Ports MandatoryPropertiesTestWithRestart: with a mandatory custom API property declared server-side
  ([[apim.publisher.custom_properties]] required=true name="PropertyName"), updating an API with that property
  present but EMPTY is rejected (400 "required properties cannot be empty"), and updating it with a non-empty
  value succeeds (200). Runs in its own IntegrationV2-MandatoryProperties block (the overlay declares the
  property); the legacy "restart" was incidental (config is applied at block boot). Teardown via @cleanup.

  @cap:publisher @feat:api-config @rule:mandatory-properties @type:regression @legacy:MandatoryPropertiesTestWithRestart
  Scenario Outline: A mandatory custom API property must be non-empty on update as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "mpApiId" and deployed it
    # Updating with the required property present but empty is rejected (the server validates
    # additionalPropertiesMap, the current map form — not the deprecated additionalProperties array).
    When I retrieve the "apis" resource with id "mpApiId"
    And I put the response payload in context as "mpApiPayload"
    When I update the "apis" resource "mpApiId" and "mpApiPayload" with configuration type "additionalPropertiesMap" and value:
      """
      {"PropertyName":{"name":"PropertyName","value":"","display":false}}
      """
    Then The response status code should be 400
    # Updating with a non-empty value for the required property succeeds.
    When I retrieve the "apis" resource with id "mpApiId"
    And I put the response payload in context as "mpApiPayload"
    When I update the "apis" resource "mpApiId" and "mpApiPayload" with configuration type "additionalPropertiesMap" and value:
      """
      {"PropertyName":{"name":"PropertyName","value":"PropertyValue","display":false}}
      """
    Then The response status code should be 200
    And The response should contain "PropertyValue"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
