@cleanup
Feature: Gateway REST Endpoint Routing

  Gateway-plane routing of a published REST API to its backend endpoints, in both the super tenant and
  tenant1.com: production and sandbox tokens reach their own endpoints (and a token for an absent key type is
  refused), unsecured resources serve the production endpoint whatever key type is presented, a swapped endpoint
  URL repoints the API, custom-sequence backends serve through the gateway,
  the backend Location header is forwarded and a trailing slash routes identically.
  Teardown via the per-scenario cleanup hook.

  # Endpoint routing by token type: an API with distinct production and sandbox endpoints routes a PRODUCTION
  # token to the production endpoint and a SANDBOX token to the sandbox endpoint. Each endpoint echoes its path,
  # so the response body reveals which backend was hit. Ports the routing case of
  # InvokeAPIWithVariousEndpointsAndTokensTestCase.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensTestCase
  Scenario Outline: A production token routes to the production endpoint and a sandbox token to the sandbox endpoint as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_prodsandbox_api.json" as "psApiId" and deployed it
    When I publish the "apis" resource with id "psApiId"
    Then The lifecycle status of API "psApiId" should be "Published"
    When I retrieve the "apis" resource with id "psApiId"
    And I extract response field "context" and store it as "psContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "psApp"
    And I create an application with payload "psApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "psSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "psApiId" using application "createdAppId" with payload "psSub" as "psSubId"
    Then The response status code should be 201

    # PRODUCTION token → production endpoint (echo/prod)
    When I put the following JSON payload in context as "psProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "psProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "psProdToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "psProdToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{psContext}}/1.0.0/x" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    # The invoke gated on "echo/prod"; also pin the OTHER endpoint's signature ABSENT, so this phase's pass is
    # unreachable from the sandbox endpoint and the two phases cannot collapse into each other.
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should not contain "echo/sandbox"

    # SANDBOX token → sandbox endpoint (echo/sandbox)
    When I put the following JSON payload in context as "psSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "psSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "psSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "psSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{psContext}}/1.0.0/x" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/sandbox/x"
    And The response should not contain "echo/prod"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: a SANDBOX token offered to an API that has ONLY a production endpoint is rejected by the gateway
  # with runtime error 900901. Ports the no-sandbox-endpoint case of InvokeAPIWithVariousEndpointsAndTokens.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:negative @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensTestCase
  Scenario Outline: A sandbox token to a production-only API is rejected with 900901 as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_prodonly_api.json" as "poApiId" and deployed it
    When I publish the "apis" resource with id "poApiId"
    Then The lifecycle status of API "poApiId" should be "Published"
    When I retrieve the "apis" resource with id "poApiId"
    And I extract response field "context" and store it as "poContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "poApp"
    And I create an application with payload "poApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "poSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "poApiId" using application "createdAppId" with payload "poSub" as "poSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "poSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "poSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "poToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "poToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{poContext}}/1.0.0/x" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900901" message "Runtime Error" and description containing "Sandbox key offered to the API with no sandbox endpoint"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: a PRODUCTION token offered to an API that has ONLY a sandbox endpoint is rejected with 900901.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:negative @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensTestCase
  Scenario Outline: A production token to a sandbox-only API is rejected with 900901 as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_sandboxonly_api.json" as "soApiId" and deployed it
    When I publish the "apis" resource with id "soApiId"
    Then The lifecycle status of API "soApiId" should be "Published"
    When I retrieve the "apis" resource with id "soApiId"
    And I extract response field "context" and store it as "soContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "soApp"
    And I create an application with payload "soApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "soSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "soApiId" using application "createdAppId" with payload "soSub" as "soSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "soProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "soProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "soToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "soToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{soContext}}/1.0.0/x" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900901" message "Runtime Error" and description containing "Production key offered to the API with no production endpoint"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Unsecured resource, production-only API: once the resource's authType is None the gateway has no key type to
  # route on, so it serves the production endpoint and the SANDBOX token that was rejected 900901 becomes
  # irrelevant. Discriminating arm of :341 — the 403 disappears once the resource is unsecured.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensTestCase
  Scenario Outline: An unsecured resource on a production-only API serves production despite a sandbox token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_prodonly_api.json" as "u1ApiId" and deployed it
    And the "apis" resource "u1ApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "u1ApiId"
    Then The lifecycle status of API "u1ApiId" should be "Published"
    When I retrieve the "apis" resource with id "u1ApiId"
    And I extract response field "context" and store it as "u1Context"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "u1App"
    And I create an application with payload "u1App"
    Then The response status code should be 201
    When I put the following JSON payload in context as "u1Sub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "u1ApiId" using application "createdAppId" with payload "u1Sub" as "u1SubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "u1SandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "u1SandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "u1Token"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "u1Token"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "u1SandboxToken"
    When I put the following JSON payload in context as "u1ProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "u1ProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "u1ProdTokenRequest"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "u1ProdTokenRequest"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "u1ProdToken"

    # Secured baseline: the SANDBOX token is rejected 403 + 900901 (this prod-only API has no sandbox endpoint).
    # The baseline is what makes the post-flip 200 attributable — it proves the resource was genuinely secured.
    When I invoke the API at gateway context "{{u1Context}}/1.0.0/x" with method "GET" using access token "u1SandboxToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900901" message "Runtime Error" and description containing "Sandbox key offered to the API with no sandbox endpoint"
    When I invoke the API at gateway context "{{u1Context}}/1.0.0/x" with method "GET" using access token "u1ProdToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"

    # Flip the resource to authType None, redeploy, and gate on propagation before reading — a read before the new
    # config reaches the gateway returns a false 403.
    When I retrieve the "apis" resource with id "u1ApiId"
    And I put the response payload in context as "u1ApiPayload"
    When I update the "apis" resource "u1ApiId" and "u1ApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/x","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    And The response body should not be null
    When I deploy the API with id "u1ApiId"
    And the "apis" resource "u1ApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "u1ApiId" revision is deployed in the gateway

    # Unsecured reading: the SANDBOX token is ignored; the production endpoint answers. The body gate proves
    # echo/prod answered; the not-contains proves echo/sandbox (which does not exist) did not.
    When I invoke the API at gateway context "{{u1Context}}/1.0.0/x" with method "GET" using access token "u1SandboxToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should not contain "echo/sandbox"
    When I invoke the API at gateway context "{{u1Context}}/1.0.0/x" with method "GET" using access token "u1ProdToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should not contain "echo/sandbox"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Unsecured resource, sandbox-only API: with authType None there is no key type, so the gateway serves the only
  # endpoint that exists (sandbox), and the PRODUCTION token that was rejected 900901 becomes irrelevant.
  # Discriminating arm of :382 — the 403 disappears once the resource is unsecured.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensTestCase
  Scenario Outline: An unsecured resource on a sandbox-only API serves sandbox despite a production token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_sandboxonly_api.json" as "u2ApiId" and deployed it
    When I publish the "apis" resource with id "u2ApiId"
    Then The lifecycle status of API "u2ApiId" should be "Published"
    When I retrieve the "apis" resource with id "u2ApiId"
    And I extract response field "context" and store it as "u2Context"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "u2App"
    And I create an application with payload "u2App"
    Then The response status code should be 201
    When I put the following JSON payload in context as "u2Sub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "u2ApiId" using application "createdAppId" with payload "u2Sub" as "u2SubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "u2ProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "u2ProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "u2Token"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "u2Token"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "u2ProdToken"
    When I put the following JSON payload in context as "u2SandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "u2SandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "u2SandboxTokenRequest"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "u2SandboxTokenRequest"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "u2SandboxToken"

    # Secured baseline: the PRODUCTION token is rejected 403 + 900901 (this sandbox-only API has no production endpoint).
    When I invoke the API at gateway context "{{u2Context}}/1.0.0/x" with method "GET" using access token "u2ProdToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900901" message "Runtime Error" and description containing "Production key offered to the API with no production endpoint"
    When I invoke the API at gateway context "{{u2Context}}/1.0.0/x" with method "GET" using access token "u2SandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/sandbox/x"

    # Flip the resource to authType None, redeploy, and gate on propagation before reading.
    When I retrieve the "apis" resource with id "u2ApiId"
    And I put the response payload in context as "u2ApiPayload"
    When I update the "apis" resource "u2ApiId" and "u2ApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/x","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    And The response body should not be null
    When I deploy the API with id "u2ApiId"
    And the "apis" resource "u2ApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "u2ApiId" revision is deployed in the gateway

    # Unsecured reading: the PRODUCTION token is ignored; the sole (sandbox) endpoint answers. Body gate proves
    # echo/sandbox answered; the not-contains proves echo/prod (which does not exist) did not.
    When I invoke the API at gateway context "{{u2Context}}/1.0.0/x" with method "GET" using access token "u2ProdToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/sandbox/x"
    And The response should not contain "echo/prod"
    When I invoke the API at gateway context "{{u2Context}}/1.0.0/x" with method "GET" using access token "u2SandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/sandbox/x"
    And The response should not contain "echo/prod"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Crown-jewel: the unifying rule stated directly. Once the resource is unsecured, NEITHER a sandbox token NOR
  # the absence of a credential changes the endpoint — the gateway always serves the production endpoint. Secured
  # baselines (SANDBOX token -> echo/sandbox per :278; no credential -> 401/900902) make both post-flip 200s
  # attributable. Discriminating arm of :278.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensTestCase
  Scenario Outline: An unsecured resource ignores the presented key type and serves the production endpoint as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_prodsandbox_api.json" as "u3ApiId" and deployed it
    When I publish the "apis" resource with id "u3ApiId"
    Then The lifecycle status of API "u3ApiId" should be "Published"
    When I retrieve the "apis" resource with id "u3ApiId"
    And I extract response field "context" and store it as "u3Context"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "u3App"
    And I create an application with payload "u3App"
    Then The response status code should be 201
    When I put the following JSON payload in context as "u3Sub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "u3ApiId" using application "createdAppId" with payload "u3Sub" as "u3SubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "u3SandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "u3SandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "u3Token"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "u3Token"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "u3SandboxToken"
    When I put the following JSON payload in context as "u3ProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "u3ProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "u3ProdTokenRequest"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "u3ProdTokenRequest"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "u3ProdToken"

    # Secured baseline A: the SANDBOX token routes to the sandbox endpoint (echo/sandbox), matching :278.
    When I invoke the API at gateway context "{{u3Context}}/1.0.0/x" with method "GET" using access token "u3SandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/sandbox/x"
    And The response should not contain "echo/prod"
    When I invoke the API at gateway context "{{u3Context}}/1.0.0/x" with method "GET" using access token "u3ProdToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should not contain "echo/sandbox"

    # Secured baseline B: with NO credential the gateway rejects the call 401 + 900902 (Missing Credentials).
    When I invoke the API at gateway context "{{u3Context}}/1.0.0/x" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900902" message "Missing Credentials" and description containing "Make sure your API invocation call has a header"

    # Flip the resource to authType None, redeploy, and gate on propagation before re-reading both arms.
    When I retrieve the "apis" resource with id "u3ApiId"
    And I put the response payload in context as "u3ApiPayload"
    When I update the "apis" resource "u3ApiId" and "u3ApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/x","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    And The response body should not be null
    When I deploy the API with id "u3ApiId"
    And the "apis" resource "u3ApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "u3ApiId" revision is deployed in the gateway

    # Unsecured reading, SANDBOX token: the presented key type is ignored; the production endpoint answers. This
    # body-gated invoke is also the propagation gate for the no-credential read that follows.
    When I invoke the API at gateway context "{{u3Context}}/1.0.0/x" with method "GET" using access token "u3SandboxToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should not contain "echo/sandbox"
    When I invoke the API at gateway context "{{u3Context}}/1.0.0/x" with method "GET" using access token "u3ProdToken" and payload "" until response body contains "echo/prod" within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should not contain "echo/sandbox"

    # Unsecured reading, NO credential: the missing credential no longer 401s; the same production endpoint answers.
    When I invoke the API at gateway context "{{u3Context}}/1.0.0/x" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/prod/x"
    And The response should contain "echo/prod"
    And The response should not contain "echo/sandbox"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Swapping the endpoint URL of an ALREADY-DEPLOYED API repoints it at a DIFFERENT upstream. Ports
  # ChangeAPIEndPointURLTestCase and the API its AdvancedWebAppDeploymentConfig fixture provisions: a version-first
  # {version}/<ctx> API exposing GET / and GET /customers/{id}, invoked over the HTTP gateway listener.
  #
  # The API's context, resource set, revision lineage and subscription all stay put; only endpointConfig changes.
  # That is what separates this from default_version_routing (which routes to a changed backend on a NEW version)
  # and from the endpoint-security scenarios (which change credentials, not the upstream).
  #
  #   * BEFORE: node-customer-service (nodebackend:3001) answers GET /customers/123 with the XML customer, and has
  #     no root route, so GET / answers 404 there.
  #   * AFTER:  name-check1_SB (nodebackend:3009) serves ONLY GET /name, and the new endpoint URL carries that
  #     /name base path. The root resource therefore reaches "Hello WSO2 from File 1_Sandbox" only when the gateway
  #     composes the FULL new URL (host + base path); the old upstream can never answer the root with 200, so the
  #     until-200 poll on the root cannot be satisfied by a stale route.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:regression @dep:publisher @legacy:ChangeAPIEndPointURLTestCase @legacy:AdvancedWebAppDeploymentConfig
  Scenario Outline: Swapping a deployed API's endpoint URL repoints it to the new backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_change_endpoint_api.json" as "ceApiId" and deployed it
    And the "apis" resource "ceApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "ceApiId"
    Then The lifecycle status of API "ceApiId" should be "Published"
    When I retrieve the "apis" resource with id "ceApiId"
    And I extract response field "context" and store it as "ceContext"
    And I replace "{version}" with "1.0.0" in context "ceContext"
    When I have set up application with keys, subscribed to API "ceApiId", and obtained access token for "ceSubId"
    Then The response status code should be 200

    # BEFORE: the original upstream serves the customer resource, id and name pinned by the XML fragment.
    When I invoke the API at HTTP gateway context "{{ceContext}}/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" with content type "application/json" and request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    And The response should not contain "Hello WSO2"

    # Change ONLY the endpoint URL (production and sandbox both), then create and deploy a new revision.
    When I retrieve the "apis" resource with id "ceApiId"
    And I put the response payload in context as "ceApiPayload"
    When I put the following JSON payload in context as "ceNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3009/name"},"sandbox_endpoints":{"url":"http://nodebackend:3009/name"}}
    """
    When I update the "apis" resource "ceApiId" and "ceApiPayload" with configuration type "endpointConfig" and value:
      """
      ceNewEndpoint
      """
    Then The response status code should be 200
    And The value of response field "id" should be "{{ceApiId}}"
    When I deploy the API with id "ceApiId"
    Then The response status code should be 201
    And I wait until "apis" "ceApiId" revision is deployed in the gateway

    # AFTER: the root resource reaches the new upstream through the full new URL, and the old body is gone.
    When I invoke the API at HTTP gateway context "{{ceContext}}" with method "GET" using access token "generatedAccessToken" and payload "" with content type "application/json" and request header "Accept" set to "*/*" until response status code becomes 200 within 120 seconds
    Then The response status code should be 200
    And The response should contain "Hello WSO2 from File 1_Sandbox"
    And The response should not contain "<id>123</id><name>John</name></Customer>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An API whose endpoint type is a custom Synapse SEQUENCE backend: upload production + sandbox sequences (each
  # a payloadFactory returning a canned JSON), deploy, and invoke -> the gateway executes the sequence and returns
  # its response (no external backend). Ports APIEndpointTypeUpdateTestCase (sequence backend).
  @cap:gateway @feat:rest-invocation @rule:sequence-backend @type:regression @dep:publisher @legacy:APIEndpointTypeUpdateTestCase
  Scenario Outline: A custom sequence backend serves the API response through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_sequence_backend_api.json" in context as "seqApiPayload"
    And I create an "apis" resource with payload "seqApiPayload" as "seqApiId"
    Then The response status code should be 201
    # Upload the sequences BEFORE creating the revision so the deployed revision carries them.
    When I upload the sequence backend "artifacts/sequenceBackend/sequence_prod.xml" of type "PRODUCTION" for API "seqApiId"
    Then The response status code should be 200
    When I upload the sequence backend "artifacts/sequenceBackend/sequence_sand.xml" of type "SANDBOX" for API "seqApiId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "seqRev"
    """
    {"description":"seq revision"}
    """
    And I make a request to create a revision for "apis" resource "seqApiId" with payload "seqRev"
    When I put the following JSON payload in context as "seqDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "seqApiId" with payload "seqDeploy"
    Then The response status code should be 201
    When I publish the "apis" resource with id "seqApiId"
    Then The lifecycle status of API "seqApiId" should be "Published"
    When I retrieve the "apis" resource with id "seqApiId"
    And I extract response field "context" and store it as "seqContext"
    When I have set up application with keys, subscribed to API "seqApiId", and obtained access token for "seqSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{seqContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Sample Response" within 60 seconds
    Then The response status code should be 200
    # The sequence's payloadFactory emits the WHOLE body, so legacy asserted the body EQUALS
    # {"Response" : "Sample Response"} — a "contains" would also pass if the sequence output were wrapped in or
    # appended to anything else. Pinned as strict full-document JSON equality (no extra members, exact value)
    # against the expected file, which carries the sequence's literal format string verbatim.
    And The response body should equal the JSON file "artifacts/payloads/sequence_backend_expected_response.json"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The gateway forwards a backend Location response header: an ABSOLUTE Location is forwarded without a doubled
  # slash, and a RELATIVE Location is preserved. Ports LocationHeaderTestCase + RelativeUrlLocationHeaderTestCase.
  @cap:gateway @feat:header-transformation @rule:location-header @type:regression @dep:publisher @legacy:LocationHeaderTestCase @legacy:RelativeUrlLocationHeaderTestCase
  Scenario Outline: The gateway forwards the backend Location header (absolute and relative) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_location_api.json" as "locApiId" and deployed it
    When I publish the "apis" resource with id "locApiId"
    Then The lifecycle status of API "locApiId" should be "Published"
    When I retrieve the "apis" resource with id "locApiId"
    And I extract response field "context" and store it as "locContext"
    When I have set up application with keys, subscribed to API "locApiId", and obtained access token for "locSubId"
    Then The response status code should be 200
    # Absolute Location: forwarded, ends with /abc/domain, no doubled slash before it.
    When I invoke the API at gateway context "{{locContext}}/1.0.0/location-abs" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response header "Location" should contain "/abc/domain"
    And The response header "Location" should not contain "//abc/domain"
    # Relative Location: the backend's relative "/abc/domain" is resolved against the gateway base
    # (https://<gw>:8243/abc/domain) — so it is NOT relative in the client-visible response; what is preserved is
    # the path segment intact (verified: a "not contain ://" assertion fails because the client sees an absolute URL).
    When I invoke the API at gateway context "{{locContext}}/1.0.0/location-rel" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response header "Location" should contain "/abc/domain"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Trailing-slash equivalence: a deployed resource is routed the same WITH and WITHOUT a trailing slash — both
  # match the same uri-template resource and return 200 (APIMANAGER-769 / APIMANAGER-4194). Ports
  # URLMappingRESTTestCase (which asserted /urlmapping/1.0.0/test and .../test/ map to the same resource).
  @cap:gateway @feat:rest-invocation @rule:trailing-slash @type:regression @dep:publisher @legacy:URLMappingRESTTestCase
  Scenario Outline: A resource is routed identically with and without a trailing slash as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tsApiId" and deployed it
    When I publish the "apis" resource with id "tsApiId"
    Then The lifecycle status of API "tsApiId" should be "Published"
    When I retrieve the "apis" resource with id "tsApiId"
    And I extract response field "context" and store it as "tsContext"
    When I have set up application with keys, subscribed to API "tsApiId", and obtained access token for "tsSubId"
    Then The response status code should be 200
    # WITHOUT a trailing slash → 200 carrying the backend payload.
    When I invoke the API at gateway context "{{tsContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # WITH a trailing slash → routes to the SAME resource → the SAME backend payload. Equivalence is the subject,
    # so both forms must be shown to produce the same backend response; two bare 200s would not establish that
    # (the API's DELETE /customers/{id} also answers 200, with an empty body).
    When I invoke the API at gateway context "{{tsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
