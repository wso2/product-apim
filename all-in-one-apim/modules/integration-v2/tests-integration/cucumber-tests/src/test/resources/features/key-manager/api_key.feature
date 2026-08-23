@cleanup
Feature: Key Manager API Key

  Key-manager-plane API-key issuance and use: enable the api_key security scheme on an API, generate an API
  key for a subscribed application, and invoke the API through the gateway using that key. Runs as admin in
  both the super tenant and tenant1.com. Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:api-key @type:smoke @dep:gateway @legacy:APIKeyInvocationTestCase
  Scenario Outline: Generate an API key and invoke a published API with it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"

    # Enable the api_key security scheme
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response field "securityScheme[*]" should be exactly the list "api_key,oauth_basic_auth_api_key_mandatory,oauth2"
    And I extract response field "context" and store it as "apiContext"

    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Subscribe an application and generate an API key
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I put the following JSON payload in context as "apiKeyGenerationPayload"
    """
    {"keyName": "TestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "apiKeyGenerationPayload"
    Then The response status code should be 200

    # Invoke through the gateway using the API key (full context path, no tenant re-prefix). The backend payload
    # is pinned, as the key-type scenario at the end of this file already does: this scenario's whole claim is
    # that the generated API key is honoured, and only the body shows the key carried the request through to
    # node-customer-service (the API's other operation, DELETE /customers/{id}, answers 200 with an empty body).
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:api-key @type:negative @dep:gateway @legacy:APIKeyInvocationTestCase
  Scenario Outline: Invoke an api_key-secured API with an invalid key is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Invoke with a garbage API key — the gateway must reject it
    When I put the following JSON payload in context as "invalidApiKey"
    """
    invalid-api-key-value
    """
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "invalidApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # API-key IP restriction: a key generated with a permittedIP is enforced at the gateway using the client IP it
  # derives from X-Forwarded-For (REST passthrough) — a matching XFF is authorised (200), a non-matching one is
  # forbidden (403). Standalone-probed: key→1.2.3.4, XFF:1.2.3.4 → 200, XFF:5.6.7.8 → 403. Ports the api-key
  # IP-restriction cases of WebSocketAPITestCase (the positive path, which the WS transport can't demonstrate).
  @cap:key-manager @feat:api-key @rule:ip-restriction @type:regression @dep:gateway @legacy:APIKeyInvocationTestCase
  Scenario Outline: An IP-restricted API key is enforced via X-Forwarded-For as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    # Subscribe an application and generate an API key restricted to permittedIP 1.2.3.4
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ipSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "ipSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ipApiKeyGenPayload"
    """
    {"keyName": "IpRestrictedKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "1.2.3.4", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "ipApiKeyGenPayload"
    Then The response status code should be 200
    # A matching X-Forwarded-For is authorised (200)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "1.2.3.4" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # A non-matching X-Forwarded-For is forbidden (403)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "5.6.7.8" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # A permittedIP list carrying a CIDR RANGE and an IPv6 range — the three matching forms legacy exercises and
    # which a single exact-address key cannot: an exact IPv4 literal, an IPv4 inside a /24 CIDR, and an IPv6 inside
    # a /23 CIDR — each with its own outside-the-range forbidden counterpart. Ports the full permittedIP matrix of
    # APISecurityTestCase#testInvocationWithApiKeysWithIPCondition ("152.23.5.6, 192.168.1.2/24, 2001:c00::/23").
    When I put the following JSON payload in context as "ipRangeApiKeyGenPayload"
    """
    {"keyName": "IpRangeRestrictedKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "152.23.5.6, 192.168.1.2/24, 2001:c00::/23", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "ipRangeApiKeyGenPayload"
    Then The response status code should be 200
    # The exact IPv4 literal in the list → 200
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "152.23.5.6" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # An IPv4 INSIDE the 192.168.1.2/24 range → 200
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "192.168.1.6" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # An IPv4 OUTSIDE that /24 → 403
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "192.168.5.6" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # An IPv6 INSIDE the 2001:c00::/23 range → 200
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "2001:c00:0:0:0:0:c:4" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # An IPv6 OUTSIDE that range → 403
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "2061:c00:0:0:0:0:0:0" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # I4c: an internal API key generated for a sandbox-only API (no production endpoint) carries keytype SANDBOX
  # in its decoded JWT. Ports APIM18CreateAnAPIThroughThePublisherRestAPITestCase#testCreateApiWithOnlySandboxEndpoints.
  @cap:key-manager @feat:api-key @rule:internal-key @type:regression @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase
  Scenario Outline: An internal API key for a sandbox-only API carries keytype SANDBOX as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_sandbox_only_api.json" in context as "sandboxApiPayload"
    And I create an "apis" resource with payload "sandboxApiPayload" as "sandboxApiId"
    Then The response status code should be 201
    When I generate an internal API key for API "sandboxApiId" and store it as "sandboxInternalKey"
    Then The response status code should be 200
    And The JWT stored as "sandboxInternalKey" should contain "SANDBOX"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # API-key Referer restriction: a key generated with a permittedReferer is enforced at the gateway against the
  # request's Referer header — a matching exact path is authorised (200), a non-matching one is forbidden (403),
  # and a wildcard subdomain pattern matches (200). Ports the api-key Referer-restriction case of APISecurityTestCase.
  @cap:key-manager @feat:api-key @rule:referer-restriction @type:regression @dep:gateway @legacy:APISecurityTestCase
  Scenario Outline: A Referer-restricted API key is enforced via the Referer header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "refSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "refSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "refApiKeyGenPayload"
    """
    {"keyName": "RefRestrictedKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": "www.abc.com/path, sub.cds.com/*, *.gef.com/*"}}
    """
    And I request an api key for application id "createdAppId" using payload "refApiKeyGenPayload"
    Then The response status code should be 200
    # A matching exact referer path is authorised (200)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and referer "www.abc.com/path" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # A non-matching referer is forbidden (403)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and referer "www.abc.com/path2" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # A SPECIFIC-subdomain wildcard ("sub.cds.com/*") matches any path UNDER that exact host — a distinct pattern
    # form from the any-subdomain "*.gef.com/*" below, and a multi-segment path at that (200).
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and referer "sub.cds.com/path1/path2" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # A wildcard subdomain pattern matches (200)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and referer "example.gef.com/path1" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # API-key revocation: a generated key invokes successfully (200), is revoked via the devportal revoke endpoint,
  # then the same key is rejected at the gateway (401). Ports the api-key revocation of APISecurityTestCase.
  @cap:key-manager @feat:api-key @rule:revoked-key @type:regression @dep:gateway @legacy:APISecurityTestCase
  Scenario Outline: A revoked API key is rejected at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "revSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "revSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "revApiKeyGenPayload"
    """
    {"keyName": "RevokeTestKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "revApiKeyGenPayload"
    Then The response status code should be 200
    # The key works first (positive control)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # Revoke the (opaque) key by its keyUUID
    When I retrieve the api key UUID for application id "createdAppId" as "revokeKeyUuid"
    Then The response status code should be 200
    When I revoke the api key with UUID "revokeKeyUuid" for application id "createdAppId"
    Then The response status code should be 200
    # The same key is now rejected at the gateway (401)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:api-key @rule:custom-header @type:regression @dep:gateway @legacy:CustomHeaderTestCase
  Scenario Outline: An API key is accepted only in the API's configured custom api-key header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_apikeyheader_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"

    # Enable the api_key security scheme
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    # Set the custom api-key header on the API (a dedicated PUT, mirroring the legacy setApiKeyHeader)
    When I retrieve the "apis" resource with id "createdApiId"
    And I put the response payload in context as "createdApiPayload2"
    When I update the "apis" resource "createdApiId" and "createdApiPayload2" with configuration type "apiKeyHeader" and value:
      """
      Custom-ApiKey-Header
      """
    Then The response status code should be 200
    # (The publisher GET does not always echo apiKeyHeader back in its representation, so we don't assert it
    #  here — the gateway invocation below is the real proof the header took effect.)
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Subscribe an application and generate an API key
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "apiKeyGenerationPayload"
    """
    {"keyName": "CustomHeaderApiKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "apiKeyGenerationPayload"
    Then The response status code should be 200

    # The key in the API's configured custom header -> accepted (200).
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" in header "Custom-ApiKey-Header" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # The same key in the default ApiKey header -> rejected (401).
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" in header "ApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Subscriptionless + API key (per the WSO2 docs "secure-apis-using-api-keys / For subscriptionless APIs"): an API
  # whose business plans are cleared (policies=[] -> internal DefaultSubscriptionless tier) can be invoked with an
  # API key generated on an application that is NOT subscribed to it — the gateway validates only the key->API
  # mapping (+ IP/referer), with no subscription check. No legacy equivalent; this closes a documented gap.
  @cap:key-manager @feat:api-key @type:regression @dep:gateway @dep:publisher
  Scenario Outline: An API key invokes a subscriptionless API without any subscription as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "slApiId" and deployed it
    When I retrieve the "apis" resource with id "slApiId"
    Then The response status code should be 200
    And I put the response payload in context as "slApiPayload"

    # Enable the api_key security scheme, deploy and PUBLISH first — the internal DefaultSubscriptionless tier is
    # only auto-applied once the API is published, so publish before clearing the business plans.
    When I update the "apis" resource "slApiId" and "slApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I deploy the API with id "slApiId"
    Then The response status code should be 201
    And I wait until "apis" "slApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "slApiId"
    Then The lifecycle status of API "slApiId" should be "Published"

    # Now make the API subscriptionless — clear its business plans; the product auto-applies DefaultSubscriptionless.
    When I retrieve the "apis" resource with id "slApiId"
    And I put the response payload in context as "slApiPayload"
    When I update the "apis" resource "slApiId" and "slApiPayload" with configuration type "policies" and value:
      """
      []
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "slApiId"
    Then The response should contain "DefaultSubscriptionless"
    And I extract response field "context" and store it as "slApiContext"
    # Redeploy so the api_key security + subscriptionless plan take effect at the gateway.
    When I deploy the API with id "slApiId"
    Then The response status code should be 201
    And I wait until "apis" "slApiId" revision is deployed in the gateway

    # An application that is NEVER subscribed to this API — generate an API key on it.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "slAppPayload"
    And I create an application with payload "slAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "slApiKeyPayload"
    """
    {"keyName": "SubscriptionlessKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "slApiKeyPayload"
    Then The response status code should be 200

    # The API key invokes the subscriptionless API with NO subscription -> 200 (gateway checks key->API, not a sub).
    When I invoke the API at gateway context "{{slApiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports testInvocationWithApiKeysWithoutSubscription: an API key that WORKS becomes refused once the
  # application's subscription to the API is removed, and the refusal is 403 (subscription validation failed) —
  # NOT the 401 an invalid key gets. This is the api-key counterpart of the unsubscribed-OAuth-token case in
  # gateway/security_enforcement.feature, and it is the exact opposite of the subscriptionless scenario above:
  # there the API's business plans were cleared so no subscription is required, here the API requires one and it
  # has been taken away. Both directions must hold, so both are asserted.
  #
  # The 403 is reached by POLLING: the gateway's subscription cache is invalidated through a JMS event, so the
  # key keeps working for a moment after the DELETE. The retry envelope waits for the exact expected status.
  @cap:key-manager @feat:api-key @rule:no-subscription @type:negative @dep:gateway @dep:devportal @legacy:APISecurityTestCase
  Scenario Outline: An API key is refused once the application's subscription is removed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "nsApiId" and deployed it
    When I retrieve the "apis" resource with id "nsApiId"
    Then The response status code should be 200
    And I put the response payload in context as "nsApiPayload"
    When I update the "apis" resource "nsApiId" and "nsApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "nsApiId"
    And I extract response field "context" and store it as "nsApiContext"
    When I deploy the API with id "nsApiId"
    Then The response status code should be 201
    And I wait until "apis" "nsApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "nsApiId"
    Then The lifecycle status of API "nsApiId" should be "Published"

    # Subscribe an application and generate an API key on it.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "nsAppPayload"
    And I create an application with payload "nsAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "nsSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "nsApiId" using application "createdAppId" with payload "nsSubscriptionPayload" as "nsSubscriptionId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "nsApiKeyGenPayload"
    """
    {"keyName": "NoSubscriptionKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "nsApiKeyGenPayload"
    Then The response status code should be 200

    # Positive control: WITH the subscription the key invokes successfully.
    When I invoke the API at gateway context "{{nsApiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Remove the subscription — the key itself is untouched and still valid.
    When I delete the subscription with id "nsSubscriptionId"
    Then The response status code should be 200

    # The same key now settles on 403 (subscription validation failed), not 401.
    When I invoke the API at gateway context "{{nsApiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An api_key-ONLY API (no oauth2, no basic_auth) exercised with BOTH key types, plus the Basic-scheme negative.
  # Ports testInvocationWithApiKeysOnly — where legacy asserts a PRODUCTION api key 200 AND a SANDBOX api key 200
  # whose body carries the expected customer payload (the key type is part of the generate path and decides which
  # endpoint the gateway routes to, so a sandbox key is a different credential, not a naming variant) — and
  # testInvocationWithBasicAuthFoAPIKeyNegative, whose legacy form is hollow: it sent "Authorization: Basic abcce",
  # which is not valid base64 of any user:password, so its 401 only proved "a garbage header is refused" and never
  # that the Basic SCHEME is refused where it is not permitted. Here the Basic credential is a WELL-FORMED base64
  # of a REAL existing user (the same credential that returns 200 on a basic_auth-permitting API), so the refusal
  # is genuinely about the scheme. Both api-key legs are positive controls for that negative.
  # OBSERVED: that refusal is 401 with code 900902 "Missing Credentials", not 900901 - on an api_key-only API the
  # Authorization header is not a candidate credential at all, so the gateway reports no credential rather than a bad
  # one. Legacy's malformed "Basic abcce" would have produced the same bare 401 even if the Basic scheme were fully
  # honoured, which is precisely why the code is pinned here.
  @cap:key-manager @feat:api-key @rule:key-type @type:regression @dep:gateway @legacy:APISecurityTestCase
  Scenario Outline: An api_key-only API is invocable with production and sandbox api keys but refuses Basic as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_apikey_only_api.json" as "koApiId" and deployed it
    When I publish the "apis" resource with id "koApiId"
    Then The lifecycle status of API "koApiId" should be "Published"
    When I retrieve the "apis" resource with id "koApiId"
    And I extract response field "context" and store it as "koContext"
    When I have set up application with keys, subscribed to API "koApiId", and obtained access token for "koSubId"
    Then The response status code should be 200

    # A PRODUCTION api key invokes the API, and the body is the backend's customer payload (not merely a 200).
    When I put the following JSON payload in context as "koApiKeyGenPayload"
    """
    {"keyName": "KeyTypeProdKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key of type "PRODUCTION" for application id "createdAppId" using payload "koApiKeyGenPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{koContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # A SANDBOX api key on the SAME application also invokes it (routed to the sandbox endpoint), same body.
    When I put the following JSON payload in context as "koSandboxKeyGenPayload"
    """
    {"keyName": "KeyTypeSandboxKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key of type "SANDBOX" for application id "createdAppId" using payload "koSandboxKeyGenPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{koContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # A WELL-FORMED Basic credential for a real user, on an API that does not permit basic_auth → refused (401).
    When I invoke the API at gateway context "{{koContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900902" message "Missing Credentials" and description containing "Make sure your API invocation call has a header"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
