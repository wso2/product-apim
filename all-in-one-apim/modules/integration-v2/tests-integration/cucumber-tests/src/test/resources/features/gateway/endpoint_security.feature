@cleanup
Feature: Gateway Backend Endpoint Security

  Gateway-plane backend endpoint security, in both the super tenant and tenant1.com: the gateway injects the
  credential an API's endpoint security configures towards a secured backend — Basic (including symbolic and
  special-character passwords), OAuth client_credentials and password grant (a gateway-minted backend Bearer,
  introspected per key type) and HTTP Digest — per key type, and keeps injecting the current credential after it
  is updated; a wrong Digest password is refused by the backend. Teardown via the per-scenario cleanup hook.

  # Ports ChangeEndPointSecurityOfAPITestCase (active in the legacy group2 endpoint-security block) — an API whose BACKEND endpoint is
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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
  # for each — 28 deploy cycles. Here the symbols are covered by two credentials whose exact base64 is asserted:
  # the create credential carries 17 of them, and the update credential carries all 28, so the PUT path that
  # legacy exercises sees every symbol:
  #   create: user / abcd-+={[}]|:;'<,>.?/efghijk  → base64 = dXNlcjphYmNkLSs9e1t9XXw6Oyc8LD4uPy9lZmdoaWpr
  #   update: user / abcd!@#$%^&*()_<>'+/=:;|-{[}],.?efghijk → base64 = dXNlcjphYmNkIUAjJCVeJiooKV88PicrLz06O3wte1t9XSwuP2VmZ2hpams=
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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"user","password":"abcd!@#$%^&*()_<>'+/=:;|-{[}],.?efghijk"},"sandbox":{"enabled":true,"type":"BASIC","username":"user","password":"abcd!@#$%^&*()_<>'+/=:;|-{[}],.?efghijk"}}}
    """
    When I update the "apis" resource "epsymApiId" and "epsymPayload" with configuration type "endpointConfig" and value:
    """
    epsymNewEndpoint
    """
    Then The response status code should be 200
    When I deploy the API with id "epsymApiId"
    Then The response status code should be 201
    And the "apis" resource "epsymApiId" should be live on the gateway, redeploying if propagation is lost

    # The backend now receives exactly base64(user:abcd!@#$%^&*()_<>'+/=:;|-{[}],.?efghijk), and no longer the create-path credential.
    When I invoke the API at gateway context "{{epsymCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "dXNlcjphYmNkIUAjJCVeJiooKV88PicrLz06O3wte1t9XSwuP2VmZ2hpams=" within 60 seconds
    Then The response status code should be 200
    And The response should contain "dXNlcjphYmNkIUAjJCVeJiooKV88PicrLz06O3wte1t9XSwuP2VmZ2hpams="
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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    And the "apis" resource "epcApiId" should be live on the gateway, redeploying if propagation is lost

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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    And the "apis" resource "epcApiId" should be live on the gateway, redeploying if propagation is lost

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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    And the "apis" resource "epcApiId" should be live on the gateway, redeploying if propagation is lost

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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    # Sandbox key → routed to the unsecured SANDBOX endpoint → the backend must receive NO Authorization
    # header at all. Asserted via the marker the /sec route emits when the header is absent: a bare
    # should-not-contain "Basic" would also pass on an empty body, i.e. if the request never reached the
    # backend, so it could not distinguish "no credential injected" from "never arrived".
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "no-authorization-header-received"

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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
    And the "apis" resource "epsApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epsApiId"
    Then The response should not contain "admin123"
    And I extract response field "context" and store it as "epsCtx"
    # The API is genuinely authored by <creator> (see the per-type scenario above).
    And The provider of API "epsApiId" should match actor "<creator>"

    # Production key → routed to the unsecured production endpoint → the backend must receive NO
    # Authorization header at all (see the marker rationale on the production-only scenario above).
    When I act as "<consumer>"
    And I have set up application with keys, subscribed to API "epsApiId", and obtained access token for "epsSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epsCtx}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "no-authorization-header-received"

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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"OAUTH","grantType":"CLIENT_CREDENTIALS","tokenUrl":"{{backendOAuthTokenUrl}}","clientId":"{{epoProdBeClientId}}","clientSecret":"{{epoProdBeClientSecret}}","customParameters":{}},"sandbox":{"enabled":true,"type":"OAUTH","grantType":"CLIENT_CREDENTIALS","tokenUrl":"{{backendOAuthTokenUrl}}","clientId":"{{epoSandBeClientId}}","clientSecret":"{{epoSandBeClientSecret}}","customParameters":{}}}}
    """
    When I update the "apis" resource "epoApiId" and "epoPayload" with configuration type "endpointConfig" and value:
    """
    epoNewEndpoint
    """
    Then The response status code should be 200
    Then The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    # Both the PUT response and the read-back response must expose the backend secrets as blank, matching the
    # legacy contract. The response must never expose the actual secret values (asserted below).
    When I retrieve the "apis" resource with id "epoApiId"
    Then The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "{{backendOAuthTokenUrl}}"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "{{epoProdBeClientId}}"
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "{{epoSandBeClientId}}"
    And The response should not contain "{{epoProdBeClientSecret}}"
    And The response should not contain "{{epoSandBeClientSecret}}"
    When I deploy the API with id "epoApiId"
    Then The response status code should be 201
    And the "apis" resource "epoApiId" should be live on the gateway, redeploying if propagation is lost

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
    # runner). This re-fires the deploy if the artifact never lands (utils/HealGate awaitOrHeal).
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
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"OAUTH","grantType":"PASSWORD","username":"{{eppRoUser}}","password":"{{eppRoPass}}","tokenUrl":"{{backendOAuthTokenUrl}}","clientId":"{{eppProdBeClientId}}","clientSecret":"{{eppProdBeClientSecret}}","customParameters":{}},"sandbox":{"enabled":true,"type":"OAUTH","grantType":"PASSWORD","username":"{{eppRoUser}}","password":"{{eppRoPass}}","tokenUrl":"{{backendOAuthTokenUrl}}","clientId":"{{eppSandBeClientId}}","clientSecret":"{{eppSandBeClientSecret}}","customParameters":{}}}}
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
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "{{backendOAuthTokenUrl}}"
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
    And the "apis" resource "eppApiId" should be live on the gateway, redeploying if propagation is lost

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

  # Ports the complex-password half of ChangeEndPointSecurityOfAPITestCase
  # (testInvokeGETResourceWithSecuredEndPointComplexPassword). The legacy loops 28 symbolic characters through the
  # endpoint_security password, redeploying the API for each — 28 redeploys for one escaping property. Ported as
  # THREE redeploys of one API, each carrying a password that packs the characters significant to ONE escaping
  # LAYER, so a failure still localises to the layer that broke:
  #
  #   1. XML-significant   — abcd&<>'efg   the API is published to the gateway as a synapse XML artifact, so an
  #                                        unescaped & or < in the password breaks artifact parsing outright;
  #   2. base64 alphabet   — abcd+/=efg    the credential is base64'd into the Authorization header, so +, / and the
  #                                        padding = are the bytes a double-encode, a URL-safe encode or a trim at
  #                                        "=" would corrupt;
  #   3. separator & meta  — abcd:;$%|efg  ":" is the user:pass separator itself and must NOT be read as one, and
  #                                        $ % | are placeholder / percent-encoding / pipe metacharacters.
  #
  # Every character is drawn from the legacy's own 28-character set, so this ports its coverage rather than widening
  # it; the two characters a JSON payload would itself have to escape (" and \) are legacy-absent too and are NOT
  # asserted here, since they would be new ground rather than a port (noted for the backlog).
  #
  # Each phase polls until the backend echoes the NEW base64 credential — that is the propagation gate (the
  # invocation is 200 with the old credential too, so an until-200 poll would race the redeploy) — and then asserts
  # the PREVIOUS phase's credential is gone, which is what proves the redeploy replaced the injected header rather
  # than the gateway serving a stale artifact or two credentials at once.
  #
  # Runs x2 tenants, extending the legacy class's single SUPER_TENANT_ADMIN data provider, so byte-level escaping
  # in the artifact and header is pinned independently for each tenant.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:ChangeEndPointSecurityOfAPITestCase
  Scenario Outline: Endpoint-security passwords containing special characters are injected intact as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_basicauth_api.json" as "spApiId" and deployed it
    When I publish the "apis" resource with id "spApiId"
    Then The lifecycle status of API "spApiId" should be "Published"
    When I retrieve the "apis" resource with id "spApiId"
    And I extract response field "context" and store it as "spContext"
    When I have set up application with keys, subscribed to API "spApiId", and obtained access token for "spSubId"
    Then The response status code should be 200

    # BASELINE — the fixture's simple credential (admin1:admin123) is injected.
    When I invoke the API at gateway context "{{spContext}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "YWRtaW4xOmFkbWluMTIz" within 60 seconds
    Then The response status code should be 200

    # --- 1. XML-significant characters: user:abcd&<>'efg -> dXNlcjphYmNkJjw+J2VmZw==
    When I retrieve the "apis" resource with id "spApiId"
    Then The response status code should be 200
    And I put the response payload in context as "spPayload1"
    When I put the following JSON payload in context as "spEndpointConfig1"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"user","password":"abcd&<>'efg"},"sandbox":{"enabled":true,"type":"BASIC","username":"user","password":"abcd&<>'efg"}}}
    """
    And I update the "apis" resource "spApiId" and "spPayload1" with configuration type "endpointConfig" and value:
    """
    spEndpointConfig1
    """
    Then The response status code should be 200
    When I deploy the API with id "spApiId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{spContext}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "dXNlcjphYmNkJjw+J2VmZw==" within 90 seconds
    Then The response status code should be 200
    # The stale baseline credential is no longer injected.
    And The response should not contain "YWRtaW4xOmFkbWluMTIz"

    # --- 2. base64 alphabet and padding: user:abcd+/=efg -> dXNlcjphYmNkKy89ZWZn
    When I retrieve the "apis" resource with id "spApiId"
    Then The response status code should be 200
    And I put the response payload in context as "spPayload2"
    When I put the following JSON payload in context as "spEndpointConfig2"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"user","password":"abcd+/=efg"},"sandbox":{"enabled":true,"type":"BASIC","username":"user","password":"abcd+/=efg"}}}
    """
    And I update the "apis" resource "spApiId" and "spPayload2" with configuration type "endpointConfig" and value:
    """
    spEndpointConfig2
    """
    Then The response status code should be 200
    When I deploy the API with id "spApiId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{spContext}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "dXNlcjphYmNkKy89ZWZn" within 90 seconds
    Then The response status code should be 200
    And The response should not contain "dXNlcjphYmNkJjw+J2VmZw=="

    # --- 3. separator and metacharacters: user:abcd:;$%|efg -> dXNlcjphYmNkOjskJXxlZmc=
    # The ":" inside the PASSWORD must not be taken for the user:pass separator — the decoded credential is still
    # user / abcd:;$%|efg, not user / abcd and a truncated remainder.
    When I retrieve the "apis" resource with id "spApiId"
    Then The response status code should be 200
    And I put the response payload in context as "spPayload3"
    When I put the following JSON payload in context as "spEndpointConfig3"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"user","password":"abcd:;$%|efg"},"sandbox":{"enabled":true,"type":"BASIC","username":"user","password":"abcd:;$%|efg"}}}
    """
    And I update the "apis" resource "spApiId" and "spPayload3" with configuration type "endpointConfig" and value:
    """
    spEndpointConfig3
    """
    Then The response status code should be 200
    When I deploy the API with id "spApiId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{spContext}}/1.0.0/sec" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "dXNlcjphYmNkOjskJXxlZmc=" within 90 seconds
    Then The response status code should be 200
    And The response should not contain "dXNlcjphYmNkKy89ZWZn"

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  # Ports DigestAuthenticationTestCase#testDigestAuthentication — the GATEWAY half of DIGEST endpoint security.
  # (The publisher-plane contract — type round-trip + password redaction — is already covered by
  # publisher/api-config, so nothing of it is repeated here.) The nodebackend /digest resource speaks HTTP Digest
  # (RFC 2617, MD5, qop=auth): the gateway's first backend call is answered 401 + WWW-Authenticate, and
  # DigestAuthMediator derives the response hash from the configured endpoint-security credential and replays the
  # request. A 200 naming the authenticated principal PROVES the handshake completed with THAT credential — the
  # backend has no allow-all route, so nothing else reaches 200. Each Examples row configures its OWN backend
  # principal (digestUser vs digestTenantUser) so the tenant1.com row asserts a DISTINCT authenticated user
  # instead of re-running the super-tenant assertion.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:regression @dep:publisher @legacy:DigestAuthenticationTestCase
  Scenario Outline: A backend secured with HTTP Digest auth is authenticated by the gateway as <digestUser> for <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put the following JSON payload in context as "epdgPayload"
    """
    {"name":"${UNIQUE:EndpointDigestAPI}","context":"${UNIQUE:endpointDigestContext}","version":"1.0.0","endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3005/digest"},"sandbox_endpoints":{"url":"http://nodebackend:3005/digest"},"endpoint_security":{"production":{"enabled":true,"type":"DIGEST","username":"<digestUser>","password":"<digestPass>"},"sandbox":{"enabled":true,"type":"DIGEST","username":"<digestUser>","password":"<digestPass>"}}},"policies":["Gold","Bronze","Unlimited"],"operations":[{"verb":"GET","target":"/auth","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[]}]}
    """
    And I create an "apis" resource with payload "epdgPayload" as "epdgApiId"
    Then The response status code should be 201
    When I deploy the API with id "epdgApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "epdgApiId"
    Then The lifecycle status of API "epdgApiId" should be "Published"
    # Deploy-readiness gate: a lost runtime propagation event cannot be recovered by retrying the request.
    And the "apis" resource "epdgApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epdgApiId"
    Then The response status code should be 200
    And I extract response field "context" and store it as "epdgCtx"
    When I have set up application with keys, subscribed to API "epdgApiId", and obtained access token for "epdgSubId"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{epdgCtx}}/1.0.0/auth" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # WHICH principal completed the handshake — not merely that something succeeded.
    And The value of response field "authenticatedUser" should be "<digestUser>"
    And The value of response field "scheme" should be "Digest"
    And The value of response field "realm" should be "wso2-digest-backend"
    # The digest-uri the gateway signed is the backend path (endpoint path + REST_URL_POSTFIX), not the gateway one.
    And The value of response field "digestUri" should be "/digest/auth"

    Examples:
      | actor             | digestUser       | digestPass       |
      | admin             | digestUser       | digestPass       |
      | admin@tenant1.com | digestTenantUser | digestTenantPass |

  # Negative half of the same arc: the DIGEST endpoint-security credential is configured with a WRONG password, so
  # the response hash the gateway computes does not match and the backend refuses the replayed request. MEASURED,
  # not guessed: the gateway relays the backend's refusal verbatim — HTTP 401 with the backend's plain-text body
  # (it does not translate it into a gateway 500 or an APIM error code). The gate is discriminating on both sides:
  # the deploy-readiness gate proves the artifact really is on the gateway (so a never-routable API cannot pass as
  # a rejection), and the invoke waits for the exact refusal status rather than for a 200. The backend names the
  # principal it rejected, so each tenant row asserts a DISTINCT rejection.
  @cap:gateway @feat:security-enforcement @rule:endpoint-security @type:negative @dep:publisher @legacy:DigestAuthenticationTestCase
  Scenario Outline: A wrong HTTP Digest endpoint-security password is refused at the backend for <digestUser> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put the following JSON payload in context as "epdgnPayload"
    """
    {"name":"${UNIQUE:EndpointDigestBadAPI}","context":"${UNIQUE:endpointDigestBadContext}","version":"1.0.0","endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3005/digest"},"sandbox_endpoints":{"url":"http://nodebackend:3005/digest"},"endpoint_security":{"production":{"enabled":true,"type":"DIGEST","username":"<digestUser>","password":"notTheDigestPassword"},"sandbox":{"enabled":true,"type":"DIGEST","username":"<digestUser>","password":"notTheDigestPassword"}}},"policies":["Gold","Bronze","Unlimited"],"operations":[{"verb":"GET","target":"/auth","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[]}]}
    """
    And I create an "apis" resource with payload "epdgnPayload" as "epdgnApiId"
    Then The response status code should be 201
    When I deploy the API with id "epdgnApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "epdgnApiId"
    Then The lifecycle status of API "epdgnApiId" should be "Published"
    # Discriminating gate: the artifact is confirmed present on the gateway BEFORE the negative invoke, so the
    # refusal below cannot be an unrouted API in disguise.
    And the "apis" resource "epdgnApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "epdgnApiId"
    Then The response status code should be 200
    And I extract response field "context" and store it as "epdgnCtx"
    When I have set up application with keys, subscribed to API "epdgnApiId", and obtained access token for "epdgnSubId"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{epdgnCtx}}/1.0.0/auth" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # The refusal is the BACKEND's digest rejection of this row's principal, not a gateway auth failure.
    And The response should contain "response hash mismatch for <digestUser>"
    And The response should not contain "900901"

    Examples:
      | actor             | digestUser       |
      | admin             | digestUser       |
      | admin@tenant1.com | digestTenantUser |
