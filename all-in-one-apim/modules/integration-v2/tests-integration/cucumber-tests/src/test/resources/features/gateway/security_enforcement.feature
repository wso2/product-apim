@cleanup
Feature: Gateway Security Enforcement

  Gateway-plane runtime security enforcement (negatives), in both the super tenant and tenant1.com:
  an invalid bearer token is rejected (401), and a valid token from an application that is NOT subscribed
  to the API is refused access (403). The valid-token happy path is covered by gateway/rest-invocation.
  Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:security-enforcement @type:negative @rule:invalid-token @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published API with an invalid token is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    When I put the following JSON payload in context as "invalidAccessToken"
    """
    abcdefgh
    """
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "invalidAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "Make sure you have provided the correct security credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:security-enforcement @type:negative @rule:no-subscription @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published API with an unsubscribed application's token is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Create an application + keys + token, but DO NOT subscribe it to the API
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Invoking without a subscription must be refused (403)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # End-to-end subscription-blocking runtime toggle (the legacy SubscriptionBlockingTestCase arc in one flow):
  # a subscribed token invokes (200), the subscription is blocked -> gateway refuses, then unblocked ->
  # invocation is restored. The block/unblock calls are management-plane (@dep:devportal); the assertion here is
  # the runtime enforcement, so it is @cap:gateway.
  @cap:gateway @feat:security-enforcement @type:regression @rule:subscription-blocking @dep:publisher @dep:devportal @legacy:SubscriptionBlockingTestCase
  Scenario Outline: A blocked subscription is refused at the gateway and restored on unblock as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and obtain a token
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Subscribed -> invocation succeeds
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Block the subscription -> gateway refuses the same token (401, code 900907 "temporarily blocked")
    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Unblock -> invocation is restored
    When I unblock the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
