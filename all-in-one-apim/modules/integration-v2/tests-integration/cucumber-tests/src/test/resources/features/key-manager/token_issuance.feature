@cleanup
Feature: Key Manager Token Issuance

  Key-manager-plane token issuance across grant/scope variants: JWT-format production tokens, OpenID-scoped
  tokens (+ userinfo, at the tenant-qualified path for a tenant actor), refresh-token re-issuance, sandbox-key
  tokens for both a JWT-tokenType and an OAUTH-tokenType application, key-generation validation of the application's
  token-expiry additionalProperties, authorization-code grants, and per-role filtering of the scopes actually
  granted in an issued token. Runs as admin in both the super tenant and tenant1.com. The refresh, sandbox,
  token-expiry and role-filtering variants invoke the gateway to prove the issued token works end-to-end.
  Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:token-issuance @type:smoke @rule:jwt-format @legacy:JWTTokenFormatTestCase
  Scenario Outline: Generate a production OAuth token in JWT format as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
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
    And The generated access token should be in JWT format

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Also covers OpenIDTokenAPITestCase (openid-scoped password-grant token → scope contains openid; userinfo → 200).
  # TENANT-PATH VARIANT: the admin@tenant1.com row does exercise t/<tenant>/oauth2/userinfo — but NOT because the
  # actor's @domain re-routes the request; it does not. The userinfo step resolves the ACTING actor's tenant and
  # builds the tenant-qualified path explicitly (Utils.getUserInfoEndpointURL(baseUrl, tenantDomain), mirroring the
  # introspect helper and the legacy tenant branch). Until that was wired, this comment asserted tenant coverage the
  # suite did not have and BOTH rows hit the super-tenant endpoint — so do not "simplify" the step back to the
  # no-arg overload without deleting this claim as well.
  @cap:key-manager @feat:token-issuance @type:smoke @rule:openid @legacy:OpenIDTokenTestCase @legacy:OpenIDTokenAPITestCase
  Scenario Outline: Generate an OpenID-scoped token and call userinfo as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
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
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @type:regression @rule:refresh @dep:gateway @legacy:RefreshTokenTestCase
  Scenario Outline: Re-issue an access token via refresh token and invoke as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
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
    # The REFRESHED token is the credential under test, so the backend payload is pinned — that is what shows the
    # newly-minted token carried the call through to the upstream rather than merely producing a 200.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Also provides parity for TokenAPITestCase (sandbox-key token invokes the API → 200; the production-key
  # password-grant and client-credential token invocations of TokenAPITestCase are covered by the smoke
  # JWT-token scenario above + gateway/rest-invocation).
  @cap:key-manager @feat:token-issuance @type:regression @rule:sandbox @dep:gateway @legacy:SandboxTokenTestCase @legacy:TokenAPITestCase
  Scenario Outline: Issue a sandbox-scoped token and invoke as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateSandboxApplicationKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateSandboxApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "SANDBOX"
    Then The response status code should be 200
    # The SANDBOX-key token is the credential under test; pin the backend payload so the 200 is evidence the token
    # was honoured all the way to the upstream. (It does not identify WHICH endpoint served it — this API's
    # production and sandbox endpoints are the same URL — that distinction is gateway/rest_invocation's
    # endpoint-routing scenario, which uses two path-echoing endpoints.)
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports TokenAPITestCase#testInfiniteTokenAPITestCase — the key-generation validation of the application's
  # `additionalProperties.application_access_token_expiry_time`, which no other scenario exercises (no scenario
  # passed additionalProperties to key generation at all, and error code 900970 appeared nowhere in the corpus).
  # Two halves in ONE scenario because the negative needs the positive control on the SAME application: a
  # NEGATIVE expiry is refused with 400 + the exact 900970 envelope, while Long.MAX_VALUE is accepted and BOTH
  # the sandbox and the production token it issues invoke the gateway successfully. Pinning code AND message AND
  # description matters here: 400 is the management API's catch-all for a dozen unrelated validation faults, so a
  # status-only assertion would pass for the wrong reason.
  @cap:key-manager @feat:token-issuance @rule:token-expiry @type:regression @dep:gateway @legacy:TokenAPITestCase
  Scenario Outline: Application token-expiry additionalProperties are validated at key generation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "infTokenApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "infTokenApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "infTokenApiId"
    Then The lifecycle status of API "infTokenApiId" should be "Published"
    When I retrieve the "apis" resource with id "infTokenApiId"
    And I extract response field "context" and store it as "infTokenApiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "infTokenAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "infTokenAppPayload"
    And I create an application with payload "infTokenAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "infTokenSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "infTokenApiId" using application "createdAppId" with payload "infTokenSubPayload" as "infTokenSubId"
    Then The response status code should be 201

    # A NEGATIVE application-token expiry is refused — exactly 400 with the 900970 envelope.
    When I put the following JSON payload in context as "negativeExpiryKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"], "additionalProperties": {"application_access_token_expiry_time": "-1"}}
    """
    And I generate client credentials for application id "createdAppId" with payload "negativeExpiryKeysPayload"
    Then The response status code should be 400
    And The error response should have code "900970" message "Invalid application additional properties" and description containing "cannot have negative values"

    # Long.MAX_VALUE is accepted for the SANDBOX key, and the token it issues invokes the gateway.
    When I put the following JSON payload in context as "infiniteSandboxKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"], "additionalProperties": {"application_access_token_expiry_time": "9223372036854775807"}}
    """
    And I generate client credentials for application id "createdAppId" with payload "infiniteSandboxKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "infiniteSandboxToken"
    When I invoke the API at gateway context "{{infTokenApiContext}}/1.0.0/customers/123/" with method "GET" using access token "infiniteSandboxToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # ...and likewise for the PRODUCTION key — legacy asserts both key types independently.
    When I put the following JSON payload in context as "infiniteProductionKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"], "additionalProperties": {"application_access_token_expiry_time": "9223372036854775807"}}
    """
    And I generate client credentials for application id "createdAppId" with payload "infiniteProductionKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "infiniteProductionToken"
    When I invoke the API at gateway context "{{infTokenApiContext}}/1.0.0/customers/123/" with method "GET" using access token "infiniteProductionToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the scope-FILTERING half of APIScopeTestCase#testSetScopeToResourceTestCase plus #testRESTAPIScopes.
  # Requesting the SAME full apim:* management scope set on ONE keyed application, as three principals of
  # different roles, proves the key manager grants only the scopes each user's roles map to (the tenant-conf
  # RESTAPIScopes role bindings). The two REST legs are the management-plane consequence legacy pins and v2
  # missed: a SUBSCRIBER token cannot list publisher APIs, and a CREATOR token cannot list devportal
  # applications — each with its positive control in the same scenario, so a blanket 401 cannot pass it.
  # Scope membership is compared WHOLE-ENTRY against the response's space-delimited list, not by substring
  # (see "The issued token scope list should include ... and exclude ...").
  @cap:key-manager @feat:scope-issuance @rule:role-filtering @type:regression @dep:publisher @dep:devportal @legacy:APIScopeTestCase
  Scenario Outline: Management scopes granted in a token are filtered by the requesting user's roles in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    # The block seeds subscriberUser (Internal/subscriber) and publisherUser (Internal/creator,publisher) but no
    # creator-WITHOUT-publisher user — the row that proves apim:api_publish needs the PUBLISHER role rather than
    # merely the creator one.
    And I provision user "roleScopeCreator" with roles "Internal/creator" in tenant "<tenant>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "roleScopeAppPayload"
    And I create an application with payload "roleScopeAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "roleScopeKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "roleScopeKeysPayload"
    Then The response status code should be 200

    # A SUBSCRIBER receives the consumer scopes and none of the publisher-plane ones.
    When I request an OAuth access token using password grant as "subscriberUser<suffix>" with scope "openid apim:api_view apim:api_create apim:api_publish apim:subscribe apim:app_update"
    Then The response status code should be 200
    And The issued token scope list should include "openid,apim:subscribe,apim:app_update" and exclude "apim:api_view,apim:api_create,apim:api_publish"

    # A CREATOR receives apim:api_create but NOT apim:api_publish, and no consumer scopes.
    When I request an OAuth access token using password grant as "roleScopeCreator<suffix>" with scope "openid apim:api_view apim:api_create apim:api_publish apim:subscribe apim:app_update"
    Then The response status code should be 200
    And The issued token scope list should include "openid,apim:api_view,apim:api_create" and exclude "apim:api_publish,apim:subscribe,apim:app_update"

    # A CREATOR+PUBLISHER receives both publisher-plane scopes, still no consumer scopes.
    When I request an OAuth access token using password grant as "publisherUser<suffix>" with scope "openid apim:api_view apim:api_create apim:api_publish apim:subscribe apim:app_update"
    Then The response status code should be 200
    And The issued token scope list should include "openid,apim:api_view,apim:api_create,apim:api_publish" and exclude "apim:subscribe,apim:app_update"

    # Publisher plane: a subscriber's token is unauthenticated for the API listing (401 — the management API's
    # answer for a token lacking the required scope, NOT 403); the creator+publisher's token is the control.
    Given The system is ready and I have valid publisher access tokens as "subscriberUser<suffix>"
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 401
    Given The system is ready and I have valid publisher access tokens as "publisherUser<suffix>"
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200

    # Devportal plane, symmetrically: a creator's token cannot list applications; a subscriber's can.
    Given The system is ready and I have valid devportal access token as "roleScopeCreator<suffix>"
    When I retrieve all applications from the Developer Portal
    Then The response status code should be 401
    Given The system is ready and I have valid devportal access token as "subscriberUser<suffix>"
    When I retrieve all applications from the Developer Portal
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:authcode @type:regression @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: Generate an access token via authorization code grant as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token via authorization code grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports GrantTypeTokenGenerateTestCase#testAuthCode's UNCOVERED legs: the authorize redirect's session cookies,
  # the consent page the product actually renders (testAuthCodeAppDisplayName), and using the authorization_code
  # token to invoke the API at the GATEWAY — the scenario above only proves the token was issued.
  #
  # The display name asserted is the SERVICE PROVIDER name the consent page renders in its <h4>, which the current
  # product builds as <owner>_<applicationUUID>_<keyType>. Legacy asserted the devportal application NAME appeared
  # there; that is no longer reachable — APIM names the OAuth service provider by application UUID, and it sets the
  # SP's display name to that same string (AMDefaultKeyManagerImpl passes OAuthApplicationInfo#getClientName into
  # ClientInfo#setApplicationDisplayName), so the [oauth] show_display_name_in_consent_page toggle legacy enabled
  # cannot surface a different value. Asserting the UUID form is therefore the exact, still-meaningful form of the
  # same check: the consent page names THIS application and no other. The expected SP owner prefix is explicit
  # per organization so both naming paths are pinned.
  @cap:key-manager @feat:token-issuance @rule:authcode-consent @type:regression @dep:gateway @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: The authorization code flow sets session cookies, names the application on the consent page, and yields a token that invokes the API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I request an OAuth access token via authorization code grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format
    # The authorize redirect must establish the per-authorization-request session nonce cookie. Legacy asserted
    # only that SOME Set-Cookie header came back; this names the cookie the OAuth flow actually depends on.
    # Pinned live: this response carries that cookie alone — it does NOT set a JSESSIONID.
    And The stored value "authorizeSetCookies" should contain "sessionNonceCookie-"
    # The consent page the user would have been shown names THIS application.
    And The stored value "consentApplicationName" should be "{{spOwnerName}}_{{createdAppId}}_PRODUCTION"
    And The stored value "consentPageBody" should contain "{{spOwnerName}}_{{createdAppId}}_PRODUCTION"
    # The authorization_code token is a real gateway credential, not just a well-formed JWT.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports GrantTypeTokenGenerateTestCase#testImplicit — v2 had NO implicit-grant coverage at all. The implicit
  # grant returns its token in the redirect FRAGMENT with no token-endpoint exchange, so the token itself is the
  # only evidence: it must be a JWT and it must work at the gateway.
  @cap:key-manager @feat:token-issuance @rule:implicit @type:regression @dep:gateway @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: Generate an access token via the implicit grant as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "implicitKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "implicit"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "implicitKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I request an OAuth access token via implicit grant with scope "PRODUCTION"
    Then The generated access token should be in JWT format
    And the actual value of "implicitTokenType" should match the expected value:
      """
      Bearer
      """
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports GrantTypeTokenGenerateTestCase#testAuthRequestWithoutCallbackURL. Legacy asserted only that the error
  # redirect CONTAINED the string "oauthErrorCode" — which passes for any OAuth error whatsoever. The product
  # distinguishes two cases here and both are pinned exactly: a MISSING redirect_uri is a protocol-level
  # invalid_request, while a PRESENT-but-unregistered one is invalid_callback.
  @cap:key-manager @feat:token-issuance @rule:redirect-uri @type:negative @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: An authorization request with a <case> redirect URI is refused with <errorCode> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200

    When I send an OAuth authorization request with redirect uri "<redirectUri>"
    Then the actual value of "authorizeErrorCode" should match the expected value:
      """
      <errorCode>
      """
    And the actual value of "authorizeErrorMessage" should match the expected value:
      """
      <errorMessage>
      """

    Examples:
      | actor             | case         | redirectUri                        | errorCode        | errorMessage                                          |
      | admin             | missing      |                                    | invalid_request  | Redirect URI is not present in the authorization request |
      | admin             | unregistered | http://localhost:8490/not-my-callback | invalid_callback | callback.not.match                                    |
      | admin@tenant1.com | missing      |                                    | invalid_request  | Redirect URI is not present in the authorization request |
      | admin@tenant1.com | unregistered | http://localhost:8490/not-my-callback | invalid_callback | callback.not.match                                    |

  # Ports GrantTypeTokenGenerateTestCase#testTokenGenerationWithCorruptedClientCredentials. Nothing in v2 asserted
  # the invalid_client error body or the WWW-Authenticate challenge anywhere; both are pinned here. The realm the
  # server advertises is its own hostname, which differs per deployment, so the challenge is asserted on the
  # scheme-and-realm prefix that is invariant — not on the host.
  @cap:key-manager @feat:token-issuance @rule:invalid-client @type:negative @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: A corrupted client credential is refused at the token endpoint as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200

    When I request an OAuth access token using corrupted client credentials
    Then The response status code should be 401
    And The response should contain "invalid_client"
    And The response should contain "Client credentials are invalid."
    And The response header "WWW-Authenticate" should contain "Basic realm="

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:authcode-default-scope @type:regression @legacy:JWTTestCase
  Scenario Outline: Auth code grant without scope issues token with 'default' scope as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token via authorization code grant without requesting any scopes
    Then The response status code should be 200
    And I extract response field "scope" and store it as "tokenScope"
    And the actual value of "tokenScope" should match the expected value:
      """
      default
      """

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:scope-in-token @type:regression @legacy:TokenEncryptionScopeTestCase
  Scenario Outline: A requested shared scope is granted in the issued token as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new shared scope as "encTokenScope"
    Then The response status code should be 201
    And I extract response field "name" and store it as "encTokenScopeName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "encApiCreate"
    And I create an "apis" resource with payload "encApiCreate" as "encApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"{{encTokenScopeName}}","displayName":"{{encTokenScopeName}}","description":"scope in token","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{encTokenScopeName}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "encRevPayload"
    """
    {"description":"scope revision"}
    """
    And I make a request to create a revision for "apis" resource "encApiId" with payload "encRevPayload"
    When I put the following JSON payload in context as "encDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "encApiId" with payload "encDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "encApiId"
    Then The lifecycle status of API "encApiId" should be "Published"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "encAppPayload"
    And I create an application with payload "encAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "encKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "encKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "encSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "encApiId" using application "createdAppId" with payload "encSubPayload" as "encSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "{{encTokenScopeName}}"
    Then The response status code should be 200
    And The response should contain "{{encTokenScopeName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An UNREGISTERED scope is granted verbatim rather than dropped or replaced by "default". Pinned because ~20
  # scenarios across the gateway and key-manager suites request "PRODUCTION"/"SANDBOX" — neither of which is a
  # registered scope — and depend on getting a usable token back. If scope validation ever tightened, this names
  # the cause instead of leaving those to fail as downstream 401/403s. Completes the granted-scope trio alongside
  # authcode-default-scope (none requested -> "default") and scope-in-token (a registered scope).
  @cap:key-manager @feat:token-issuance @rule:unregistered-scope @type:regression
  Scenario Outline: A scope not registered on the application is still granted in the issued token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "unregScopeAppPayload"
    And I create an application with payload "unregScopeAppPayload"
    Then The response status code should be 201
    # No "scopes" key in this payload: the application registers NO scopes at all.
    When I put the following JSON payload in context as "unregScopeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "unregScopeKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    # Exact, not containment: the granted scope must be exactly what was requested — no silent substitution to
    # "default" and nothing extra appended.
    And I extract response field "scope" and store it as "unregTokenScope"
    And the actual value of "unregTokenScope" should match the expected value:
      """
      PRODUCTION
      """

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports OpenIDTokenAPITestCase's ACTUAL arc: legacy created its application with tokenType OAUTH (opaque) and
  # presented THAT token at userinfo. The openid scenario above uses the JWT-default app, so the opaque-token-at-
  # userinfo path was untested for every principal (admin included). Discriminator: the fixture's tokenType OAUTH.
  # There is no existing step to assert "not in JWT format", so that assertion is intentionally omitted (see report).
  @cap:key-manager @feat:token-issuance @type:regression @rule:openid @legacy:OpenIDTokenAPITestCase
  Scenario Outline: Call userinfo with an opaque OpenID-scoped token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
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
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports TokenAPITestCase's sandbox-key arc on an OPAQUE application. Every SANDBOX key in the tree today sits on a
  # default/JWT app; this pins the same sandbox-token-invoke on an app whose tokenType is OAUTH (opaque).
  # Discriminator: the fixture's tokenType OAUTH — the invoke itself is identical to the JWT sandbox case above.
  @cap:key-manager @feat:token-issuance @type:regression @rule:sandbox @dep:gateway @legacy:TokenAPITestCase
  Scenario Outline: Issue a sandbox-scoped token on an opaque application and invoke as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateSandboxApplicationKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateSandboxApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "SANDBOX"
    Then The response status code should be 200
    # The SANDBOX-key token is the credential under test; pin the backend payload so the 200 is evidence the token
    # was honoured all the way to the upstream (same body string the JWT sandbox scenario above asserts).
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

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
