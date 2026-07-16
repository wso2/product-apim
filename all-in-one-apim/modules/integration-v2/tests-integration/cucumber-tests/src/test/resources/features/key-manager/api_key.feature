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
    Then The response should contain "api_key"
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

    # Invoke through the gateway using the API key (full context path, no tenant re-prefix)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

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
    # A non-matching X-Forwarded-For is forbidden (403)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and forwarded-for "5.6.7.8" until response status code becomes 403 within 60 seconds
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
    # A non-matching referer is forbidden (403)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and referer "www.abc.com/path2" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # A wildcard subdomain pattern matches (200)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" and referer "example.gef.com/path1" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

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
    # The same key in the default ApiKey header -> rejected (401).
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" in header "ApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
