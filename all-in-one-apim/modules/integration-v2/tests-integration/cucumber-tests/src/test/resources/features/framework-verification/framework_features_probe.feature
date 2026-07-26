@framework
Feature: Framework Verification 7.2/7.3/7.7/7.8 - test-authoring framework features

  Verifies the core mechanisms added during feature porting, in one booted block (initTenantUsers + initBackend
  + a tomlExtraOverlayPath overlay): tenancy provisioning & routing (7.2), the actor/Identity model with auth
  keys (7.3), gateway invocation wiring (7.7), and the extra-overlay merge (7.8). Reuses the product step
  library. Teardown via the per-runner AfterClass sweep.

  @rule:overlay
  Scenario: 7.8 - the feature-specific overlay was merged into the block's container
    Then the in-container deployment.toml contains the marker "fv-7-8-marker"

  @rule:actor
  Scenario: 7.3 - the default actors are provisioned and mint their own tokens
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I act as "subscriberUser"
    And I act as "admin"

  @rule:tenancy
  Scenario: 7.2 - a tenant actor's resource is routed under the tenant context
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "fvTenantApiId" and deployed it
    When I retrieve the "apis" resource with id "fvTenantApiId"
    Then The response should contain "/t/tenant1.com"

  @rule:gateway
  Scenario: 7.7 - a published API is invocable through the gateway backend
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "fvGwApiId" and deployed it
    When I publish the "apis" resource with id "fvGwApiId"
    Then The lifecycle status of API "fvGwApiId" should be "Published"
    When I retrieve the "apis" resource with id "fvGwApiId"
    And I extract response field "context" and store it as "apiContext"
    When I have set up application with keys, subscribed to API "fvGwApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
