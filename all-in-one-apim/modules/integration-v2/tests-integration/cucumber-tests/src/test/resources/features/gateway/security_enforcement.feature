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
  @cap:gateway @feat:security-enforcement @type:regression @rule:subscription-blocking @dep:publisher @dep:devportal @legacy:SubscriptionBlockingTestCase @legacy:ApplicationBlockSubscriptionTestCase
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

  # The SAME enforcement arc, but with a HYPHEN in both halves of the identity the gateway looks the block up by -
  # the application NAME and the application OWNER's username - which is the specific regression
  # ApplicationBlockSubscriptionTestCase guards ("test-app" owned by "test-user"). It is not a duplicate of the
  # outline above: there every application name is generated from an alphanumeric base and the owner is always
  # "admin", so a separator-parsing regression in the blocked-subscription lookup key would pass unnoticed.
  # The owner is created by SELF-SIGNUP, as the legacy did (UserManagementUtils.signupUser), not by an admin SOAP
  # add - so the credential reaching the gateway is a real consumer-onboarded one. This block carries no workflow
  # overlay, so the sign-up grants the subscriber role immediately and the new user can hold its own application.
  @cap:gateway @feat:security-enforcement @type:regression @rule:subscription-blocking @dep:publisher @dep:devportal @legacy:ApplicationBlockSubscriptionTestCase
  Scenario Outline: A blocked subscription of a hyphenated application owned by a hyphenated self-signed-up user is refused at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # The hyphenated OWNER, onboarded through the product's own self-registration path
    When I self-sign-up a DevPortal user with a hyphenated username and password "Signup#12345" as actor "<ownerActor>" storing the username as "blockHyphenOwnerName"
    Then The response status code should be 201
    And the user "{{blockHyphenOwnerName}}" in tenant "<tenant>" should exist

    # ...owning a hyphenated APPLICATION, subscribed to the API, with its own token
    Given I act as "<ownerActor>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I generate a unique alphanumeric value and store it as "blockHyphenAppSuffix"
    And I put the following JSON payload in context as "blockHyphenAppCreate"
    """
    {"name": "blk-app-{{blockHyphenAppSuffix}}", "throttlingPolicy": "Unlimited", "description": "Hyphenated application for the subscription-block regression"}
    """
    And I create an application with payload "blockHyphenAppCreate"
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

    # Block as the API's own provider (the block endpoint is publisher-scoped, and the subscriber cannot self-block)
    Given I act as "<actor>"
    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    When I unblock the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             | tenant       | ownerActor                   |
      | admin             | carbon.super | blockHyphenOwner             |
      | admin@tenant1.com | tenant1.com  | blockHyphenOwner@tenant1.com |

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
    # Deploy-readiness gate: the token-less invokes below can only retry the REQUEST, so a lost runtime
    # propagation event is unrecoverable that way and surfaces as a 404 "Invalid URL" instead of the
    # expected 401/200 (observed in this runner). Re-fires the deploy if the artifact never lands.
    And the "apis" resource "atApiId" should be live on the gateway, redeploying if propagation is lost
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
  # user=admin1 / pass=admin123 → base64("admin1:admin123") = "YWRtaW4xOmFkbWluMTIz". The legacy complex-password
  # matrix over 28 symbolic characters is exercised by the dedicated symbolic-password scenario below.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityOfAPITestCase
  Scenario Outline: A backend secured with Basic auth receives the gateway-injected Authorization header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_basicauth_api.json" as "epApiId" and deployed it
    When I publish the "apis" resource with id "epApiId"
    Then The lifecycle status of API "epApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epApiId" should be live on the gateway, redeploying if propagation is lost
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

  # Ports ChangeEndPointSecurityOfAPITestCase#testInvokeGETResourceWithSecuredEndPointComplexPassword — the whole
  # point of that test is the ENCODING of symbolic characters in the backend-security password, which the plain
  # admin1:admin123 case above cannot catch (a gateway that mangled/escaped a symbol, or double-encoded the
  # credential, would still pass it). Legacy walked 28 symbols one at a time, re-updating and redeploying the API
  # for each — 28 deploy cycles. Here the SAME 28 symbols are covered by two credentials whose exact base64 is
  # asserted, split so that BOTH the create path and the update path carry symbols:
  #   create: user / abcd-+={[}]|:;'<,>.?/efghijk  → base64 = dXNlcjphYmNkLSs9e1t9XXw6Oyc8LD4uPy9lZmdoaWpr
  #   update: user / abcd!@#$%^&*()_efghijk       → base64 = dXNlcjphYmNkIUAjJCVeJiooKV9lZmdoaWpr
  # The symbolic credential is carried in a doc string, so tenant parameterization does not alter it.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityOfAPITestCase
  Scenario Outline: An endpoint-security password of symbolic characters is base64-encoded verbatim on injection as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_symbolic.json" as "epsymApiId" and deployed it
    When I publish the "apis" resource with id "epsymApiId"
    Then The lifecycle status of API "epsymApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epsymApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epsymApiId"
    And I extract response field "context" and store it as "epsymCtx"
    When I have set up application with keys, subscribed to API "epsymApiId", and obtained access token for "epsymSubId"
    Then The response status code should be 200

    # CREATE path: the backend receives exactly base64(user:abcd-+={[}]|:;'<,>.?/efghijk).
    When I invoke the API at gateway context "{{epsymCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "dXNlcjphYmNkLSs9e1t9XXw6Oyc8LD4uPy9lZmdoaWpr"

    # UPDATE path: switch to the second symbolic credential and redeploy.
    When I retrieve the "apis" resource with id "epsymApiId"
    And I put the response payload in context as "epsymPayload"
    When I put the following JSON payload in context as "epsymNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"user","password":"abcd!@#$%^&*()_efghijk"},"sandbox":{"enabled":true,"type":"BASIC","username":"user","password":"abcd!@#$%^&*()_efghijk"}}}
    """
    When I update the "apis" resource "epsymApiId" and "epsymPayload" with configuration type "endpointConfig" and value:
    """
    epsymNewEndpoint
    """
    Then The response status code should be 200
    When I deploy the API with id "epsymApiId"
    Then The response status code should be 201

    # The backend now receives exactly base64(user:abcd!@#$%^&*()_efghijk), and no longer the create-path credential.
    When I invoke the API at gateway context "{{epsymCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "dXNlcjphYmNkIUAjJCVeJiooKV9lZmdoaWpr" within 60 seconds
    Then The response status code should be 200
    And The response should contain "dXNlcjphYmNkIUAjJCVeJiooKV9lZmdoaWpr"
    And The response should not contain "dXNlcjphYmNkLSs9e1t9XXw6Oyc8LD4uPy9lZmdoaWpr"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Per-type endpoint security (production AND sandbox, DISTINCT credentials): a production-key token is injected
  # with the production backend credential; a sandbox-key token (on the same application) is injected with the
  # DISTINCT sandbox credential — proving the gateway selects the endpoint-security config by the key type of the
  # invoking token. Also confirms the retrieved API redacts BOTH stored passwords.
  # Ports the core of AddEndPointSecurityPerTypeTestCase (per-key-type BASIC injection + secret redaction).
  # base64(prodUser:prodPass)=cHJvZFVzZXI6cHJvZFBhc3M=, base64(sandUser:sandPass)=c2FuZFVzZXI6c2FuZFBhc3M=.
  # The <creator> column runs the arc as a NON-ADMIN publisher too (legacy SUPER_TENANT_USER / TENANT_USER); the
  # devportal half stays with <consumer> because publisherUser's devportal token has no application-manage scope
  # (POST /applications -> 401) and so cannot drive the subscribe composite.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Per-type endpoint security injects the matching Basic credential per key type as <creator>
    Given The system is ready
    And I have valid access tokens as "<consumer>"
    And The system is ready and I have valid publisher access tokens as "<creator>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_both.json" as "epsApiId" and deployed it
    When I publish the "apis" resource with id "epsApiId"
    Then The lifecycle status of API "epsApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epsApiId" should be live on the gateway, redeploying if propagation is lost
    # Retrieved API redacts the stored backend passwords (never returned in plaintext).
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "prodPass"
    And The response should not contain "sandPass"
    And I extract response field "context" and store it as "epsCtx"
    # The API is genuinely authored by <creator> — without this the non-admin creator rows would be vacuous if the
    # actor silently fell back to admin.
    And The provider of API "epsApiId" should match actor "<creator>"

    # Production key → backend receives the PRODUCTION Basic credential.
    When I act as "<consumer>"
    And I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
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
      | creator                   | consumer          |
      | admin                     | admin             |
      | admin@tenant1.com         | admin@tenant1.com |
      | publisherUser             | admin             |
      | publisherUser@tenant1.com | admin@tenant1.com |

  # CHANGE path — updating the PRODUCTION endpoint-security credential on an EXISTING API. Ports the
  # testUpdateEndpointSecurityForProduction arc of ChangeEndPointSecurityPerTypeTestCase: create a per-type
  # BASIC-secured API (prodInit / sandInit), invoke with a production-key token and assert the backend received
  # base64(prodInit:prodInitPass); then GET-mutate-PUT the endpoint_security to a NEW production credential
  # (prodNew:prodNewPass), redeploy, and assert the gateway now injects base64(prodNew:prodNewPass) — proving the
  # UPDATED credential (not the stale one) is injected. base64(prodInit:prodInitPass)=cHJvZEluaXQ6cHJvZEluaXRQYXNz,
  # base64(prodNew:prodNewPass)=cHJvZE5ldzpwcm9kTmV3UGFzcw==. Runs in both tenants (×2).
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityPerTypeTestCase @legacy:ChangeEndPointSecurityOfAPITestCase
  Scenario Outline: Updating the production endpoint-security credential injects the new Basic credential as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_change.json" as "epcApiId" and deployed it
    When I publish the "apis" resource with id "epcApiId"
    Then The lifecycle status of API "epcApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epcApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epcApiId"
    And I extract response field "context" and store it as "epcCtx"

    # Production key → backend receives the INITIAL production Basic credential.
    When I have set up application with keys, subscribed to API "epcApiId", and obtained access token for "epcSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epcCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "cHJvZEluaXQ6cHJvZEluaXRQYXNz"

    # UPDATE the production endpoint_security to a NEW credential (GET-mutate-PUT), then redeploy.
    When I retrieve the "apis" resource with id "epcApiId"
    And I put the response payload in context as "epcPayload"
    When I put the following JSON payload in context as "epcNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"prodNew","password":"prodNewPass"},"sandbox":{"enabled":true,"type":"BASIC","username":"sandInit","password":"sandInitPass"}}}
    """
    When I update the "apis" resource "epcApiId" and "epcPayload" with configuration type "endpointConfig" and value:
    """
    epcNewEndpoint
    """
    Then The response status code should be 200
    When I deploy the API with id "epcApiId"
    Then The response status code should be 201

    # Production key on the SAME token → backend now receives the UPDATED production Basic credential.
    When I invoke the API at gateway context "{{epcCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "cHJvZE5ldzpwcm9kTmV3UGFzcw==" within 60 seconds
    Then The response status code should be 200
    And The response should contain "cHJvZE5ldzpwcm9kTmV3UGFzcw=="
    And The response should not contain "cHJvZEluaXQ6cHJvZEluaXRQYXNz"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # CHANGE path — updating the SANDBOX endpoint-security credential on an EXISTING API. Ports the
  # testUpdateEndpointSecurityForSandbox arc: a sandbox-key token receives base64(sandInit:sandInitPass) initially;
  # after the sandbox endpoint_security is updated to sandNew:sandNewPass and redeployed, the same sandbox-key token
  # receives base64(sandNew:sandNewPass). base64(sandInit:sandInitPass)=c2FuZEluaXQ6c2FuZEluaXRQYXNz,
  # base64(sandNew:sandNewPass)=c2FuZE5ldzpzYW5kTmV3UGFzcw==. Runs in both tenants (×2).
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityPerTypeTestCase
  Scenario Outline: Updating the sandbox endpoint-security credential injects the new Basic credential as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_change.json" as "epcApiId" and deployed it
    When I publish the "apis" resource with id "epcApiId"
    Then The lifecycle status of API "epcApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epcApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epcApiId"
    And I extract response field "context" and store it as "epcCtx"

    # Sandbox key → backend receives the INITIAL sandbox Basic credential.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "epcAppPayload"
    And I create an application with payload "epcAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "epcApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "epcSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "epcSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "epcSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epcSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "epcSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epcCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "c2FuZEluaXQ6c2FuZEluaXRQYXNz"

    # UPDATE the sandbox endpoint_security to a NEW credential (GET-mutate-PUT), then redeploy.
    When I retrieve the "apis" resource with id "epcApiId"
    And I put the response payload in context as "epcPayload"
    When I put the following JSON payload in context as "epcNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"prodInit","password":"prodInitPass"},"sandbox":{"enabled":true,"type":"BASIC","username":"sandNew","password":"sandNewPass"}}}
    """
    When I update the "apis" resource "epcApiId" and "epcPayload" with configuration type "endpointConfig" and value:
    """
    epcNewEndpoint
    """
    Then The response status code should be 200
    When I deploy the API with id "epcApiId"
    Then The response status code should be 201

    # Sandbox key on the SAME token → backend now receives the UPDATED sandbox Basic credential.
    When I invoke the API at gateway context "{{epcCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "c2FuZE5ldzpzYW5kTmV3UGFzcw==" within 60 seconds
    Then The response status code should be 200
    And The response should contain "c2FuZE5ldzpzYW5kTmV3UGFzcw=="
    And The response should not contain "c2FuZEluaXQ6c2FuZEluaXRQYXNz"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # CHANGE path — updating BOTH production and sandbox endpoint-security credentials at once on an EXISTING API.
  # Ports the testUpdateEndpointSecurityForSandboxAndProduction arc: after both are updated and redeployed, a
  # production-key token receives base64(prodNew:prodNewPass) and a sandbox-key token (same application) receives
  # base64(sandNew:sandNewPass) — the gateway selects the per-key-type UPDATED credential. Runs in both tenants (×2).
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityPerTypeTestCase
  Scenario Outline: Updating both production and sandbox endpoint-security credentials injects each new Basic credential per key type as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_change.json" as "epcApiId" and deployed it
    When I publish the "apis" resource with id "epcApiId"
    Then The lifecycle status of API "epcApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epcApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epcApiId"
    And I extract response field "context" and store it as "epcCtx"

    # Set up an application subscribed to the API and generate a PRODUCTION token.
    When I have set up application with keys, subscribed to API "epcApiId", and obtained access token for "epcSubId"
    Then The response status code should be 200

    # UPDATE both endpoint_security credentials (GET-mutate-PUT), then redeploy.
    When I retrieve the "apis" resource with id "epcApiId"
    And I put the response payload in context as "epcPayload"
    When I put the following JSON payload in context as "epcNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"prodNew","password":"prodNewPass"},"sandbox":{"enabled":true,"type":"BASIC","username":"sandNew","password":"sandNewPass"}}}
    """
    When I update the "apis" resource "epcApiId" and "epcPayload" with configuration type "endpointConfig" and value:
    """
    epcNewEndpoint
    """
    Then The response status code should be 200
    When I deploy the API with id "epcApiId"
    Then The response status code should be 201

    # Production key → backend receives the UPDATED production Basic credential.
    When I invoke the API at gateway context "{{epcCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "cHJvZE5ldzpwcm9kTmV3UGFzcw==" within 60 seconds
    Then The response status code should be 200
    And The response should contain "cHJvZE5ldzpwcm9kTmV3UGFzcw=="

    # Sandbox key on the SAME application → backend receives the UPDATED sandbox Basic credential.
    When I put the following JSON payload in context as "epcSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "epcSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epcSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "epcSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epcCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "c2FuZE5ldzpzYW5kTmV3UGFzcw==" within 60 seconds
    Then The response status code should be 200
    And The response should contain "c2FuZE5ldzpzYW5kTmV3UGFzcw=="
    And The response should not contain "cHJvZE5ldzpwcm9kTmV3UGFzcw=="

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Per-type endpoint security (PRODUCTION only): the production backend is Basic-secured (admin1:admin123 →
  # YWRtaW4xOmFkbWluMTIz), the sandbox endpoint is unsecured. A production-key token gets the injected header; a
  # sandbox-key token, routed to the unsecured sandbox endpoint, gets none.
  # The <creator> column runs the arc as a NON-ADMIN publisher too (legacy SUPER_TENANT_USER / TENANT_USER); the
  # devportal half stays with <consumer> (publisherUser cannot create applications — see the per-type scenario).
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Per-type endpoint security — production-only Basic auth is injected only for production keys as <creator>
    Given The system is ready
    And I have valid access tokens as "<consumer>"
    And The system is ready and I have valid publisher access tokens as "<creator>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_prod_only.json" as "epsApiId" and deployed it
    When I publish the "apis" resource with id "epsApiId"
    Then The lifecycle status of API "epsApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epsApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "admin123"
    And I extract response field "context" and store it as "epsCtx"
    # The API is genuinely authored by <creator> (see the per-type scenario above).
    And The provider of API "epsApiId" should match actor "<creator>"

    When I act as "<consumer>"
    And I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
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
      | creator                   | consumer          |
      | admin                     | admin             |
      | admin@tenant1.com         | admin@tenant1.com |
      | publisherUser             | admin             |
      | publisherUser@tenant1.com | admin@tenant1.com |

  # Per-type endpoint security (SANDBOX only): the mirror case — sandbox backend Basic-secured, production
  # unsecured. A sandbox-key token gets the injected header; a production-key token gets none.
  # The <creator> column runs the arc as a NON-ADMIN publisher too (legacy SUPER_TENANT_USER / TENANT_USER); the
  # devportal half stays with <consumer> (publisherUser cannot create applications — see the per-type scenario).
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Per-type endpoint security — sandbox-only Basic auth is injected only for sandbox keys as <creator>
    Given The system is ready
    And I have valid access tokens as "<consumer>"
    And The system is ready and I have valid publisher access tokens as "<creator>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_sandbox_only.json" as "epsApiId" and deployed it
    When I publish the "apis" resource with id "epsApiId"
    Then The lifecycle status of API "epsApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epsApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "admin123"
    And I extract response field "context" and store it as "epsCtx"
    # The API is genuinely authored by <creator> (see the per-type scenario above).
    And The provider of API "epsApiId" should match actor "<creator>"

    # Production key → routed to the unsecured production endpoint → no Basic header.
    When I act as "<consumer>"
    And I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
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
      | creator                   | consumer          |
      | admin                     | admin             |
      | admin@tenant1.com         | admin@tenant1.com |
      | publisherUser             | admin             |
      | publisherUser@tenant1.com | admin@tenant1.com |

  # OAUTH-type endpoint security (CLIENT_CREDENTIALS grant), PER KEY TYPE: both the production and the sandbox
  # backend legs are OAUTH-secured with a DISTINCT DCR-registered backend client, so the gateway obtains an access
  # token from the configured tokenUrl (the resident key manager's own /oauth2/token, reachable in-container at
  # https://localhost:9443) using that leg's client_credentials and injects it as "Authorization: Bearer <token>"
  # on the backend leg. The /sec route echoes that header verbatim, so the test can (a) prove a Bearer (and NOT the
  # "Basic " of BASIC endpoint security) was injected, and (b) INTROSPECT the echoed token — asserting active=true
  # and client_id == the DCR client of the matching key type, which is what proves the gateway minted a REAL token
  # against the RIGHT backend client rather than forwarding something opaque. A repeat request proves the arc is
  # not single-shot. Ports the client_credentials arcs of both AddEndPointSecurityPerTypeTestCase
  # (testAddEndpointSecurityForOauthForClientCredentialsGrantType — introspection, sandbox leg, 2nd request) and
  # ChangeEndPointSecurityPerTypeTestCase (testUpdateEndpointSecurityForOauthForClientCredentialsGrantType — the
  # config is switched to OAUTH via a GET-mutate-PUT embedding each DCR client's raw id/secret). Also confirms the
  # retrieved API redacts both stored clientSecrets.
  # The <creator> column runs the arc as a NON-ADMIN publisher too (legacy SUPER_TENANT_USER / TENANT_USER): the
  # API author is publisherUser while the devportal half stays with <consumer>, because publisherUser's devportal
  # token carries no application-manage scope (POST /applications -> 401) and so cannot drive the subscribe
  # composite. Introspection therefore also runs as <consumer>, which suits it — /oauth2/introspect authenticates
  # with the caller's own carbon credentials, not a scoped token.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityPerTypeTestCase @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: OAUTH client_credentials endpoint security injects and per key type introspects a gateway-minted backend Bearer as <creator>
    Given The system is ready
    And I have valid access tokens as "<consumer>"
    And The system is ready and I have valid publisher access tokens as "<creator>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_change.json" as "epoApiId" and deployed it
    When I publish the "apis" resource with id "epoApiId"
    Then The lifecycle status of API "epoApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "epoApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epoApiId"
    And I extract response field "context" and store it as "epoCtx"
    # The API is genuinely authored by <creator> (see the per-type BASIC scenario above).
    And The provider of API "epoApiId" should match actor "<creator>"

    # Register TWO backend OAuth clients (DCR) — one per key type — as the endpoint-security client_credentials
    # principals, so the introspected client_id identifies which leg the gateway used.
    When I register an OAuth client "epoProdBackendClient" as "epoProdBe"
    And I register an OAuth client "epoSandBackendClient" as "epoSandBe"

    # Switch BOTH endpoint_security legs to OAUTH client_credentials, tokenUrl = the in-container token endpoint.
    When I retrieve the "apis" resource with id "epoApiId"
    And I put the response payload in context as "epoPayload"
    When I put the following JSON payload in context as "epoNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"OAUTH","grantType":"CLIENT_CREDENTIALS","tokenUrl":"https://localhost:9443/oauth2/token","clientId":"{{epoProdBeClientId}}","clientSecret":"{{epoProdBeClientSecret}}","customParameters":{}},"sandbox":{"enabled":true,"type":"OAUTH","grantType":"CLIENT_CREDENTIALS","tokenUrl":"https://localhost:9443/oauth2/token","clientId":"{{epoSandBeClientId}}","clientSecret":"{{epoSandBeClientSecret}}","customParameters":{}}}}
    """
    When I update the "apis" resource "epoApiId" and "epoPayload" with configuration type "endpointConfig" and value:
    """
    epoNewEndpoint
    """
    Then The response status code should be 200
    # The retrieved API must round-trip the non-secret OAUTH config per key type and BLANK both stored backend
    # clientSecrets. Probed contract (4.7.0): the field is present and equal to "" — see the password-grant
    # scenario below. == "" is the legacy assertion and is stronger than a plaintext "should not contain".
    When I retrieve the "apis" resource with id "epoApiId"
    Then The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "{{epoProdBeClientId}}"
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "{{epoSandBeClientId}}"
    And The response should not contain "{{epoProdBeClientSecret}}"
    And The response should not contain "{{epoSandBeClientSecret}}"
    When I deploy the API with id "epoApiId"
    Then The response status code should be 201

    # Consumer half: the application, keys, subscription and token belong to <consumer>.
    When I act as "<consumer>"
    And I have set up application with keys, subscribed to API "epoApiId", and obtained access token for "epoSubId"
    Then The response status code should be 200

    # PRODUCTION key -> the backend receives a gateway-minted Bearer (OAUTH), NOT a Basic credential.
    When I invoke the API at gateway context "{{epoCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Bearer " within 90 seconds
    Then The response status code should be 200
    And The response should contain "Bearer "
    And The response should not contain "Basic "
    And I put the response payload in context as "epoProdInjected"
    # Introspecting the echoed token proves it is a live token issued to the PRODUCTION backend client.
    When I introspect the access token "epoProdInjected"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "{{epoProdBeClientId}}"

    # A REPEAT request on the same key must also succeed and carry a live injected token (legacy's "2nd request
    # also works" — a gateway that mishandled the cached backend token would fail only on the second call).
    When I invoke the API at gateway context "{{epoCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Bearer "
    And I put the response payload in context as "epoProdInjectedRepeat"
    When I introspect the access token "epoProdInjectedRepeat"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "{{epoProdBeClientId}}"

    # SANDBOX key on the SAME application -> the gateway uses the SANDBOX leg's client, proven by the introspected
    # client_id being the sandbox DCR client and NOT the production one.
    When I put the following JSON payload in context as "epoSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "epoSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epoSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "epoSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epoCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Bearer " within 90 seconds
    Then The response status code should be 200
    And I put the response payload in context as "epoSandInjected"
    When I introspect the access token "epoSandInjected"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "{{epoSandBeClientId}}"

    Examples:
      | creator                   | consumer          |
      | admin                     | admin             |
      | admin@tenant1.com         | admin@tenant1.com |
      | publisherUser             | admin             |
      | publisherUser@tenant1.com | admin@tenant1.com |

  # OAUTH-type endpoint security (PASSWORD grant), PER KEY TYPE: as the client_credentials case above, but the
  # gateway performs a resource-owner password grant against the tokenUrl using the API author's own credentials,
  # then injects the resulting Bearer on the backend leg — with a DISTINCT DCR backend client per key type. The
  # echoed token is INTROSPECTED (active=true, client_id == that leg's DCR client), a repeat request proves the arc
  # is not single-shot, and the sandbox leg proves per-key-type selection. Ports the password-grant arcs of both
  # ChangeEndPointSecurityPerTypeTestCase (testUpdateEndpointSecurityForOauthForPasswordGrantType) and
  # AddEndPointSecurityPerTypeTestCase (testAddEndpointSecurityForOauthForPasswordGrantType).
  # The <creator> column runs the arc as a NON-ADMIN publisher too — see the client_credentials scenario above for
  # why the devportal half must stay with <consumer>. The resource-owner credentials are the CREATOR's, captured
  # while it is the acting actor.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityPerTypeTestCase @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: OAUTH password-grant endpoint security injects and per key type introspects a gateway-minted backend Bearer as <creator>
    Given The system is ready
    And I have valid access tokens as "<consumer>"
    And The system is ready and I have valid publisher access tokens as "<creator>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_sec_change.json" as "eppApiId" and deployed it
    When I publish the "apis" resource with id "eppApiId"
    Then The lifecycle status of API "eppApiId" should be "Published"
    # Deploy-readiness gate: the gateway invokes below can only retry the REQUEST, and a lost runtime
    # propagation event is unrecoverable that way (it produced three 404 "Invalid URL" failures in this
    # runner). This re-fires the deploy if the artifact never lands (utils/Utils awaitWithRetry).
    And the "apis" resource "eppApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "eppApiId"
    And I extract response field "context" and store it as "eppCtx"
    # The API is genuinely authored by <creator> (see the per-type BASIC scenario above).
    And The provider of API "eppApiId" should match actor "<creator>"

    # Register TWO backend OAuth clients (one per key type) and capture the creator's resource-owner credentials.
    When I register an OAuth client "eppProdBackendClient" as "eppProdBe"
    And I register an OAuth client "eppSandBackendClient" as "eppSandBe"
    And I store the acting actor credentials as "eppRoUser" and "eppRoPass"

    # Switch BOTH endpoint_security legs to OAUTH password grant, tokenUrl = the in-container token endpoint.
    When I retrieve the "apis" resource with id "eppApiId"
    And I put the response payload in context as "eppPayload"
    When I put the following JSON payload in context as "eppNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"OAUTH","grantType":"PASSWORD","username":"{{eppRoUser}}","password":"{{eppRoPass}}","tokenUrl":"https://localhost:9443/oauth2/token","clientId":"{{eppProdBeClientId}}","clientSecret":"{{eppProdBeClientSecret}}","customParameters":{}},"sandbox":{"enabled":true,"type":"OAUTH","grantType":"PASSWORD","username":"{{eppRoUser}}","password":"{{eppRoPass}}","tokenUrl":"https://localhost:9443/oauth2/token","clientId":"{{eppSandBeClientId}}","clientSecret":"{{eppSandBeClientSecret}}","customParameters":{}}}}
    """
    When I update the "apis" resource "eppApiId" and "eppPayload" with configuration type "endpointConfig" and value:
    """
    eppNewEndpoint
    """
    Then The response status code should be 200
    # The retrieved API must round-trip the non-secret PASSWORD-grant config per key type (including the resource
    # owner USERNAME) and BLANK both secrets. Probed contract (4.7.0): the secret fields are PRESENT and equal to
    # the empty string — not stripped, not masked — for both the backend clientSecret and the resource owner's
    # password. Asserting == "" pins that exactly and is stronger than a "should not contain" on the credential
    # value, which for the resource-owner password would be unsound anyway: its value is the acting actor's carbon
    # password, which legitimately occurs elsewhere in the payload for some actors (e.g. admin/admin).
    When I retrieve the "apis" resource with id "eppApiId"
    Then The value of response field "endpointConfig.endpoint_security.production.password" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.password" should be ""
    And The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    Then The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "PASSWORD"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "{{eppProdBeClientId}}"
    And The value of response field "endpointConfig.endpoint_security.production.username" should be "{{eppRoUser}}"
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "PASSWORD"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "{{eppSandBeClientId}}"
    And The value of response field "endpointConfig.endpoint_security.sandbox.username" should be "{{eppRoUser}}"
    And The response should not contain "{{eppProdBeClientSecret}}"
    And The response should not contain "{{eppSandBeClientSecret}}"
    When I deploy the API with id "eppApiId"
    Then The response status code should be 201

    # Consumer half: the application, keys, subscription and token belong to <consumer>.
    When I act as "<consumer>"
    And I have set up application with keys, subscribed to API "eppApiId", and obtained access token for "eppSubId"
    Then The response status code should be 200

    # PRODUCTION key -> the backend receives a gateway-minted Bearer (OAUTH password grant), NOT a Basic credential.
    When I invoke the API at gateway context "{{eppCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Bearer " within 90 seconds
    Then The response status code should be 200
    And The response should contain "Bearer "
    And The response should not contain "Basic "
    And I put the response payload in context as "eppProdInjected"
    When I introspect the access token "eppProdInjected"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "{{eppProdBeClientId}}"

    # A REPEAT request on the same key must also succeed and carry a live injected token.
    When I invoke the API at gateway context "{{eppCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Bearer "
    And I put the response payload in context as "eppProdInjectedRepeat"
    When I introspect the access token "eppProdInjectedRepeat"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "{{eppProdBeClientId}}"

    # SANDBOX key on the SAME application -> the gateway uses the SANDBOX leg's client.
    When I put the following JSON payload in context as "eppSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "eppSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "eppSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "eppSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{eppCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Bearer " within 90 seconds
    Then The response status code should be 200
    And I put the response payload in context as "eppSandInjected"
    When I introspect the access token "eppSandInjected"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "{{eppSandBeClientId}}"

    Examples:
      | creator                   | consumer          |
      | admin                     | admin             |
      | admin@tenant1.com         | admin@tenant1.com |
      | publisherUser             | admin             |
      | publisherUser@tenant1.com | admin@tenant1.com |
