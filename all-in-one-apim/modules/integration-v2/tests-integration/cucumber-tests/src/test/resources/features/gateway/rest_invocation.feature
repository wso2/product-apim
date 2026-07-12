@cleanup
Feature: Gateway REST API Invocation

  Gateway-plane runtime invocation of a published REST API: subscribe an application, obtain an access
  token, and invoke the API through the gateway expecting a 200. This is the gateway counterpart of the
  publisher-plane create/publish features (which assert only publisher outcomes). Runs in both the super
  tenant and tenant1.com as the tenant admin (the flow spans publish + subscribe + invoke). Teardown via
  the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @type:smoke @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published REST API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Capture the API's full gateway context (already carries /t/<tenant> for tenant APIs)
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and obtain an access token
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # Invoke through the gateway by full context path (retry while the gateway becomes eventually consistent)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIResourceWithTemplate #3 — an encoded URI path segment must be routed to the backend. FAITHFUL to
  # legacy: uri-template resource /{val} + a {uri.var.val} TEMPLATED ENDPOINT (…/echo/sub{uri.var.val}). The
  # gateway substitutes the path var into the endpoint AND appends the postfix → doubled backend path
  # /echo/sub<val>/<val> (exactly what legacy's bespoke Synapse backend hardcoded). The encoded segment must be
  # sent RAW (%28/%29 preserved) — the default invoke's HTTP client would decode it, which a uri-template
  # resource then 404s; the raw invoke sends it verbatim so the gateway routes it (as legacy's client did).
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIResourceWithTemplateTestCase @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: An encoded URI path segment is routed through a templated endpoint to the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_uritemplate_api.json" as "uriApiId" and deployed it
    When I publish the "apis" resource with id "uriApiId"
    Then The lifecycle status of API "uriApiId" should be "Published"
    When I retrieve the "apis" resource with id "uriApiId"
    And I extract response field "context" and store it as "uriApiContext"
    When I have set up application with keys, subscribed to API "uriApiId", and obtained access token for "uriSubscriptionId"
    Then The response status code should be 200
    # Plain segment: routes through the {uri.var.val} endpoint; backend gets the doubled path /echo/subplainseg/plainseg.
    When I invoke the API at gateway context "{{uriApiContext}}/1.0.0/plainseg" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/subplainseg/plainseg"
    # Encoded segment: sent RAW so %28/%29 reach the gateway verbatim; it routes (legacy uri-template + uri-var
    # endpoint) and the backend receives the encoding intact.
    When I invoke the API at raw gateway context "{{uriApiContext}}/1.0.0/S2222-0496%2815%2927436-0" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "S2222-0496%2815%2927436-0"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # L5: an API whose context uses a version-FIRST template ({version}/<ctx>) deploys to /{version}/<ctx> and is
  # invocable there — the version sits first in the URL, not appended. Ports PluggableVersioningStrategyTestCase
  # (the API is created with context "{version}/api"). The deployed context already carries the version, so it
  # is invoked directly (no appended /1.0.0) — that's what proves the version-first routing.
  @cap:gateway @feat:rest-invocation @rule:version-first @type:regression @dep:publisher @legacy:PluggableVersioningStrategyTestCase
  Scenario Outline: An API with a version-first context template routes at /{version}/context as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_version_first_api.json" as "vfApiId" and deployed it
    When I publish the "apis" resource with id "vfApiId"
    Then The lifecycle status of API "vfApiId" should be "Published"
    When I retrieve the "apis" resource with id "vfApiId"
    And I extract response field "context" and store it as "vfContext"
    # The publisher returns the context template verbatim (/{version}/ctx); the gateway resolves {version} at
    # deploy, so substitute it for the invocation URL.
    And I replace "{version}" with "1.0.0" in context "vfContext"
    When I have set up application with keys, subscribed to API "vfApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{vfContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Wave B-1: a REST resource ADDED to a deployed API becomes invocable at the gateway after redeploy; an
  # undefined path is refused. Ports AddEditRemoveRESTResourceTestCase.
  @cap:gateway @feat:rest-invocation @rule:dynamic-resource @type:regression @dep:publisher @legacy:AddEditRemoveRESTResourceTestCase
  Scenario Outline: A REST resource added to a deployed API becomes invocable at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "resApiId" and deployed it
    When I publish the "apis" resource with id "resApiId"
    Then The lifecycle status of API "resApiId" should be "Published"
    When I retrieve the "apis" resource with id "resApiId"
    And I extract response field "context" and store it as "resContext"
    When I have set up application with keys, subscribed to API "resApiId", and obtained access token for "resSubId"
    Then The response status code should be 200
    # Baseline: the existing GET resource is invocable.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # POST to an as-yet-undefined resource is refused (405 — path matches GET /customers/{id}, POST not allowed).
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/name" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 405 within 60 seconds
    Then The response status code should be 405
    # Add a POST /customers/name operation and redeploy.
    When I retrieve the "apis" resource with id "resApiId"
    And I put the response payload in context as "resApiPayload"
    When I update the "apis" resource "resApiId" and "resApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET"},{"target":"/customers/{id}","verb":"DELETE"},{"target":"/customers/name","verb":"POST"}]
      """
    Then The response status code should be 200
    When I deploy the API with id "resApiId"
    # The newly added POST resource is now invocable and routes to the backend.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/name" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Tom"
    # An undefined path is refused by the gateway.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/invalid" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # HTTP PATCH and HEAD method routing through the gateway. PATCH forwards to the backend echo route → 200. HEAD
  # on a GET-backed resource routes cleanly (no body, no historical NPE). Ports HttpPATCHSupportTestCase and
  # GIT2231HeadRequestNPEErrorTestCase. HEAD status is asserted at whatever the backend returns (verify-first).
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:HttpPATCHSupportTestCase @legacy:GIT2231HeadRequestNPEErrorTestCase
  Scenario Outline: PATCH and HEAD requests are routed through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_patchhead_api.json" as "phApiId" and deployed it
    When I publish the "apis" resource with id "phApiId"
    Then The lifecycle status of API "phApiId" should be "Published"
    When I retrieve the "apis" resource with id "phApiId"
    And I extract response field "context" and store it as "phContext"
    When I have set up application with keys, subscribed to API "phApiId", and obtained access token for "phSubId"
    Then The response status code should be 200

    # PATCH is forwarded to the backend echo route → 200 (empty body: the reflect-body route echoes any body)
    When I invoke the API at gateway context "{{phContext}}/1.0.0/reflect-body" with method "PATCH" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # HEAD on a GET-backed resource routes without the historical NPE
    When I invoke the API at gateway context "{{phContext}}/1.0.0/hello" with method "HEAD" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

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
    And The response should contain "900901"
    And The response should contain "no sandbox endpoint"

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
    And The response should contain "900901"
    And The response should contain "no production endpoint"

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
    When I publish the "apis" resource with id "seqApiId"
    Then The lifecycle status of API "seqApiId" should be "Published"
    When I retrieve the "apis" resource with id "seqApiId"
    And I extract response field "context" and store it as "seqContext"
    When I have set up application with keys, subscribed to API "seqApiId", and obtained access token for "seqSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{seqContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Sample Response" within 60 seconds
    Then The response status code should be 200
    And The response should contain "Sample Response"

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
    # Relative Location: preserved.
    When I invoke the API at gateway context "{{locContext}}/1.0.0/location-rel" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response header "Location" should contain "/abc/domain"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
