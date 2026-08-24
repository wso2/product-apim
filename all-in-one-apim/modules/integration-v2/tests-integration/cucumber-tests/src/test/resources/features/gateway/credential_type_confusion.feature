Feature: Gateway Credential-Type Confusion

  The cross-credential negatives of APISecurityTestCase. The gateway accepts three DIFFERENT kinds of credential,
  each in its own transport position — an OAuth2 access token in "Authorization: Bearer", a DevPortal application
  API key in the "ApiKey" header, and a publisher internal API key in the "Internal-Key" header — and it must not
  accept any of them in another's position. Every credential presented below is a REAL, LIVE credential that
  works in its OWN header (proven by the positive controls in the first scenario), which is what makes these
  refusals evidence of TYPE DISCRIMINATION rather than of nonsense-rejection: the corpus already covers a garbage
  string in the apikey header (key-manager/api_key.feature) and a garbage bearer token
  (gateway/security_enforcement.feature), and neither proves a credential of the wrong KIND is refused.

  Each case asserts its own EXACT status — never a widened "4xx". All of them are 401 (authentication failed at
  the header the credential was presented in), NOT 403: the request never reaches the subscription/scope checks
  because the authenticator owning that header rejects the credential outright.

  The two APIs, the application subscribed to both, and the three credentials come from
  _setup_credential_type_confusion (listed first in the runner). Teardown is the runner's AfterClass sweep, so
  this feature is deliberately NOT tagged @cleanup — a per-scenario sweep would delete the shared fixture out
  from under the scenarios that follow.

  Runs x2 tenants so each tenant independently proves which credential TYPE each auth header accepts.

  # Ports testInvokeApiKeyAsJWTNegative, testInvokeJWTAsAPIKeyNegative, testInvokeInternalKeyAsAPIKeyNegative,
  # testInvokeInternalKeyAsJWTNegative, testInvokeJWTasInternalKeyNegative and testInvokeAPIKeyAsInternalKeyNegative
  # — the full 6-cell matrix, in one scenario because it is ONE property (each header accepts only its own
  # credential kind) and the cells share one fixture. The three positive controls come FIRST and are load-bearing:
  # without them a broken/expired credential would make every negative pass for the wrong reason.
  @cap:gateway @feat:security-enforcement @rule:credential-type-confusion @type:negative @dep:publisher @dep:key-manager @legacy:APISecurityTestCase
  Scenario Outline: Each credential kind is accepted only in its own auth header as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"

    # --- POSITIVE CONTROLS: all three credentials are live and each works in its OWN header.
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using access token "ctcAccessToken<suffix>" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using api key "ctcApiKey<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using internal key "ctcInternalKey<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # --- The "ApiKey" header accepts ONLY an API key.
    # An OAuth2 access token in the apikey header (testInvokeApiKeyAsJWTNegative / testInvokeJWTAsAPIKeyNegative).
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using api key "ctcAccessToken<suffix>" in header "ApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # An internal API key in the apikey header (testInvokeInternalKeyAsAPIKeyNegative).
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using api key "ctcInternalKey<suffix>" in header "ApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # --- "Authorization: Bearer" accepts ONLY an OAuth2 access token.
    # An API key presented as a bearer token. NOTE: this cell has NO legacy equivalent (the legacy class never
    # presented an API key in the Authorization header) — it is the symmetric completion of the matrix.
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using access token "ctcApiKey<suffix>" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # An internal API key presented as a bearer token (testInvokeInternalKeyAsJWTNegative).
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using access token "ctcInternalKey<suffix>" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # --- The "Internal-Key" header accepts ONLY an internal API key.
    # An OAuth2 access token in the Internal-Key header (testInvokeJWTasInternalKeyNegative).
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using internal key "ctcAccessToken<suffix>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # An API key in the Internal-Key header (testInvokeAPIKeyAsInternalKeyNegative).
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using internal key "ctcApiKey<suffix>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testInvocationWithBasicAuthForOauthOnlyAPINegative. The legacy sent a literal "Authorization: Basic
  # abcce" (not even valid base64), which is indistinguishable from the garbage-credential negative already in
  # the corpus. This sends the VALID carbon credentials of a real user — the very credentials a basic_auth API
  # accepts (proven in gateway/basic_auth_security.feature) — so the 401 is attributable to the API not
  # declaring basic_auth, not to the credentials being bad.
  @cap:gateway @feat:security-enforcement @rule:scheme-mismatch @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: Valid HTTP Basic credentials are refused on an oauth2 + api_key API that does not declare basic_auth as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testInvocationWithBasicAuthFoAPIKeyNegative — the same scheme mismatch against an API whose ONLY
  # declared application-security scheme is api_key. The api-key positive control first proves the API is live
  # and routable, so the following 401 is the scheme gate and not a dead route.
  @cap:gateway @feat:security-enforcement @rule:scheme-mismatch @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: Valid HTTP Basic credentials are refused on an api_key-only API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{ctcKeyOnlyContext<suffix>}}/1.0.0/customers/123/" with method "GET" using api key "ctcApiKey<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{ctcKeyOnlyContext<suffix>}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testWWWAuthorizationHeaderForApiWithApiKeys: an UNAUTHENTICATED request to an API with api_key enabled
  # must advertise the API-key challenge in the WWW-Authenticate response header. The legacy asserts the header
  # CONTAINS 'API Key realm="WSO2 API Manager"' (it also carries the OAuth2 challenge, since this API declares
  # oauth2 too), so this uses the substring header step rather than the exact-value one — the exact-value step
  # cannot express "contains" and pinning the whole header would pin the challenge ORDER as well.
  #
  # The legacy's "second leg" (a repeat request with requestHeaders2.put("Authorization", null)) is deliberately
  # NOT ported: a null HashMap value is not a header at all, so that leg re-sends the identical request as the
  # first and asserts nothing new.
  @cap:gateway @feat:security-enforcement @rule:www-authenticate @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An unauthenticated request to an api_key-enabled API advertises the API Key realm as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{ctcContext<suffix>}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response header "WWW-Authenticate" should contain "API Key realm=\"WSO2 API Manager\""

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
