@cleanup
Feature: Gateway Custom Authorization Header

  Gateway-plane invocation where the container reads the OAuth token from a custom header instead of the
  standard Authorization one (the IntegrationV2-CustomAuthHeaderAndAppSharing block overlay sets
  `[apim.oauth_config] auth_header = "Test-Custom-Header"`). A published API invoked with the token in the
  configured custom header succeeds; the same token in the standard Authorization header is not recognised and
  is rejected. Runs in both the super tenant and tenant1.com as the tenant admin (the flow spans publish +
  subscribe + invoke). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:custom-auth-header @type:smoke @dep:publisher @legacy:CustomAuthHeaderTestCase
  Scenario Outline: Invoke a published API using the configured custom auth header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # The token is accepted when presented in the configured custom header.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" in header "Test-Custom-Header" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:custom-auth-header @type:negative @dep:publisher @legacy:CustomAuthHeaderTestCase
  Scenario Outline: The standard Authorization header is not accepted when a custom auth header is configured as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # The same token in the standard Authorization header is not recognised by the gateway (token expected
    # only in the configured custom header), so invocation is rejected.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
