@cleanup
Feature: Gateway Security Enforcement

  Gateway-plane runtime security enforcement (negatives), in both the super tenant and tenant1.com:
  an invalid bearer token is rejected (401), a valid token from an application that is NOT subscribed
  to the API is refused access (403), a blocked subscription is refused until it is unblocked, malformed and
  unmatched requests are answered by the gateway's error handling without echoing the offending input, and a
  resource's authType decides whether a token is required. The valid-token happy path is covered by
  gateway/rest-invocation; backend endpoint security is in gateway/endpoint_security, and credential-kind,
  password-change and role-bound scope enforcement in gateway/credential_enforcement. Teardown via the
  per-scenario cleanup hook.

  @cap:gateway @feat:security-enforcement @type:negative @rule:invalid-token @dep:publisher @legacy:APIMANAGERInvocationTestCase @legacy:InvalidTokenTestCase @legacy:TokenAPITestCase
  Scenario Outline: Invoke a published API with an invalid token is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_token_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "invalidAccessToken"
    """
    abcdefgh
    """
    # Use a declared API resource so the gateway reaches token validation rather than returning route-not-found first.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "invalidAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response field "description" should contain "Make sure you have provided the correct security credentials"
    # Legacy also sends the same invalid token to /most_popular on this API and expects authentication to reject it.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/most_popular" with method "GET" using access token "invalidAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The value of error response field "code" should be "900901"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
      | userKey1          |
      | userKey1@tenant1.com |

  @cap:gateway @feat:security-enforcement @type:negative @rule:no-subscription @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published API with an unsubscribed application's token is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
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

    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
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
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

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
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

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

    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
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
  # APISecurityTestCase (a distinct auth SCHEME, not tested elsewhere in the suite) — testInvokeBasicAuth's
  # valid-user/wrong-password pair plus testInvokeBasicAuthInvalidCredentials2, which is the DISTINCT failure of an
  # entirely UNKNOWN user (no such account) as opposed to a known user with the wrong password: the two take
  # different paths through the user store yet must both answer 401, so each is asserted here — plus legacy's
  # email-form-username fan-out, where the username ITSELF is an email address.
  #
  # The email-form rows do NOT depend on the absent email-username ACTOR infrastructure — an Identity principal
  # needing token issuance, publisher-plane resolution and per-actor cleanup — which stays out of scope. What legacy's testInvokeBasicAuth needs here is only a gateway Basic CREDENTIAL, so the
  # users are provisioned inline through UserAccountSteps and referenced as raw username/password strings. Legacy
  # applied no server config for them either, so no email-username TOML overlay is involved.
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
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    # Valid carbon user credentials → 200 carrying the BACKEND payload. The body is what makes this a credential
    # ACCEPTANCE assertion rather than a bare reachability one: it shows the Basic credential carried the request
    # all the way through to node-customer-service, which is the claim the two 401 refusals below are contrasted
    # against.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Email-form usernames as a gateway Basic credential (legacy's users[] fan-out, apisecUser2@wso2.com /
    # apisecUser2@abc.com) are covered in gateway/basic_auth_security.feature's email-shaped username cases,
    # which run in both standard and email-user blocks; they need
    # enable_email_domain=true, which only the IntegrationV2-EmailUserName block sets. That block's
    # infrastructure landed with blocker B9 (CLOSED 2026-08-11), superseding the earlier note here that called
    # the dimension out of scope.

    # The two REFUSAL paths. Both are 401 carrying 900901 "Invalid Credentials", and that identity is the point:
    # a known user with a wrong password and a user that does not exist at all take different paths through the user
    # store (BasicAuthCredentialValidator#validate returns not-authenticated either way) yet the gateway answers
    # BYTE-IDENTICALLY, so the response is not a user-enumeration oracle. Pinned as the exact code, never widened —
    # a future build that distinguished them (e.g. 900902 for the unknown user) would be a real information leak and
    # must fail here. The cases stay distinguishable by construction (which credential is sent), not by response.
    #
    # A valid user with the WRONG password → 401 / 900901.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" with password "totallyWrongPassword" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # An entirely UNKNOWN user (no such account in the tenant) → also 401 / 900901. Fully qualified with the tenant
    # domain: a domainless made-up name resolves against the super tenant and answers 403 on a tenant API instead.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "noSuchUser@<domain>" password "randomPassword" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"

    Examples:
      | actor             | domain       |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |

  # A large request body does NOT bypass gateway authentication: a POST of a ~1 MB body with an INVALID bearer token
  # is rejected. A 1 MB body exercises the large-request case without duplicating smaller payload sizes. The API
  # exposes a POST /reflect-body resource so the request reaches the auth check — a POST to a GET-only resource
  # returns 405 before auth (verified live). The request must return 401 or produce a recognized peer-close error
  # while uploading; unrelated I/O errors do not count as rejection.
  @cap:gateway @feat:security-enforcement @type:negative @rule:invalid-token @dep:publisher @legacy:InvalidAuthTokenLargePayloadTestCase
  Scenario Outline: A large payload with an invalid token is rejected at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "lpApiId" and deployed it
    When I publish the "apis" resource with id "lpApiId"
    Then The lifecycle status of API "lpApiId" should be "Published"
    When I retrieve the "apis" resource with id "lpApiId"
    And I extract response field "context" and store it as "lpContext"

    And the "apis" resource "lpApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "lpInvalidToken"
    """
    abcdefgh
    """
    # Warm-up: confirm the POST resource is routable and a small invalid-token POST is rejected (401).
    And I invoke the API at gateway context "{{lpContext}}/1.0.0/reflect-body" with method "POST" using access token "lpInvalidToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # The large body must NOT bypass auth — single shot, expect 401 or an explicit peer-close while uploading.
    When I invoke the API at gateway context "{{lpContext}}/1.0.0/reflect-body" with method "POST" using access token "lpInvalidToken" and a 1024 KB payload expecting authentication rejection

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Malformed XML robustness: an API whose POST operation carries a body-parsing policy (jsonToXML) forces the Synapse
  # message builder to run on the request. POSTing a malformed XML body (Content-Type application/xml) must be handled
  # cleanly — a server error, not a gateway crash / dropped connection. Ports MalformedRequestTest, which POSTs the
  # malformed body to getGatewayURLNhttp()+"response" without an Authorization header. That is NOT a bare path:
  # in the shared legacy suite a "/response" API is deployed by another test. The request matches it and its
  # sequence tries to BUILD the body
  # -> Woodstox WstxEOFException on the unclosed <request> -> fault sequence -> 500 (confirmed in CI: wire log shows
  # {api:Response_API_1} ... "HTTP/1.1 500 Internal Server Error"). A bare /response with no API deployed just 404s
  # (unmatched context) — expected, not a change. Rather than depend on a stray cross-test API, this isolated test
  # deploys its OWN body-building API (a jsonToXML request policy forces the same builder), reproducing the identical
  # malformed-parse 500. The authenticated outline below pins the parser cause (Synapse 601000 and Woodstox detail).
  # A separate no-token scenario below preserves the legacy credential path without relying on its shared /response API.
  @cap:gateway @feat:security-enforcement @type:negative @rule:malformed-request @dep:publisher @legacy:MalformedRequestTest
  Scenario Outline: An authenticated malformed XML request body is handled by the gateway message builder as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_jsontoxml_api.json" as "mfApiId" and deployed it
    When I publish the "apis" resource with id "mfApiId"
    Then The lifecycle status of API "mfApiId" should be "Published"
    When I retrieve the "apis" resource with id "mfApiId"
    And I extract response field "context" and store it as "mfContext"
    When I have set up application with keys, subscribed to API "mfApiId", and obtained access token for "mfSubId"
    Then The response status code should be 200
    And the "apis" resource "mfApiId" should be live on the gateway, redeploying if propagation is lost
    When I put the following JSON payload in context as "mfBody"
    """
    <request>Request<request>
    """
    # A malformed XML body (unclosed element) with an application/xml content type drives the builder → clean 500.
    And I invoke the API at gateway context "{{mfContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "mfBody" with content type "application/xml" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500
    # Pin the ROOT CAUSE, not just the status: the fault body carries Synapse error code 601000 and the exact
    # Woodstox parser message, proving the 500 is the malformed-XML build failure (not some incidental 500).
    And The error response should have code "601000" message "Runtime Error" and description containing "Unexpected EOF; was expecting a close tag for element <request>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:security-enforcement @type:negative @rule:malformed-request @dep:publisher @legacy:MalformedRequestTest
  Scenario: A malformed XML request without authentication reaches the gateway message builder
    Given The system is ready
    And I have valid access tokens as "admin"
    # Created WITHOUT an initial deployment: the only revision the gateway ever serves is the final authType None one,
    # so no invocation can land on a previous revision (401) or in a revision-swap gap (404).
    And I put JSON payload from file "artifacts/payloads/create_apim_jsontoxml_api.json" in context as "mfAnonCreatePayload"
    And I create an "apis" resource with payload "mfAnonCreatePayload" as "mfAnonApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "mfAnonApiId"
    And I put the response payload in context as "mfAnonApiPayload"
    And I update the "apis" resource "mfAnonApiId" and "mfAnonApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/reflect-body","verb":"POST","authType":"None","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[{"policyName":"jsonToXML","policyVersion":"v1","parameters":{}}],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I deploy the API with id "mfAnonApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mfAnonApiId"
    Then The lifecycle status of API "mfAnonApiId" should be "Published"
    And the "apis" resource "mfAnonApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "mfAnonApiId"
    And I extract response field "context" and store it as "mfAnonContext"
    Then The "POST" operation on "/reflect-body" of API "mfAnonApiId" should declare authType "None"
    When I put the following JSON payload in context as "mfAnonBody"
    """
    <request>Request<request>
    """
    # No Authorization header is sent. A 500 with the exact parser error proves authType None let the malformed body
    # reach the same jsonToXML request builder exercised by the authenticated cases above.
    And I invoke the API at gateway context "{{mfAnonContext}}/1.0.0/reflect-body" with method "POST" without authentication and payload "mfAnonBody" with content type "application/xml" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500
    And The error response should have code "601000" message "Runtime Error" and description containing "Unexpected EOF; was expecting a close tag for element <request>"

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
    And the "apis" resource "erApiId" should be live on the gateway, redeploying if propagation is lost

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

  # Ports ErrorMessageTypeTestCase — the gateway's DEFAULT error message TYPE, asserted on a request the gateway
  # cannot dispatch to any resource.
  #
  # MEASURED on this build (wso2am 4.7.0-SNAPSHOT / carbon-apimgt 9.33.171) before writing this scenario, by
  # deploying an API with only GET|DELETE /customers/{id} and hitting an undefined path under its context, over
  # HTTP :8280 and HTTPS :8243, with no credential and with a bogus bearer — all four give the SAME answer:
  #   404, Content-Type application/json; charset=UTF-8, body
  #   {"code":"404","type":"Status report","message":"Runtime Error","description":"No matching resource found for given API Request"}
  # Legacy expected 403 with a body opening {"fault":{ — BOTH are stale: 4.x emits a flat {"code":...} error
  # document, not the 2.x/3.x {"fault":{...}} wrapper, and an unmatched resource is 404 rather than 403. Legacy's
  # own testng.xml:188 entry is commented out, so that expectation never actually ran. The ACTUAL values are
  # pinned here exactly rather than the legacy ones being carried over unverified.
  #
  # This is a DIFFERENT axis from the unknown-CONTEXT 404 above, and the measurement is what proves it: an unknown
  # context answers message "Not Found" / description "The requested resource is not available.", whereas a known
  # context with an unmatched RESOURCE answers "Runtime Error" / "No matching resource found for given API
  # Request". Asserting the absence of the unknown-context wording is therefore the discriminating gate that makes
  # this a resource-dispatch assertion and not an accidental "the API was never deployed" 404.
  @cap:gateway @feat:fault-handling @rule:error-message-type @type:negative @dep:publisher @legacy:ErrorMessageTypeTestCase
  Scenario Outline: An unmatched resource path returns the JSON fault error document as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "emtApiId" and deployed it
    When I publish the "apis" resource with id "emtApiId"
    Then The lifecycle status of API "emtApiId" should be "Published"
    When I retrieve the "apis" resource with id "emtApiId"
    And I extract response field "context" and store it as "emtContext"
    And the "apis" resource "emtApiId" should be live on the gateway, redeploying if propagation is lost

    # Control: the DEFINED resource, invoked with no credential, is refused by the security handler (401/900902).
    # That answer is only reachable once the context is routable at the gateway, so it establishes the API really
    # is deployed before the unmatched-path assertion below is read.
    When I invoke the API at gateway context "{{emtContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The value of error response field "code" should be "900902"

    # The unmatched resource path under the SAME (live) context: 404, JSON media type, and the exact fault document.
    When I invoke the API at gateway context "{{emtContext}}/1.0.0/emtNoSuchResource/" with method "GET" without authentication until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The response header "Content-Type" should contain "application/json"
    # The typed fault step parses the envelope and compares EXACTLY, so a body that merely happens to contain
    # these strings elsewhere cannot satisfy it. "message" has no typed counterpart, so it stays a substring.
    And The fault response should have code "404" type "Status report" and description "No matching resource found for given API Request"
    And The response should contain "\"message\":\"Runtime Error\""
    # The discriminator: this is resource-level dispatch, NOT the unknown-context 404 (whose body reads
    # "The requested resource is not available.").
    And The response should not contain "The requested resource is not available."

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the operation-level auth-type enforcement of ChangeAuthTypeOfResourceTestCase. The legacy test updates a
  # resource through "Any", "Application", "Application_User" and "None" and invokes each WITH a valid token → 200
  # (which does not discriminate between the types). "Application_User" is not an OAS auth-type literal, so the
  # publisher stores it as "Application & Application User"; the "Application" row is ported by a scenario below.
  # The security-relevant distinction is the "None" auth type: a resource with authType "None" is invocable WITHOUT any token (200),
  # whereas the default "Application & Application User" resource requires one (401 without a token). This scenario
  # pins that discriminating behaviour. The resource is switched to authType None via an operations update + redeploy.
  #
  # Also carries the DECLARATION half of APISecurityTestCase#testValidateSecurityOfResources, which legacy asserted
  # on two separate fixture APIs (a security-disabled one and a security-enabled one): every DTO operation's
  # authType is the expected literal, AND the served OpenAPI definition's per-operation x-auth-type matches it
  # (non-null). Asserting both states on ONE API across the flip is strictly stronger than legacy's two static
  # snapshots — it proves the update actually propagates into the regenerated definition, which two fixed APIs
  # cannot show.
  @cap:gateway @feat:security-enforcement @rule:resource-auth-type @type:regression @dep:publisher @legacy:ChangeAuthTypeOfResourceTestCase @legacy:APISecurityTestCase
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

    # The initial authType is Any (Application & Application User). Prove the application-token arm is accepted
    # before the tokenless denial and before changing the operation to None.
    When I have set up application with keys, subscribed to API "atApiId", and obtained access token for "atSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{atContext}}/1.0.0/customers/123/" once with method "GET" using access token "generatedAccessToken" and payload ""
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Security ENABLED: every operation declares the default "Application & Application User" auth type, and the
    # served definition's x-auth-type matches it operation for operation. The matching application-token success
    # above and tokenless 401 below verify both sides of the Any contract.
    Then Every operation of API "atApiId" should declare authType "Application & Application User"
    When I retrieve the swagger of "apis" resource "atApiId"
    Then The response status code should be 200
    And I put the response payload in context as "atSecuredSwagger"
    And The definition stored as "atSecuredSwagger" should declare exactly the operations of API "atApiId"

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
    And the "apis" resource "atApiId" should be live on the gateway, redeploying if propagation is lost

    # Security DISABLED: every operation now declares "None", and the regenerated definition's x-auth-type
    # followed it (the extension is present and equal, not silently dropped).
    Then Every operation of API "atApiId" should declare authType "None"
    When I retrieve the swagger of "apis" resource "atApiId"
    Then The response status code should be 200
    And I put the response payload in context as "atNoneSwagger"
    And The definition stored as "atNoneSwagger" should declare exactly the operations of API "atApiId"

    # Now the resource is invocable WITHOUT a token (200) and the backend really served it — the same body
    # assertion the swagger-route twin scenario below already makes, for the same reason: it separates "the
    # unauthenticated call went through to node-customer-service" from any gateway-produced 200.
    When I invoke the API at gateway context "{{atContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Resource security disabled AT CREATION on two verbs. Ports the security-disabled fixture of
  # APISecurityTestCase#testValidateSecurityOfResources, whose API is created with GET and POST both authType
  # "None" (the scenario above reaches None only by an update, on GET alone). Both operations declare "None", the
  # served definition agrees operation for operation, and both verbs are invocable with no credential at all.
  @cap:gateway @feat:security-enforcement @rule:resource-auth-type @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An API created with authType None on GET and POST is invocable without a token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_authtype_none_api.json" as "anApiId" and deployed it
    When I publish the "apis" resource with id "anApiId"
    Then The lifecycle status of API "anApiId" should be "Published"
    And the "apis" resource "anApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "anApiId"
    And I extract response field "context" and store it as "anContext"
    Then The "GET" operation on "/customers/{id}" of API "anApiId" should declare authType "None"
    And The "POST" operation on "/reflect-body" of API "anApiId" should declare authType "None"
    And Every operation of API "anApiId" should declare authType "None"
    When I retrieve the swagger of "apis" resource "anApiId"
    Then The response status code should be 200
    And I put the response payload in context as "anSwagger"
    And The definition stored as "anSwagger" should declare exactly the operations of API "anApiId"

    When I invoke the API at gateway context "{{anContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I put the following JSON payload in context as "anBody"
    """
    {"probe":"authTypeNoneAtCreation"}
    """
    And I invoke the API at gateway context "{{anContext}}/1.0.0/reflect-body" with method "POST" without authentication and payload "anBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "authTypeNoneAtCreation"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the Application and Application_User rows of ChangeAuthTypeOfResourceTestCase: this sequentially updated
  # resource accepts a client-credentials token for Application and a password-grant user token for
  # Application_User. The Application update is read back from the operations and served definition; each update
  # is redeployed and gateway-ready before its matching token is invoked.
  @cap:gateway @feat:security-enforcement @rule:resource-auth-type @type:regression @dep:publisher @legacy:ChangeAuthTypeOfResourceTestCase
  Scenario Outline: An updated resource accepts the matching token for Application and Application_User as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "aaApiId" and deployed it
    When I publish the "apis" resource with id "aaApiId"
    Then The lifecycle status of API "aaApiId" should be "Published"
    And the "apis" resource "aaApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "aaApiId"
    And I extract response field "context" and store it as "aaContext"
    And I extract response field "name" and store it as "aaApiName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "aaAppPayload"
    And I create an application with payload "aaAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "aaKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "aaKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "aaSubPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "aaApiId" using application "createdAppId" with payload "aaSubPayload" as "aaSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "aaClientTokenPayload"
    """
    {"consumerSecret":"{{appConsumerSecret}}","validityPeriod":3600}
    """
    And I request an access token for application id "createdAppId" using payload "aaClientTokenPayload"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "aaApplicationToken"

    When I retrieve the "apis" resource with id "aaApiId"
    And I put the response payload in context as "aaPayload"
    When I update the "apis" resource "aaApiId" and "aaPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    Then The "GET" operation on "/customers/{id}" of API "aaApiId" should declare authType "Application"
    When I retrieve the swagger of "apis" resource "aaApiId"
    Then The response status code should be 200
    And I put the response payload in context as "aaSwagger"
    And The definition stored as "aaSwagger" should declare exactly the operations of API "aaApiId"

    And I mark the current end of the server log file "wso2carbon.log"
    When I deploy the API with id "aaApiId"
    Then The response status code should be 201
    And the "apis" resource "aaApiId" should be live on the gateway, redeploying if propagation is lost
    And The server log file "wso2carbon.log" should gain a line containing all of the following within 60 seconds
      | {{aaApiName}}                                       |
      | was added to the Synapse configuration successfully |

    When I invoke the API at gateway context "{{aaContext}}/1.0.0/customers/123/" with method "GET" using access token "aaApplicationToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Application_User requires a user access token rather than the application token above. Keep the same
    # application subscription and mint a password-grant token with the legacy PRODUCTION scope.
    When I retrieve the "apis" resource with id "aaApiId"
    And I put the response payload in context as "aaUserPayload"
    When I update the "apis" resource "aaApiId" and "aaUserPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application_User","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    Then The "GET" operation on "/customers/{id}" of API "aaApiId" should declare authType "Application & Application User"
    When I deploy the API with id "aaApiId"
    Then The response status code should be 201
    And the "apis" resource "aaApiId" should be live on the gateway, redeploying if propagation is lost
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "aaPasswordToken"
    When I invoke the API at gateway context "{{aaContext}}/1.0.0/customers/123/" once with method "GET" using access token "aaPasswordToken" and payload ""
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The SWAGGER route to the same authType flip. The scenario above sets authType on the API's `operations` array;
  # this one sets `x-auth-type` in the OPENAPI DEFINITION and PUTs the definition back, which is a different product
  # surface — it proves the definition→operations direction (APIM re-derives the URI templates from the uploaded
  # definition), where the scenario above proves operations→definition. Ports
  # DisableSecurityAndTryOutRESTResourceWithElkAnalyticsEnabledTestCase#testTurnOffSecurityAndInvokeGETResource.
  #
  # NO ELK-ANALYTICS BLOCK IS STOOD UP, deliberately. The legacy class applied an ElkAnalytics deployment.toml in its
  # BeforeClass and then asserted NOTHING analytics-specific: its only assertions are the swagger changed, the
  # invocation returned 200, and the body carried the customer payload — all reachable on default config. The toml
  # was pure backdrop, the same "config with no observable" shape as the open-tracing rows (see the note in
  # gateway/websocket_invocation.feature), so an ELK container/overlay would add cost and zero assertion. The class
  # was also never executed: its <class> entry is COMMENTED OUT at legacy testng.xml:30.
  #
  # Legacy asserted the response BODY (its accept: text/xml made the customer service answer XML); the v2 backend
  # answers JSON, so the same payload is pinned as "name":"John". The body matters: it proves the call reached the
  # backend rather than being short-circuited by the gateway.
  @cap:gateway @feat:security-enforcement @rule:resource-auth-type @type:regression @dep:publisher @legacy:DisableSecurityAndTryOutRESTResourceWithElkAnalyticsEnabledTestCase
  Scenario Outline: A resource switched to authType None through the swagger definition is invocable without a token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "swApiId" and deployed it
    When I publish the "apis" resource with id "swApiId"
    Then The lifecycle status of API "swApiId" should be "Published"
    When I retrieve the "apis" resource with id "swApiId"
    And I extract response field "context" and store it as "swContext"
    # Deploy propagation is at-most-once, so a dropped event can only be fixed by re-emitting it — the same gate
    # the sibling scenarios use. Without it BOTH token-less calls below just burn their retry window.
    And the "apis" resource "swApiId" should be live on the gateway, redeploying if propagation is lost

    # Baseline: the operation is secured, so a token-less call is refused.
    Then Every operation of API "swApiId" should declare authType "Application & Application User"
    When I invoke the API at gateway context "{{swContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Flip x-auth-type to None IN THE DEFINITION, for the GET only, and PUT the definition back.
    When I set x-auth-type "None" for path "/customers/{id}" verb "GET" in the swagger of API "swApiId"
    Then The response status code should be 200
    # The definition update PROPAGATED to the API's operations — the assertion the operations-route scenario cannot
    # make, and what the swagger PUT is actually for. Asserted per operation, not "every operation": the DELETE on
    # the same path must be UNTOUCHED, which is what proves the update was scoped to the one resource rather than
    # rewriting the API's security wholesale.
    Then The "GET" operation on "/customers/{id}" of API "swApiId" should declare authType "None"
    And The "DELETE" operation on "/customers/{id}" of API "swApiId" should declare authType "Application & Application User"
    When I deploy the API with id "swApiId"
    Then The response status code should be 201
    # The RE-deploy propagates at-most-once as well: a 201 only means the publisher accepted it. Without this the
    # invocation below would poll against the pre-update artifact (authType still Application & Application User)
    # until the window expired.
    And the "apis" resource "swApiId" should be live on the gateway, redeploying if propagation is lost

    # The resource is now invocable with NO Authorization header, and the backend really served it.
    When I invoke the API at gateway context "{{swContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
