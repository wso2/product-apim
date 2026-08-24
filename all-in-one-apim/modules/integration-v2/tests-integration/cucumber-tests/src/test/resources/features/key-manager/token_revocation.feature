@cleanup
Feature: Key Manager Token Revocation

  Key-manager-plane token revocation: after revoking an OAuth access token, gateway invocation with that
  token is rejected (eventually 401). Runs as admin in both the super tenant and tenant1.com. Also covers
  ONE-TIME tokens — the shipped revokeOneTimeToken common operation policy, which revokes a token carrying a
  configured scope as it passes through the request flow — including the no-scope control that proves the
  revocation is driven by the scope. Those run in BOTH tenants too (admin and admin@tenant1.com), unlike
  legacy, which gated them to the super tenant. Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:token-revocation @type:regression @dep:gateway @legacy:RevokeTokenTestCase
  Scenario Outline: Revoke an access token and verify invocation is blocked as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
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
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    When I revoke the OAuth access token "generatedAccessToken"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports RevokeOneTimeTokenFlowTestCase#testAPIInvocationOfAttachedRevokeOneTimePolicyUsingOneTimeToken together
  # with its control, #testAPIInvocationWithoutAddingOneTimeTokenScopeToJWT. The two belong in ONE scenario: the
  # control is only meaningful if the no-scope token is still accepted AFTER the scoped token has demonstrably
  # been revoked, which is what proves the 401 came from the policy reading the scope rather than from the API,
  # the application or the gateway going bad. Legacy ran them as separate tests each padded with a fixed
  # Thread.sleep(15000); here the revocation is observed by polling, and the control invocation happens strictly
  # after that observation — no sleep, and a tighter ordering guarantee than legacy had.
  #
  # The policy is attached BEFORE the first deploy so the gateway never serves a revision without it.
  @cap:key-manager @feat:token-revocation @rule:one-time-token @type:regression @dep:gateway @dep:publisher @legacy:RevokeOneTimeTokenFlowTestCase
  Scenario Outline: A one-time token self-revokes after a single use while a token without the scope keeps working as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ottApiPayload"
    And I create an "apis" resource with payload "ottApiPayload" as "ottApiId"
    Then The response status code should be 201
    And I extract response field "context" and store it as "ottApiContext"

    When I attach the common operation policy "revokeOneTimeToken" to operation 0 of API "ottApiId" in flows "request" with parameters "{\"scope\":\"OTT\"}"
    Then The response status code should be 200
    When I publish the "apis" resource with id "ottApiId"
    Then The lifecycle status of API "ottApiId" should be "Published"
    And I deploy the API with id "ottApiId"
    Then The response status code should be 201

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "ottAppPayload"
    And I create an application with payload "ottAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ottKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"], "scopes": ["OTT"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "ottKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "ottSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "ottApiId" using application "createdAppId" with payload "ottSubPayload" as "ottSubId"
    Then The response status code should be 201

    # Two tokens from the SAME application: one carrying the one-time-token scope, one without it.
    When I request an OAuth access token for the current user using password grant with scope "OTT"
    Then The response status code should be 200
    And The response should contain "OTT"
    And I copy context value "generatedAccessToken" to "oneTimeToken"
    When I request an OAuth access token for the current user using password grant with scope ""
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "plainToken"

    # Both tokens are accepted on their first use.
    When I invoke the API at gateway context "{{ottApiContext}}/1.0.0/customers/123/" with method "GET" using access token "oneTimeToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    When I invoke the API at gateway context "{{ottApiContext}}/1.0.0/customers/123/" with method "GET" using access token "plainToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # The one-time token revoked ITSELF on that first use.
    When I invoke the API at gateway context "{{ottApiContext}}/1.0.0/customers/123/" with method "GET" using access token "oneTimeToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # CONTROL, strictly after the revocation above was observed: the token WITHOUT the scope is still accepted.
    When I invoke the API at gateway context "{{ottApiContext}}/1.0.0/customers/123/" with method "GET" using access token "plainToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports RevokeOneTimeTokenFlowTestCase#testAPIInvocationWithOutsideUser: the one-time-token policy revokes on
  # the token's SCOPE, so it must fire for a user who is not the application owner and holds no portal role — a
  # scenario-provisioned Internal/everyone user whose token is minted with the application's own credentials.
  @cap:key-manager @feat:token-revocation @rule:one-time-token @type:regression @dep:gateway @dep:publisher @legacy:RevokeOneTimeTokenFlowTestCase
  Scenario Outline: A one-time token issued to a user outside the portal roles also self-revokes after a single use as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ottOutApiPayload"
    And I create an "apis" resource with payload "ottOutApiPayload" as "ottOutApiId"
    Then The response status code should be 201
    And I extract response field "context" and store it as "ottOutApiContext"

    When I attach the common operation policy "revokeOneTimeToken" to operation 0 of API "ottOutApiId" in flows "request" with parameters "{\"scope\":\"OTT\"}"
    Then The response status code should be 200
    When I publish the "apis" resource with id "ottOutApiId"
    Then The lifecycle status of API "ottOutApiId" should be "Published"
    And I deploy the API with id "ottOutApiId"
    Then The response status code should be 201

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "ottOutAppPayload"
    And I create an application with payload "ottOutAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ottOutKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"], "scopes": ["OTT"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "ottOutKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "ottOutSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "ottOutApiId" using application "createdAppId" with payload "ottOutSubPayload" as "ottOutSubId"
    Then The response status code should be 201

    When I provision a user with name prefix "ottOutsideUser" password "OutsideUser@123" and roles "Internal/everyone" storing the username as "ottOutsideUser"
    And I request an OAuth access token using password grant as user "{{ottOutsideUserLoginName}}" with password "OutsideUser@123" requesting scope "OTT"
    Then The response status code should be 200
    And The response should contain "OTT"

    When I invoke the API at gateway context "{{ottOutApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{ottOutApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports testInvokeJWTUserToken: a password-grant token is invalidated IMPLICITLY when the resource owner's
  # password is reset out of band by an admin — no explicit revoke call. This is the security property a
  # re-authentication test cannot show: proving the NEW password works says nothing about whether the token
  # issued under the OLD one is still honoured at the gateway.
  #
  # The resource owner is a principal this scenario provisions at runtime, NOT one of the block's seeded actors:
  # those live in the block-shared actor registry with their passwords cached there, so resetting one would break
  # every sibling runner in the block. Because that principal is deliberately not registered as an actor, the
  # token is requested with the EXPLICIT-credentials password-grant step.
  #
  # Runs x2 tenants so token invalidation on credential change is verified in each tenant's key-manager context.
  @cap:key-manager @feat:token-revocation @rule:password-change @type:regression @dep:gateway @dep:admin @legacy:APISecurityTestCase
  Scenario Outline: An already-issued access token is rejected after its user's password is reset as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "pcApiId" and deployed it
    When I publish the "apis" resource with id "pcApiId"
    Then The lifecycle status of API "pcApiId" should be "Published"
    When I retrieve the "apis" resource with id "pcApiId"
    And I extract response field "context" and store it as "pcApiContext"

    # The resource owner, provisioned for this scenario only, under a runner-unique name.
    When I generate a unique alphanumeric value and store it as "pcUsr"
    And I provision store user "{{pcUsr}}" with password "OldPass@123" and roles "Internal/subscriber" in tenant "<tenant>"

    # An application (password grant enabled) subscribed to the API.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "pcAppPayload"
    And I create an application with payload "pcAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pcAppKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "pcAppKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "pcSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "pcApiId" using application "createdAppId" with payload "pcSubscriptionPayload" as "pcSubscriptionId"
    Then The response status code should be 201

    # A token issued to that user under the OLD password invokes successfully.
    When I request an OAuth access token using password grant as user "{{pcUsr}}<suffix>" with password "OldPass@123"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{pcApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # The admin resets that user's password. The token is not touched, and nothing is revoked explicitly.
    When I change the password of user "pcUsr" to "NewPass@456" as the tenant admin

    # The ALREADY-ISSUED token is now rejected at the gateway (polled — the invalidation propagates via JMS).
    # "generatedAccessToken" still holds the OLD token: the new one is requested only AFTER this assertion, so
    # the old value is asserted in place rather than copied to a second key (a copy that silently failed to
    # resolve would send a garbage bearer token and 401 for the wrong reason).
    When I invoke the API at gateway context "{{pcApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Positive control: the account itself still works — a token issued under the NEW password invokes (200). So
    # the 401 above is the old token being invalidated, not the user having been locked out or deleted.
    When I request an OAuth access token using password grant as user "{{pcUsr}}<suffix>" with password "NewPass@456"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{pcApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Remove the runtime principal (the step's deleteUser is store-agnostic; this name is in the primary store).
    When I remove the secondary user store user "{{pcUsr}}" in tenant "<tenant>"

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |
