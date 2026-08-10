@cleanup
Feature: Key Manager Token Issuance

  Key-manager-plane token issuance across grant/scope variants: JWT-format production tokens, OpenID-scoped
  tokens (+ userinfo), refresh-token re-issuance, and sandbox-key tokens. Runs as admin in both the super
  tenant and tenant1.com. The refresh and sandbox variants invoke the gateway to prove the issued token works
  end-to-end. Teardown via the per-scenario cleanup hook.

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
    And The response should contain "\"name\":\"John\""

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
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

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
    And The stored value "consentApplicationName" should be "<spOwner>_{{createdAppId}}_PRODUCTION"
    And The stored value "consentPageBody" should contain "<spOwner>_{{createdAppId}}_PRODUCTION"
    # The authorization_code token is a real gateway credential, not just a well-formed JWT.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "John"

    Examples:
      | actor             | spOwner              |
      | admin             | admin                |
      | admin@tenant1.com | admin                |

  # Ports GrantTypeTokenGenerateTestCase#testImplicit — v2 had NO implicit-grant coverage at all. The implicit
  # grant returns its token in the redirect FRAGMENT with no token-endpoint exchange, so the token itself is the
  # only evidence: it must be a JWT and it must work at the gateway.
  @cap:key-manager @feat:token-issuance @rule:implicit @type:regression @dep:gateway @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: Generate an access token via the implicit grant as <actor>
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
    And The response should contain "John"

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
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "encApiCreate"
    And I create an "apis" resource with payload "encApiCreate" as "encApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"encTokenScope","displayName":"encTokenScope","description":"scope in token","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["encTokenScope"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
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
    When I request an OAuth access token for the current user using password grant with scope "encTokenScope"
    Then The response status code should be 200
    And The response should contain "encTokenScope"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
