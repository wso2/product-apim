@cleanup
Feature: Subscription and API throttling survive a Gateway restart

  Restart-specific Gateway enforcement checks: each case configures an isolated throttling dimension
  (subscription bandwidth, API bandwidth, subscription burst) before restarting the Gateway, then verifies the
  exact throttle response afterward. The policy CRUD and application-bandwidth restart cases are in
  gateway/throttling_restart. The same feature runs in standard and email-user restart blocks; superTenantAdmin
  resolves to the appropriate block identity.

  @cap:gateway @feat:throttling-enforcement @type:regression @legacy:JWTBandwidthThrottlingServerRestartTestCase
  Scenario Outline: Subscription bandwidth throttling is enforced after a Gateway restart as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:restartSubBw}" allowing 1 KB per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "restartSubBwApiId" and deployed it
    And the "apis" resource "restartSubBwApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "restartSubBwApiId"
    And I put the response payload in context as "restartSubBwApiPayload"
    And I update the "apis" resource "restartSubBwApiId" and "restartSubBwApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "restartSubBwApiId" until its subscription policies equal the following within 180 seconds:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    When I publish the "apis" resource with id "restartSubBwApiId"
    Then The lifecycle status of API "restartSubBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "restartSubBwApiId"
    And I extract response field "context" and store it as "restartSubBwContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "restartSubBwConsumerKey"
    And I extract response field "consumerSecret" and store it as "restartSubBwConsumerSecret"
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "restartSubBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "restartSubBwSubscriptionId"
    Then The response status code should be 201
    When I request a client-credentials token using consumer key "restartSubBwConsumerKey" and secret "restartSubBwConsumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "restartSubBwToken"
    And I put a 4 KB text payload in context as "restartSubBwPayload"

    When I gracefully restart the API Manager server
    And the "apis" resource "restartSubBwApiId" should be live on the gateway, redeploying if propagation is lost
    And I invoke the API at gateway context "{{restartSubBwContext}}/1.0.0/reflect-body" with method "POST" using access token "restartSubBwToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{restartSubBwContext}}/1.0.0/reflect-body" with method "POST" using access token "restartSubBwToken" and payload "restartSubBwPayload" with content type "text/plain" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900804"

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @legacy:JWTBandwidthThrottlingServerRestartTestCase
  Scenario Outline: API bandwidth throttling is enforced after a Gateway restart as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:restartApiBw}" allowing 1 KB per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "restartApiBwApiId" and deployed it
    When I retrieve the "apis" resource with id "restartApiBwApiId"
    And I put the response payload in context as "restartApiBwApiPayload"
    And I update the "apis" resource "restartApiBwApiId" and "restartApiBwApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "restartApiBwApiId"
    Then The response status code should be 201
    And the "apis" resource "restartApiBwApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "restartApiBwApiId"
    Then The lifecycle status of API "restartApiBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "restartApiBwApiId"
    And I extract response field "context" and store it as "restartApiBwContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "restartApiBwConsumerKey"
    And I extract response field "consumerSecret" and store it as "restartApiBwConsumerSecret"
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "restartApiBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "restartApiBwSubscriptionId"
    Then The response status code should be 201
    When I request a client-credentials token using consumer key "restartApiBwConsumerKey" and secret "restartApiBwConsumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "restartApiBwToken"
    And I put a 4 KB text payload in context as "restartApiBwPayload"

    When I gracefully restart the API Manager server
    And the "apis" resource "restartApiBwApiId" should be live on the gateway, redeploying if propagation is lost
    And I invoke the API at gateway context "{{restartApiBwContext}}/1.0.0/reflect-body" with method "POST" using access token "restartApiBwToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{restartApiBwContext}}/1.0.0/reflect-body" with method "POST" using access token "restartApiBwToken" and payload "restartApiBwPayload" with content type "text/plain" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900800"

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @legacy:BurstControlServerRestartTestCase
  Scenario Outline: Subscription burst throttling is enforced after a Gateway restart as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:restartBurst}" allowing 1000 requests per minute with burst limit 5 per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "restartBurstApiId" and deployed it
    And the "apis" resource "restartBurstApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "restartBurstApiId"
    And I put the response payload in context as "restartBurstApiPayload"
    And I update the "apis" resource "restartBurstApiId" and "restartBurstApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "restartBurstApiId" until its subscription policies equal the following within 180 seconds:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    When I publish the "apis" resource with id "restartBurstApiId"
    Then The lifecycle status of API "restartBurstApiId" should be "Published"
    When I retrieve the "apis" resource with id "restartBurstApiId"
    And I extract response field "context" and store it as "restartBurstApiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "restartBurstApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "restartBurstSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    When I gracefully restart the API Manager server
    And the "apis" resource "restartBurstApiId" should be live on the gateway, redeploying if propagation is lost
    And I invoke the API at gateway context "{{restartBurstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 allowing interim statuses "200" within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900807"

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |
