@framework
Feature: Framework Verification 7.4a - multi-feature runner handoff (setup)

  First feature in a runner whose features={} array lists this before the consumer. Creates a resource into the
  runner's local TestContext scope so the next feature in the SAME runner can consume it. Asserts only that the
  resource is created.

  Scenario: create a shared resource into the runner's local scope
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "fvHandoffApiId" and deployed it
    When I retrieve the "apis" resource with id "fvHandoffApiId"
    And I put the response payload in context as "fvHandoffApiPayload"
