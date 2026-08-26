@cleanup
Feature: Token Issuance By A Secondary-Store Principal

  The SUPER_TENANT_USER_STORE_USER factory leg of the token tests: a SECONDARY.COM store principal minting a token
  at the token endpoint / userinfo / gateway. Split out of token_issuance.feature and given its OWN runner because
  that feature is run by TWO blocks - IntegrationV2-KeyManager and IntegrationV2-EmailUserName - and these arcs can
  only pass in the first.

  WHY THEY CANNOT RUN IN THE EMAIL-USERNAME BLOCK (measured, not assumed). That block sets emailUserMode, and with
  EnableEmailUserName a username is split on its LAST "@" only when it carries TWO OR MORE. A store principal
  "SECONDARY.COM/subscriberUser1@tenant1.com" carries exactly one, so it resolves to carbon.super and never to
  tenant1 - the tenant row is refused 401 "Unauthenticated request" while the carbon.super row passes. That is the
  specified rule gateway/basic_auth_email_username.feature exists to pin, so the fix is to keep these arcs out of
  that block rather than to seed the store into it.

  Requires initSecondaryUserStore on its block: the seeder provisions SECONDARY.COM/subscriberUser1 and
  SECONDARY.COM/publisherUser1 per tenant.

  # C5c — ports the SUPER_TENANT_USER_STORE_USER factory leg of the token tests (§12): a SECONDARY.COM store
  # principal minting a token at the token endpoint / userinfo / gateway — never exercised here before. The token
  # OWNER is the store SUBSCRIBER (subscriberUser1), because every arc must create an application + generate keys and
  # apim:app_manage / apim:subscribe map to Internal/subscriber only — the store PUBLISHER (creator/publisher) cannot
  # create an application. The store subscriber is still a store-backed principal, so this is real store-user parity.
  #
  # S1 (OpenIDTokenAPITestCase store-user leg): the store principal obtains an openid-scoped password-grant token and
  # calls userinfo. Mirrors the openid scenario above. The userinfo step is tenant-aware (resolves the acting actor's
  # tenant), so the @tenant1.com row genuinely exercises t/<tenant>/oauth2/userinfo. Runs in BOTH tenants (×2) via
  # the store-qualified @tenant key.
  @cap:key-manager @feat:token-issuance @type:regression @rule:store-principal @legacy:OpenIDTokenAPITestCase
  Scenario Outline: A secondary-store principal generates an OpenID-scoped token and calls userinfo as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And The response should contain "openid"
    When I invoke the OpenID userinfo endpoint using access token "generatedAccessToken"
    Then The response status code should be 200

    Examples:
      | actor                                     |
      | SECONDARY.COM/subscriberUser1             |
      | SECONDARY.COM/subscriberUser1@tenant1.com |

  # S2 (TokenAPITestCase store-user leg): a store principal's token reaches a published API through the gateway (200 +
  # backend body). Both principals are store users — the store PUBLISHER creates/deploys/publishes the API, then the
  # store SUBSCRIBER (the token owner) subscribes and mints the password-grant token that invokes the gateway.
  # Explicit storeActor + providerActor columns make both store principals visible at the call site (both live in
  # the same tenant per row). Runs in BOTH tenants (×2).
  @cap:key-manager @feat:token-issuance @type:regression @rule:store-principal @dep:gateway @legacy:TokenAPITestCase
  Scenario Outline: A secondary-store principal's token invokes a published API through the gateway as <storeActor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    # Act as the store SUBSCRIBER (token owner): create app, subscribe, mint the token that invokes the gateway.
    Given The system is ready and I have valid devportal access token as "<storeActor>"
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
    # The store principal's token is the credential under test; pin the backend body so the 200 proves it carried
    # through to the upstream (same body string the sandbox/refresh scenarios assert).
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | storeActor                                | providerActor                            |
      | SECONDARY.COM/subscriberUser1             | SECONDARY.COM/publisherUser1             |
      | SECONDARY.COM/subscriberUser1@tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

  # S3 (RefreshTokenTestCase store-user leg): a store principal re-issues its access token via the refresh grant and
  # the RE-ISSUED token invokes the gateway (200 + backend body) — proving the refreshed credential works end-to-end,
  # not merely that the exchange returned 200. Provider = store publisher, token owner = store subscriber. Runs ×2.
  @cap:key-manager @feat:token-issuance @type:regression @rule:store-principal @dep:gateway @legacy:RefreshTokenTestCase
  Scenario Outline: A secondary-store principal re-issues an access token via the refresh grant and invokes as <storeActor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    # Act as the store SUBSCRIBER (token owner): create app, subscribe, mint + refresh the token that invokes.
    Given The system is ready and I have valid devportal access token as "<storeActor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
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
    When I request a new OAuth access token using refresh token "refreshToken"
    Then The response status code should be 200
    # The REFRESHED store-principal token is the credential under test — pin the backend body so the 200 proves the
    # re-issued token carried through to the upstream, not merely that the exchange returned 200.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | storeActor                                | providerActor                            |
      | SECONDARY.COM/subscriberUser1             | SECONDARY.COM/publisherUser1             |
      | SECONDARY.COM/subscriberUser1@tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |
