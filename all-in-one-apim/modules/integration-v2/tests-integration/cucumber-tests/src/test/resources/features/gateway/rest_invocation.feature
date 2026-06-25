@cleanup
Feature: Gateway REST API Invocation

  Gateway-plane runtime invocation of a published REST API: subscribe an application, obtain an access
  token, and invoke the API through the gateway expecting a 200. This is the gateway counterpart of the
  publisher-plane create/publish features (which assert only publisher outcomes). Runs in both the super
  tenant and tenant1.com as the tenant admin (the flow spans publish + subscribe + invoke). Teardown via
  the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @type:smoke @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published REST API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Capture the API's full gateway context (already carries /t/<tenant> for tenant APIs)
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and obtain an access token
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # Invoke through the gateway by full context path (retry while the gateway becomes eventually consistent)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
