@cleanup
Feature: Gateway Credential Enforcement

  Gateway-plane enforcement of which credential a request may present, in both the super tenant and tenant1.com:
  a valid credential in another credential kind's header is refused, an API accepts only the security schemes it
  enables (an api-key API answers an unauthenticated call with a WWW-Authenticate challenge), an api key is
  refused once its subscription is removed, a password reset invalidates already-issued tokens and Basic
  credentials, a roleless user authenticates, and a role-bound operation scope is granted only to a user carrying
  the role. Teardown via the per-scenario cleanup hook.

  # Cross-credential confusion: a credential that is perfectly VALID of its own kind must not be accepted when it
  # is presented in the header belonging to a DIFFERENT kind. Ports the six cross-credential negatives of
  # APISecurityTestCase (testInvokeApiKeyAsJWTNegative, testInvokeJWTAsAPIKeyNegative,
  # testInvokeInternalKeyAsAPIKeyNegative, testInvokeInternalKeyAsJWTNegative, testInvokeJWTasInternalKeyNegative,
  # testInvokeAPIKeyAsInternalKeyNegative) against an API that permits BOTH oauth2 and api_key — so every
  # credential here is one the API would accept in its own header. The two POSITIVE CONTROLS come first and are
  # what make the refusals meaningful: they prove the token and the api key are genuinely valid on this API, so
  # each later 401 is about the credential KIND, not a bad credential. Each case pins its OWN exact status (they
  # are not assumed uniform — see CLAUDE.md §12 on per-subtype differences).
  @cap:gateway @feat:security-enforcement @type:negative @rule:cross-credential @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A valid credential presented in another credential kind's header is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_apikey_api.json" as "xcApiId" and deployed it
    When I publish the "apis" resource with id "xcApiId"
    Then The lifecycle status of API "xcApiId" should be "Published"
    When I retrieve the "apis" resource with id "xcApiId"
    And I extract response field "context" and store it as "xcContext"
    When I have set up application with keys, subscribed to API "xcApiId", and obtained access token for "xcSubId"
    Then The response status code should be 200
    And the "apis" resource "xcApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "xcApiKeyGenPayload"
    """
    {"keyName": "CrossCredentialKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "xcApiKeyGenPayload"
    Then The response status code should be 200
    When I generate an internal API key for API "xcApiId" and store it as "xcInternalKey"
    Then The response status code should be 200

    # POSITIVE CONTROL 1: the OAuth2 token IS valid on this API in its own header. The control only does its job —
    # making each refusal below attributable to the credential KIND — if it shows the credential reached the
    # BACKEND, so the payload is pinned, not just the status.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    # POSITIVE CONTROL 2: the api key IS valid on this API in its own header.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # The OAuth2 access token in the api-key header -> refused.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" presenting credential "generatedAccessToken" verbatim in header "apikey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # The publisher internal key in the api-key header -> refused.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" presenting credential "xcInternalKey" verbatim in header "apikey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # The devportal api key as an Authorization bearer -> refused (testInvokeApiKeyAsJWTNegative). Uses the same
    # bearer step as the token control above, so the ONLY difference from that passing case is the credential kind.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" using access token "apiKey" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # The publisher internal key as an Authorization bearer -> refused.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" using access token "xcInternalKey" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # The OAuth2 access token in the Internal-Key header -> refused.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" presenting credential "generatedAccessToken" verbatim in header "Internal-Key" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # The devportal api key in the Internal-Key header -> refused.
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" presenting credential "apiKey" verbatim in header "Internal-Key" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # A locally signed JWT-format api key: valid in the api-key header (control), refused in the Internal-Key header
    # (the JWT half of testInvokeAPIKeyAsInternalKeyNegative).
    When I generate locally signed JWT API key of type "PRODUCTION" for application id "createdAppId" with permitted IP "" and permitted referer "" as "xcJwtApiKey"
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" using api key "xcJwtApiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{xcContext}}/1.0.0/customers/123/" with method "GET" presenting credential "xcJwtApiKey" verbatim in header "Internal-Key" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A password-grant USER token presented in the api-key header is refused. Ports testInvokeJWTAsAPIKeyNegative,
  # whose credential is a resource-owner token rather than the application token the cross-credential scenario
  # above uses. The application's keys include the password grant, so it is built inline. Both controls come
  # first: the same user token works as a bearer, and a real api key works in the api-key header.
  @cap:gateway @feat:security-enforcement @type:negative @rule:cross-credential @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A password-grant user token presented in the api-key header is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_apikey_api.json" as "utApiId" and deployed it
    When I publish the "apis" resource with id "utApiId"
    Then The lifecycle status of API "utApiId" should be "Published"
    When I retrieve the "apis" resource with id "utApiId"
    And I extract response field "context" and store it as "utContext"
    And the "apis" resource "utApiId" should be live on the gateway, redeploying if propagation is lost
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "utAppPayload"
    And I create an application with payload "utAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "utKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "utKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "utSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "utApiId" using application "createdAppId" with payload "utSubPayload" as "utSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "utApiKeyGenPayload"
    """
    {"keyName": "UserTokenCtlKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "utApiKeyGenPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope ""
    Then The response status code should be 200

    # CONTROL 1: the user token is valid on this API as a bearer.
    When I invoke the API at gateway context "{{utContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    # CONTROL 2: the api-key header is honoured on this API.
    When I invoke the API at gateway context "{{utContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    # The user token in the api-key header -> refused.
    When I invoke the API at gateway context "{{utContext}}/1.0.0/customers/123/" with method "GET" presenting credential "generatedAccessToken" verbatim in header "apikey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The HTTP Basic SCHEME is refused where the API does not permit it. This replaces the hollow legacy
  # testInvocationWithBasicAuthForOauthOnlyAPINegative, which was vacuous for TWO independent reasons: it sent
  # "Authorization: Basic abcce" (not valid base64 of any user:password, so a 401 only proves "a garbage header is
  # refused"), AND it targeted an API with mutual SSL AND OAuth mandatory while presenting NO client certificate,
  # so the refusal could not be distinguished from a missing-certificate rejection. Here the target is an
  # OAUTH2-ONLY API (no mTLS in the mix) and the credential is a WELL-FORMED base64 of a REAL existing user's
  # username:password — a credential the gateway CAN decode and authenticate, and which succeeds on a
  # basic_auth-permitting API (see the basic-auth scenario above). So the refusal below is genuinely about the
  # SCHEME not being permitted. The OAuth positive control first proves the API itself is routable.
  #
  # OBSERVED, and stronger than expected: the refusal is 401 with code 900902 "Missing Credentials" - NOT 900901
  # "Invalid Credentials". The gateway does not merely reject the Basic credential, it does not treat the header as
  # a candidate credential AT ALL for an API that does not permit basic_auth. That is the sharpest possible form of
  # scheme enforcement, and it is exactly what legacy's "Basic abcce" could never have shown: a malformed Basic
  # header on a basic_auth-PERMITTING API is 900901 (the wrong-password case above), so legacy's bare 401 was
  # equally consistent with the Basic scheme being fully accepted. The code is pinned per case, empirically: the
  # cross-credential scenario above answers 900901 on the very same status, so a status-only assertion would
  # conflate two different product behaviours.
  @cap:gateway @feat:security-enforcement @type:negative @rule:basic-auth @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A well-formed Basic credential is refused on an oauth2-only API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth2_only_api.json" as "boApiId" and deployed it
    When I publish the "apis" resource with id "boApiId"
    Then The lifecycle status of API "boApiId" should be "Published"
    When I retrieve the "apis" resource with id "boApiId"
    And I extract response field "context" and store it as "boContext"
    When I have set up application with keys, subscribed to API "boApiId", and obtained access token for "boSubId"
    Then The response status code should be 200
    And the "apis" resource "boApiId" should be live on the gateway, redeploying if propagation is lost

    # POSITIVE CONTROL: the API is routable and its permitted scheme (oauth2) works, all the way to the backend.
    When I invoke the API at gateway context "{{boContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # A WELL-FORMED Basic credential for a real user, on an API that does not permit basic_auth -> refused (401).
    When I invoke the API at gateway context "{{boContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900902" message "Missing Credentials" and description containing "Make sure your API invocation call has a header"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Which credential kinds a basic_auth-ONLY API honours at the gateway. Ports testInvokeBearerTokenForBasicNegative
  # (a VALID OAuth bearer -> 401), testInvokeAPIKeyForBasicOauthAPINegative (a VALID api key -> 401) and
  # testInvokeInternalKeyForBasicAuthOnlyAPI (the publisher internal key -> 200: the internal key deliberately
  # BYPASSES the declared application-security scheme, which is the try-out path). The Basic positive control comes
  # first so the two refusals cannot be confused with an unroutable API. The bearer token and the api key are both
  # minted against a real subscription, so they are valid credentials refused purely on scheme grounds.
  # OBSERVED: both refusals are 401 with code 900902 "Missing Credentials" - on a basic_auth-only API neither an
  # Authorization: Bearer header nor an ApiKey header is even a candidate credential, so nothing is "invalid": there
  # is simply no Basic credential present. Contrast the cross-credential scenario above, where the same 401 carries
  # 900901 because the header IS one the API's authenticators read. Pinned per case, never widened.
  @cap:gateway @feat:security-enforcement @rule:scheme-enforcement @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A basic_auth-only API accepts Basic and the internal key but refuses bearer and api-key credentials as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_basicauth_only_api.json" as "baApiId" and deployed it
    When I publish the "apis" resource with id "baApiId"
    Then The lifecycle status of API "baApiId" should be "Published"
    When I retrieve the "apis" resource with id "baApiId"
    And I extract response field "context" and store it as "baContext"
    When I have set up application with keys, subscribed to API "baApiId", and obtained access token for "baSubId"
    Then The response status code should be 200
    And the "apis" resource "baApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "baApiKeyGenPayload"
    """
    {"keyName": "BasicOnlyApiKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "baApiKeyGenPayload"
    Then The response status code should be 200
    When I generate an internal API key for API "baApiId" and store it as "baInternalKey"
    Then The response status code should be 200

    # POSITIVE CONTROL: the permitted scheme (basic_auth) authorises a valid carbon user through to the backend.
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # A VALID OAuth2 bearer token on a basic_auth-only API -> refused (401).
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900902" message "Missing Credentials" and description containing "Make sure your API invocation call has a header"
    # A VALID api key on a basic_auth-only API -> refused (401).
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900902" message "Missing Credentials" and description containing "Make sure your API invocation call has a header"
    # The publisher internal key BYPASSES the declared scheme -> 200 from the backend. The payload matters here
    # more than anywhere else in this scenario: the claim is that a credential the API's declared scheme does NOT
    # permit still reaches the upstream, and only the backend body shows that it did.
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using internal key "baInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # A locally signed JWT-format api key on the basic_auth-only API -> refused (401). The key is first proven valid
    # on an api_key-only API the same application is subscribed to, so the refusal is about the scheme.
    And I have created an api from "artifacts/payloads/create_apim_apikey_only_api.json" as "baKeyApiId" and deployed it
    When I publish the "apis" resource with id "baKeyApiId"
    Then The lifecycle status of API "baKeyApiId" should be "Published"
    When I retrieve the "apis" resource with id "baKeyApiId"
    And I extract response field "context" and store it as "baKeyContext"
    And the "apis" resource "baKeyApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "baKeySubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "baKeyApiId" using application "createdAppId" with payload "baKeySubPayload" as "baKeySubId"
    Then The response status code should be 201
    When I generate locally signed JWT API key of type "PRODUCTION" for application id "createdAppId" with permitted IP "" and permitted referer "" as "baJwtApiKey"
    When I invoke the API at gateway context "{{baKeyContext}}/1.0.0/customers/123/" with method "GET" using api key "baJwtApiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using api key "baJwtApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900902" message "Missing Credentials" and description containing "Make sure your API invocation call has a header"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The WWW-Authenticate challenge on an api-key-enabled API. Ports testWWWAuthorizationHeaderForApiWithApiKeys:
  # invoking an API whose securityScheme includes api_key with NO Authorization header must answer with a
  # WWW-Authenticate response header advertising the API Key realm. This is the first v2 assertion on a RESPONSE
  # HEADER of a gateway auth rejection. Legacy's second case sends an Authorization header with an empty value; the
  # empty-header step checks on a loopback socket that the header really goes out empty before calling the gateway.
  @cap:gateway @feat:security-enforcement @type:negative @rule:auth-challenge @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An api-key-enabled API answers an unauthenticated call with a WWW-Authenticate challenge as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_apikey_api.json" as "wwApiId" and deployed it
    When I publish the "apis" resource with id "wwApiId"
    Then The lifecycle status of API "wwApiId" should be "Published"
    When I retrieve the "apis" resource with id "wwApiId"
    And I extract response field "context" and store it as "wwContext"

    And the "apis" resource "wwApiId" should be live on the gateway, redeploying if propagation is lost
    When I invoke the API at gateway context "{{wwContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response header "WWW-Authenticate" should contain "API Key realm=\"WSO2 API Manager\""
    # An Authorization header that is present but EMPTY still earns the same challenge (legacy's second case).
    When I invoke the API at gateway context "{{wwContext}}/1.0.0/customers/123/" with method GET and an empty Authorization header until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response header "WWW-Authenticate" should contain "API Key realm=\"WSO2 API Manager\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Subscription REMOVAL (as opposed to never having subscribed) is enforced on an api-key credential. Ports
  # testInvocationWithApiKeysWithoutSubscription: an api key generated on a SUBSCRIBED application invokes the
  # api_key-secured API (200); once the subscription is DELETED the same key is refused (403). The never-subscribed
  # OAuth case is covered by the no-subscription scenario above; this one proves the gateway also drops an
  # already-valid api key when its subscription goes away (a JMS-propagated invalidation, hence the retry window).
  @cap:gateway @feat:security-enforcement @type:negative @rule:no-subscription @dep:publisher @dep:devportal @legacy:APISecurityTestCase
  Scenario Outline: An api key is refused after its application's subscription is removed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_apikey_only_api.json" as "srApiId" and deployed it
    When I publish the "apis" resource with id "srApiId"
    Then The lifecycle status of API "srApiId" should be "Published"
    When I retrieve the "apis" resource with id "srApiId"
    And I extract response field "context" and store it as "srContext"
    When I have set up application with keys, subscribed to API "srApiId", and obtained access token for "srSubId"
    Then The response status code should be 200
    And the "apis" resource "srApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "srApiKeyGenPayload"
    """
    {"keyName": "SubRemovalKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "srApiKeyGenPayload"
    Then The response status code should be 200

    # Subscribed -> the api key invokes successfully.
    When I invoke the API at gateway context "{{srContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # A locally signed JWT-format api key issued while subscribed also invokes successfully (legacy used this form).
    When I generate locally signed JWT API key of type "PRODUCTION" for application id "createdAppId" with permitted IP "" and permitted referer "" as "srJwtApiKey"
    When I invoke the API at gateway context "{{srContext}}/1.0.0/customers/123/" with method "GET" using api key "srJwtApiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # Remove the subscription -> the SAME api key is refused (403).
    When I delete the subscription with id "srSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{srContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # The JWT-format key issued before the removal is refused the same way (403).
    When I invoke the API at gateway context "{{srContext}}/1.0.0/customers/123/" with method "GET" using api key "srJwtApiKey" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # A key minted AFTER the removal: generation still succeeds (200), but the key is refused (403). An unpropagated
    # opaque key answers 401, so the 403 poll cannot be satisfied before the key reaches the gateway.
    When I put the following JSON payload in context as "srLateKeyGenPayload"
    """
    {"keyName": "SubRemovedLateKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "srLateKeyGenPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{srContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An admin-side PASSWORD RESET invalidates the credentials already issued to that user, enforced at the gateway.
  # Ports APISecurityTestCase#testInvokeJWTUserToken (a password-grant USER token invokes with 200; after the admin
  # changes that user's password the SAME already-issued token returns 401) and #testInvokeBasicAuthAfterCredentialsInvalid
  # (Basic with the OLD password is then 401 while Basic with the NEW password is 200). The two are one arc on one
  # user and are asserted together: the Basic legs are what prove the reset actually took effect in the user store,
  # so the token rejection is credential invalidation rather than an unrelated token failure.
  #
  # The user is provisioned BY THIS SCENARIO under a generated name and swept afterwards. It cannot be one of the
  # block-seeded actors: changing a shared actor's password would invalidate the tokens of every scenario running in
  # parallel as that actor. The API permits both oauth2 and basic_auth so one fixture carries both credential kinds.
  # The application's keys must include the password grant, so the app is built inline rather than via the
  # client-credentials-only composite.
  @cap:gateway @feat:security-enforcement @rule:password-change @type:regression @dep:publisher @dep:admin @legacy:APISecurityTestCase
  Scenario Outline: A password reset invalidates the user's already-issued token and old Basic credential as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "pcApiId" and deployed it
    When I publish the "apis" resource with id "pcApiId"
    Then The lifecycle status of API "pcApiId" should be "Published"
    When I retrieve the "apis" resource with id "pcApiId"
    And I extract response field "context" and store it as "pcContext"

    And the "apis" resource "pcApiId" should be live on the gateway, redeploying if propagation is lost
    # A scenario-owned user whose password this scenario is free to change.
    When I provision a user with name prefix "pcUser" password "Password@123" and roles "Internal/subscriber" storing the username as "pcUsername"

    # An application subscribed to the API, with keys that support the PASSWORD grant.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "pcAppPayload"
    And I create an application with payload "pcAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pcKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "pcKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "pcSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "pcApiId" using application "createdAppId" with payload "pcSubPayload" as "pcSubId"
    Then The response status code should be 201

    # A password-grant token FOR THAT USER invokes the API (200) — the token is issued and works.
    # No explicit scope on purpose: what is pinned below is credential-change revocation, which is
    # scope-independent. The "PRODUCTION" scope on this file's other password grants is a key-type marker on the
    # acting-actor step, not a scope under test.
    When I request an OAuth access token using password grant as user "{{pcUsernameLoginName}}" with password "Password@123"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{pcContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The user's Basic credential also works (the second credential kind, before the reset).
    When I invoke the API at gateway context "{{pcContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{pcUsernameLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The admin resets that user's password.
    When I change the password of user "pcUsername" to "Changed@456" as the tenant admin

    # The SAME already-issued access token is now rejected at the gateway (401) — the credential change invalidated
    # it. This is the assertion the whole arc exists for; it is NOT a re-run of the token grant.
    When I invoke the API at gateway context "{{pcContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # Basic with the OLD password is refused (401) and Basic with the NEW password is accepted (200) — proof the
    # reset landed in the user store, so the token rejection above is credential invalidation.
    When I invoke the API at gateway context "{{pcContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{pcUsernameLoginName}}" password "Password@123" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    When I invoke the API at gateway context "{{pcContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{pcUsernameLoginName}}" password "Changed@456" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

  Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The reset scenario above uses a subscriber-role user. This separate scenario composes the legacy roleless-user
  # fixture with the same reset/revocation contract: password grant and Basic work before reset; the already-issued
  # token and old Basic credential are rejected afterwards, while the new Basic credential works. Keeping a
  # scenario-owned user avoids mutating shared actors used by parallel scenarios.
  @cap:gateway @feat:security-enforcement @rule:password-change @type:regression @dep:publisher @dep:admin @legacy:APISecurityTestCase
  Scenario Outline: Resetting a roleless user's password invalidates existing credentials as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "rrApiId" and deployed it
    When I publish the "apis" resource with id "rrApiId"
    Then The lifecycle status of API "rrApiId" should be "Published"
    When I retrieve the "apis" resource with id "rrApiId"
    And I extract response field "context" and store it as "rrContext"
    And the "apis" resource "rrApiId" should be live on the gateway, redeploying if propagation is lost

    When I provision a user with no roles with name prefix "rrUser" password "Password@123" storing the username as "rrUsername"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "rrAppPayload"
    And I create an application with payload "rrAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "rrKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "rrKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "rrSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "rrApiId" using application "createdAppId" with payload "rrSubPayload" as "rrSubId"
    Then The response status code should be 201

    # Prove the roleless identity's password-grant token and Basic credential both work before reset.
    When I request an OAuth access token using password grant as user "{{rrUsernameLoginName}}" with password "Password@123"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{rrContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{rrContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{rrUsernameLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    When I change the password of user "rrUsername" to "Changed@456" as the tenant admin
    # The same token issued before reset must now fail, proving roleless-user credential invalidation.
    When I invoke the API at gateway context "{{rrContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    When I invoke the API at gateway context "{{rrContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{rrUsernameLoginName}}" password "Password@123" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    When I invoke the API at gateway context "{{rrContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{rrUsernameLoginName}}" password "Changed@456" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Principals with NO role at all authenticate at the gateway. Ports the role-less users of APISecurityTestCase
  # (created with an empty role list): testInvokeBasicAuth (Basic -> 200 for a plain and an email-form name) and
  # testInvokeJWTUserToken (that user's password-grant token -> 200). The users are provisioned without any
  # roleList, so they carry only the implicit Internal/everyone. A wrong password for the same user is refused, so
  # the 200s are attributable to the credential.
  @cap:gateway @feat:security-enforcement @rule:basic-auth @type:regression @dep:publisher @dep:admin @legacy:APISecurityTestCase
  Scenario Outline: A user with no roles authenticates with Basic and with a password-grant token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "rlApiId" and deployed it
    When I publish the "apis" resource with id "rlApiId"
    Then The lifecycle status of API "rlApiId" should be "Published"
    When I retrieve the "apis" resource with id "rlApiId"
    And I extract response field "context" and store it as "rlContext"
    And the "apis" resource "rlApiId" should be live on the gateway, redeploying if propagation is lost
    When I provision a user with no roles with name prefix "rlUser" password "Password@123" storing the username as "rlUsername"
    And I provision a user with no roles with name prefix "rlMail" and email domain "wso2.com" password "Password@123" storing the username as "rlMailUsername"

    When I invoke the API at gateway context "{{rlContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{rlUsernameLoginName}}" password "Wrong@999" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{rlContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{rlUsernameLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{rlContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{rlMailUsernameLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "rlAppPayload"
    And I create an application with payload "rlAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "rlKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "rlKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "rlSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "rlApiId" using application "createdAppId" with payload "rlSubPayload" as "rlSubId"
    Then The response status code should be 201
    When I request an OAuth access token using password grant as user "{{rlUsernameLoginName}}" with password "Password@123"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{rlContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the role-bound-scope ENFORCEMENT half of APIScopeTestCase#testSetScopeToResourceTestCase, which v2 had
  # only in its publisher-plane form (a scope can be attached to an operation) with no runtime consequence and no
  # second principal. Here ONE scope bound to ONE scenario-unique role is requested by TWO users of DIFFERENT
  # roles using the SAME application credentials and the SAME gated operation: the user carrying the role is
  # granted the scope and gets 200; the user without it receives a token whose scope list is EXACTLY "default"
  # and is refused with 403. Because every other variable is shared between the two legs, the 403 can only be
  # caused by the role -> scope binding. The role, the scope and both users are scenario-unique (roles and scopes
  # are tenant-global).
  @cap:gateway @feat:security-enforcement @rule:role-bound-scope @type:regression @dep:publisher @legacy:APIScopeTestCase
  Scenario Outline: A role-bound operation scope is granted to one user and withheld from another in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "rbsRole"
    And I generate a unique value and store it as "rbsScope"
    And I provision role "{{rbsRole}}" in tenant "<tenant>"
    And I provision user "rbsWithRole" with roles "Internal/subscriber,{{rbsRole}}" in tenant "<tenant>"
    And I provision user "rbsWithoutRole" with roles "Internal/subscriber" in tenant "<tenant>"

    # An API whose GET /customers/{id} operation requires the role-bound scope.
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "rbsApiId" and deployed it
    When I retrieve the "apis" resource with id "rbsApiId"
    And I put the response payload in context as "rbsApiPayload"
    When I update the "apis" resource "rbsApiId" and "rbsApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":false,"scope":{"name":"{{rbsScope}}","displayName":"{{rbsScope}}","description":"role-bound operation scope","bindings":["{{rbsRole}}"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "rbsApiId"
    And I put the response payload in context as "rbsApiPayload"
    When I update the "apis" resource "rbsApiId" and "rbsApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{rbsScope}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I deploy the API with id "rbsApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "rbsApiId"
    Then The lifecycle status of API "rbsApiId" should be "Published"
    When I retrieve the "apis" resource with id "rbsApiId"
    And I extract response field "context" and store it as "rbsApiContext"

    # One subscribed, keyed application — both users' tokens are minted with ITS credentials.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "rbsAppPayload"
    And I create an application with payload "rbsAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "rbsKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "rbsKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "rbsSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "rbsApiId" using application "createdAppId" with payload "rbsSubPayload" as "rbsSubId"
    Then The response status code should be 201

    # GRANTED — the user carrying the bound role receives the scope and invokes the gated operation.
    When I request an OAuth access token using password grant as "rbsWithRole<suffix>" with scope "{{rbsScope}}"
    Then The response status code should be 200
    And The issued token scope list should include "{{rbsScope}}" and exclude ""
    And I invoke the API at gateway context "{{rbsApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # WITHHELD — the user without the role gets EXACTLY the default scope and is refused at the gateway.
    When I request an OAuth access token using password grant as "rbsWithoutRole<suffix>" with scope "{{rbsScope}}"
    Then The response status code should be 200
    And I extract response field "scope" and store it as "rbsWithheldScope"
    And the actual value of "rbsWithheldScope" should match the expected value:
      """
      default
      """
    And I invoke the API at gateway context "{{rbsApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
