@cleanup
Feature: Throttling state survives component restarts

  These restart-specific checks complement the steady-state enforcement scenarios. The policy CRUD case restarts
  the Control Plane after creating/deleting policy resources. The application-bandwidth case configures an
  isolated throttling dimension before restarting the Gateway, then verifies the exact throttle response
  afterward. The subscription, API and burst Gateway-restart cases are in gateway/throttling_restart_gateway_limits.
  The same feature runs in standard and email-user restart blocks; superTenantAdmin resolves to the appropriate
  block identity.

  @cap:admin @feat:throttling-policies @type:regression @legacy:ApplicationThrottlingPolicyServerRestartTestCase @legacy:CustomThrottlingPolicyServerRestartTestCase
  Scenario Outline: Application policy deletion and custom policy CRUD survive a Control Plane restart as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "${UNIQUE:restartAppPolicy}" allowing 10 requests per minute
    Then The response status code should be 201
    And I extract response field "policyId" and store it as "restartAppPolicyId"
    When I retrieve the "application" throttling policy with id "restartAppPolicyId"
    Then The response status code should be 200
    And The value of response field "policyName" should be "{{appThrottlePolicyName}}"
    And The value of response field "defaultLimit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "defaultLimit.requestCount.requestCount" should be "10"
    When I delete the "application" throttling policy with id "restartAppPolicyId"
    Then The response status code should be 200

    # Custom throttling rules are super-tenant global resources. Run their lifecycle as the super-tenant admin,
    # independently of which tenant's application policy was prepared above.
    When I have valid access tokens as "superTenantAdmin"
    And I create a custom throttling policy "${UNIQUE:restartCustomRule}" throttling API context "restartDummyContext" after 10 requests per minute
    Then The response status code should be 201
    And I extract response field "policyId" and store it as "restartCustomPolicyId"
    When I gracefully restart the Control Plane
    And I retrieve the "custom" throttling policy with id "restartCustomPolicyId"
    Then The response status code should be 200
    And The value of response field "policyName" should be "{{customThrottlePolicyName}}"
    And The response field "siddhiQuery" should be non-blank
    When I update the "custom" throttling policy "restartCustomPolicyId" setting its description to "updated after restart"
    Then The response status code should be 200
    And The response should contain "updated after restart"
    When I retrieve the "custom" throttling policy with id "restartCustomPolicyId"
    Then The response status code should be 200
    And The value of response field "description" should be "updated after restart"
    When I create a custom throttling policy "{{customThrottlePolicyName}}" throttling API context "restartDummyContext" after 10 requests per minute
    Then The response status code should be 409
    When I delete the "custom" throttling policy with id "restartCustomPolicyId"
    Then The response status code should be 200
    When I retrieve the "custom" throttling policy with id "restartCustomPolicyId"
    Then The response status code should be 404

    When I have valid access tokens as "<actor>"
    And I retrieve the "application" throttling policy with id "restartAppPolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @legacy:JWTBandwidthThrottlingServerRestartTestCase
  Scenario Outline: Application bandwidth throttling is enforced after a Gateway restart as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "${UNIQUE:restartAppBw}" allowing 1 KB per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "restartAppBwApiId" and deployed it
    And the "apis" resource "restartAppBwApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "restartAppBwApiId"
    Then The lifecycle status of API "restartAppBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "restartAppBwApiId"
    And I extract response field "context" and store it as "restartAppBwContext"
    When I create an application "${UNIQUE:restartAppBwApplication}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "restartAppBwConsumerKey"
    And I extract response field "consumerSecret" and store it as "restartAppBwConsumerSecret"
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "restartAppBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "restartAppBwSubscriptionId"
    Then The response status code should be 201
    When I request a client-credentials token using consumer key "restartAppBwConsumerKey" and secret "restartAppBwConsumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "restartAppBwToken"
    And I put a 4 KB text payload in context as "restartAppBwPayload"

    When I gracefully restart the API Manager server
    And the "apis" resource "restartAppBwApiId" should be live on the gateway, redeploying if propagation is lost
    And I invoke the API at gateway context "{{restartAppBwContext}}/1.0.0/reflect-body" with method "POST" using access token "restartAppBwToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{restartAppBwContext}}/1.0.0/reflect-body" with method "POST" using access token "restartAppBwToken" and payload "restartAppBwPayload" with content type "text/plain" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |
