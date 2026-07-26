@cleanup
Feature: Gateway Security Enforcement

  Gateway-plane runtime security enforcement (negatives), in both the super tenant and tenant1.com:
  an invalid bearer token is rejected (401), and a valid token from an application that is NOT subscribed
  to the API is refused access (403). The valid-token happy path is covered by gateway/rest-invocation.
  Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:security-enforcement @type:negative @rule:invalid-token @dep:publisher @legacy:APIMANAGERInvocationTestCase @legacy:InvalidTokenTestCase
  Scenario Outline: Invoke a published API with an invalid token is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    When I put the following JSON payload in context as "invalidAccessToken"
    """
    abcdefgh
    """
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "invalidAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "Make sure you have provided the correct security credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:security-enforcement @type:negative @rule:no-subscription @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published API with an unsubscribed application's token is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Create an application + keys + token, but DO NOT subscribe it to the API
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

    # Invoking without a subscription must be refused (403)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # End-to-end subscription-blocking runtime toggle (the legacy SubscriptionBlockingTestCase arc in one flow):
  # a subscribed token invokes (200), the subscription is blocked -> gateway refuses, then unblocked ->
  # invocation is restored. The block/unblock calls are management-plane (@dep:devportal); the assertion here is
  # the runtime enforcement, so it is @cap:gateway.
  @cap:gateway @feat:security-enforcement @type:regression @rule:subscription-blocking @dep:publisher @dep:devportal @legacy:SubscriptionBlockingTestCase
  Scenario Outline: A blocked subscription is refused at the gateway and restored on unblock as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and obtain a token
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

    # Subscribed -> invocation succeeds
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Block the subscription -> gateway refuses the same token (401, code 900907 "temporarily blocked")
    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Unblock -> invocation is restored
    When I unblock the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Basic-auth application security: an API whose securityScheme is basic_auth accepts a valid carbon user's HTTP
  # Basic credentials at the gateway (200) and rejects invalid credentials (401). Ports the basic-auth cases of
  # APISecurityTestCase (a distinct auth SCHEME, not tested elsewhere in the suite).
  @cap:gateway @feat:security-enforcement @rule:basic-auth @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A basic-auth-secured API accepts valid user credentials and rejects invalid ones as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["basic_auth", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    # Valid carbon user credentials → 200
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # A valid user with the WRONG password → 401
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" with password "totallyWrongPassword" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A large request body does NOT bypass gateway authentication: a POST of a ~1 MB body with an INVALID bearer token
  # is rejected, regardless of body size. Ports InvalidAuthTokenLargePayloadTestCase (uploads 1KB/100KB/1MB with a
  # bad token and asserts the upload is rejected). The API exposes a POST /reflect-body resource so the request
  # reaches the auth check — a POST to a GET-only resource returns 405 before auth (verified live); with a POST
  # resource, the invalid token is rejected with 401 (the large body is captured cleanly, no connection drop needed).
  @cap:gateway @feat:security-enforcement @type:negative @rule:invalid-token @dep:publisher @legacy:InvalidAuthTokenLargePayloadTestCase
  Scenario Outline: A large payload with an invalid token is rejected at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "lpApiId" and deployed it
    When I publish the "apis" resource with id "lpApiId"
    Then The lifecycle status of API "lpApiId" should be "Published"
    When I retrieve the "apis" resource with id "lpApiId"
    And I extract response field "context" and store it as "lpContext"

    When I put the following JSON payload in context as "lpInvalidToken"
    """
    abcdefgh
    """
    # Warm-up: confirm the POST resource is routable and a small invalid-token POST is rejected (401).
    And I invoke the API at gateway context "{{lpContext}}/1.0.0/reflect-body" with method "POST" using access token "lpInvalidToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # The large body must NOT bypass auth — single shot, expect rejection (401 response, or a connection drop).
    When I invoke the API at gateway context "{{lpContext}}/1.0.0/reflect-body" with method "POST" using access token "lpInvalidToken" and a 1024 KB payload expecting authentication rejection

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Malformed XML robustness: an API whose POST operation carries a body-parsing policy (jsonToXML) forces the Synapse
  # message builder to run on the request. POSTing a malformed XML body (Content-Type application/xml) must be handled
  # cleanly — a server error, not a gateway crash / dropped connection. Ports MalformedRequestTest, which POSTs the
  # malformed body to getGatewayURLNhttp()+"response". That is NOT a bare path: in the shared legacy suite a
  # "/response" API is deployed by another test, so the request matches it and its sequence tries to BUILD the body
  # -> Woodstox WstxEOFException on the unclosed <request> -> fault sequence -> 500 (confirmed in CI: wire log shows
  # {api:Response_API_1} ... "HTTP/1.1 500 Internal Server Error"). A bare /response with no API deployed just 404s
  # (unmatched context) — expected, not a change. Rather than depend on a stray cross-test API, this isolated test
  # deploys its OWN body-building API (a jsonToXML request policy forces the same builder), reproducing the identical
  # malformed-parse 500. Unlike the legacy (which asserted only the 500 status), the fault body here exposes the
  # Synapse error code (601000) and the Woodstox message, so this asserts the exact root cause, not just the code.
  @cap:gateway @feat:security-enforcement @type:negative @rule:malformed-request @dep:publisher @legacy:MalformedRequestTest
  Scenario Outline: A malformed XML request body is handled cleanly by the gateway message builder as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_jsontoxml_api.json" as "mfApiId" and deployed it
    When I publish the "apis" resource with id "mfApiId"
    Then The lifecycle status of API "mfApiId" should be "Published"
    When I retrieve the "apis" resource with id "mfApiId"
    And I extract response field "context" and store it as "mfContext"
    When I have set up application with keys, subscribed to API "mfApiId", and obtained access token for "mfSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mfBody"
    """
    <request>Request<request>
    """
    # A malformed XML body (unclosed element) with an application/xml content type drives the builder → clean 500.
    And I invoke the API at gateway context "{{mfContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "mfBody" with content type "application/xml" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500
    # Pin the ROOT CAUSE, not just the status: the fault body carries Synapse error code 601000 and the exact
    # Woodstox parser message, proving the 500 is the malformed-XML build failure (not some incidental 500).
    And The response should contain "601000"
    And The response should contain "Unexpected EOF; was expecting a close tag for element <request>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the security-relevant assertions of ErrorResponseCheckTestCase — a gateway error response must NOT leak
  # the offending input back to the caller. Invoking a NON-EXISTENT context returns 404 whose body does not echo the
  # requested context path; invoking with an INVALID token returns 401 whose body does not echo the token value.
  # (The happy-path 200 is covered by rest-invocation; here the subject is the no-leakage property of the error
  # bodies.)
  @cap:gateway @feat:security-enforcement @type:negative @rule:error-response-leakage @dep:publisher @legacy:ErrorResponseCheckTestCase
  Scenario Outline: Gateway error responses do not echo the offending context or token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "erApiId" and deployed it
    When I publish the "apis" resource with id "erApiId"
    Then The lifecycle status of API "erApiId" should be "Published"
    When I retrieve the "apis" resource with id "erApiId"
    And I extract response field "context" and store it as "erContext"
    When I have set up application with keys, subscribed to API "erApiId", and obtained access token for "erSubId"
    Then The response status code should be 200

    # A non-existent context → 404 that does NOT echo the requested path segment.
    When I invoke the API at gateway context "erNoSuchContext/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The response should not contain "erNoSuchContext"

    # An invalid token → 401 that does NOT echo the token value.
    When I put the following JSON payload in context as "erBadToken"
    """
    erSecretTokenValue12345
    """
    And I invoke the API at gateway context "{{erContext}}/1.0.0/customers/123/" with method "GET" using access token "erBadToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should not contain "erSecretTokenValue12345"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the operation-level auth-type enforcement of ChangeAuthTypeOfResourceTestCase. The legacy test cycles a
  # resource through the four auth types (Application & Application User, Application, Application User, None) and
  # invokes each WITH a valid token → 200 (which does not discriminate between the types). The security-relevant
  # distinction is the "None" auth type: a resource with authType "None" is invocable WITHOUT any token (200),
  # whereas the default "Application & Application User" resource requires one (401 without a token). This scenario
  # pins that discriminating behaviour. The resource is switched to authType None via an operations update + redeploy.
  @cap:gateway @feat:security-enforcement @rule:resource-auth-type @type:regression @dep:publisher @legacy:ChangeAuthTypeOfResourceTestCase
  Scenario Outline: A resource with authType None is invocable without a token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "atApiId" and deployed it
    When I publish the "apis" resource with id "atApiId"
    Then The lifecycle status of API "atApiId" should be "Published"
    When I retrieve the "apis" resource with id "atApiId"
    And I extract response field "context" and store it as "atContext"

    # Default resource (Application & Application User): a token-less invocation is rejected (401).
    When I invoke the API at gateway context "{{atContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Switch the GET resource to authType "None" and redeploy.
    When I retrieve the "apis" resource with id "atApiId"
    And I put the response payload in context as "atPayload"
    When I update the "apis" resource "atApiId" and "atPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    When I deploy the API with id "atApiId"
    Then The response status code should be 201

    # Now the resource is invocable WITHOUT a token (200).
    When I invoke the API at gateway context "{{atContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports ChangeEndPointSecurityOfAPITestCase (commented-out in the legacy suite) — an API whose BACKEND endpoint is
  # secured with HTTP Basic auth causes the gateway to inject an "Authorization: Basic <base64(user:pass)>" header on
  # the backend leg. The backend /sec route echoes the Authorization header it received, so a 200 whose body carries
  # the base64 of the configured credentials PROVES the gateway injected the endpoint-security header. Uses
  # user=admin1 / pass=admin123 → base64("admin1:admin123") = "YWRtaW4xOmFkbWluMTIz". (The legacy complex-password
  # matrix over 28 symbolic characters is represented by this single deterministic case — the encoding path is
  # identical regardless of the password bytes.)
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityOfAPITestCase
  Scenario Outline: A backend secured with Basic auth receives the gateway-injected Authorization header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_basicauth_api.json" as "epApiId" and deployed it
    When I publish the "apis" resource with id "epApiId"
    Then The lifecycle status of API "epApiId" should be "Published"
    When I retrieve the "apis" resource with id "epApiId"
    And I extract response field "context" and store it as "epContext"
    When I have set up application with keys, subscribed to API "epApiId", and obtained access token for "epSubId"
    Then The response status code should be 200

    # The backend /sec route echoes the Authorization header the gateway injected — assert it is the configured
    # Basic credential.
    When I invoke the API at gateway context "{{epContext}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "YWRtaW4xOmFkbWluMTIz"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Per-type endpoint security (production AND sandbox, DISTINCT credentials): a production-key token is injected
  # with the production backend credential; a sandbox-key token (on the same application) is injected with the
  # DISTINCT sandbox credential — proving the gateway selects the endpoint-security config by the key type of the
  # invoking token. Also confirms the retrieved API redacts BOTH stored passwords. Runs in both tenants (×2).
  # Ports the core of AddEndPointSecurityPerTypeTestCase (per-key-type BASIC injection + secret redaction).
  # base64(prodUser:prodPass)=cHJvZFVzZXI6cHJvZFBhc3M=, base64(sandUser:sandPass)=c2FuZFVzZXI6c2FuZFBhc3M=.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Per-type endpoint security injects the matching Basic credential per key type as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_both.json" as "epsApiId" and deployed it
    When I publish the "apis" resource with id "epsApiId"
    Then The lifecycle status of API "epsApiId" should be "Published"
    # Retrieved API redacts the stored backend passwords (never returned in plaintext).
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "prodPass"
    And The response should not contain "sandPass"
    And I extract response field "context" and store it as "epsCtx"

    # Production key → backend receives the PRODUCTION Basic credential.
    When I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "cHJvZFVzZXI6cHJvZFBhc3M="

    # Sandbox key on the SAME application → backend receives the DISTINCT SANDBOX credential, not the production one.
    When I put the following JSON payload in context as "epsSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "epsSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epsSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "epsSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "c2FuZFVzZXI6c2FuZFBhc3M="
    And The response should not contain "cHJvZFVzZXI6cHJvZFBhc3M="

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Per-type endpoint security (PRODUCTION only): the production backend is Basic-secured (admin1:admin123 →
  # YWRtaW4xOmFkbWluMTIz), the sandbox endpoint is unsecured. A production-key token gets the injected header; a
  # sandbox-key token, routed to the unsecured sandbox endpoint, gets none. Runs in both tenants (×2).
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Per-type endpoint security — production-only Basic auth is injected only for production keys as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_prod_only.json" as "epsApiId" and deployed it
    When I publish the "apis" resource with id "epsApiId"
    Then The lifecycle status of API "epsApiId" should be "Published"
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "admin123"
    And I extract response field "context" and store it as "epsCtx"

    When I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "YWRtaW4xOmFkbWluMTIz"

    When I put the following JSON payload in context as "epsSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "epsSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epsSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "epsSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "Basic"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Per-type endpoint security (SANDBOX only): the mirror case — sandbox backend Basic-secured, production
  # unsecured. A sandbox-key token gets the injected header; a production-key token gets none. Runs in both tenants.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Per-type endpoint security — sandbox-only Basic auth is injected only for sandbox keys as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_sandbox_only.json" as "epsApiId" and deployed it
    When I publish the "apis" resource with id "epsApiId"
    Then The lifecycle status of API "epsApiId" should be "Published"
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "admin123"
    And I extract response field "context" and store it as "epsCtx"

    # Production key → routed to the unsecured production endpoint → no Basic header.
    When I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "Basic"

    # Sandbox key on the SAME application → backend receives the sandbox Basic credential.
    When I put the following JSON payload in context as "epsSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "epsSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epsSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "epsSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "YWRtaW4xOmFkbWluMTIz"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
