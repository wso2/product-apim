@cleanup
Feature: Gateway Lifecycle-Stage Invocation

  Ports the lifecycle-stage invocation semantics from the legacy APIRevisionServerRestartTestCase (functional
  concern; the legacy "restart" was incidental): the gateway's runtime response to an invocation depends on the
  API's lifecycle state — a deployed-but-unpublished (CREATED) API is invocable via the publisher internal key,
  a PUBLISHED API via a subscription token (200), a BLOCKED API is refused (503), a DEPRECATED API is still
  invocable (200), and a RETIRED API is gone (404). Run in BOTH the super tenant and tenant1.com to prove the
  lifecycle enforcement is tenant-agnostic (the tenant API is addressed by its full /t/<tenant> context). Runs
  in the concurrent IntegrationV2-Gateway block (backend started). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIRevisionServerRestartTestCase
  Scenario Outline: The gateway response to an invocation tracks the API lifecycle state as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "revApiId" and deployed it
    When I retrieve the "apis" resource with id "revApiId"
    And I extract response field "context" and store it as "revContext"

    # CREATED (deployed, not yet published): invocable via the publisher internal API key.
    When I generate an internal API key for API "revApiId" and store it as "internalKey"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using internal key "internalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # PUBLISHED: invocable via an application subscription token.
    When I publish the "apis" resource with id "revApiId"
    Then The lifecycle status of API "revApiId" should be "Published"
    When I have set up application with keys, subscribed to API "revApiId", and obtained access token for "revSub"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # BLOCKED: the gateway refuses the invocation with 503.
    When I change the lifecycle of API "revApiId" with action "Block"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503

    # DEPRECATED: still invocable (200).
    When I change the lifecycle of API "revApiId" with action "Deprecate"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # RETIRED: undeployed from the gateway → 404.
    When I change the lifecycle of API "revApiId" with action "Retire"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
