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

    # Invoke through the gateway by full context path (retry while the gateway becomes eventually consistent).
    # The BACKEND payload is pinned, not just the status: the API also exposes DELETE /customers/{id}, which the
    # backend answers 200 with an EMPTY body — so a status-only assertion cannot tell "GET reached
    # node-customer-service" from "the gateway dispatched to the wrong operation".
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Re-imported API is functionally invocable end-to-end: create an API, subscribe and INVOKE it (200) through
  # the gateway BEFORE export; then export -> delete -> re-import; deploy + publish the re-imported API,
  # re-subscribe, and INVOKE AGAIN (200) — proving the re-imported API is still routable/invocable through the
  # gateway, not merely recreated as a publisher artifact. Runs in the gateway block (backend up) as admin
  # (import needs apim:api_import_export). Ports APIImportExportTestCase#testNewAPIInvoke +
  # testNewAPIInvokeAfterImport.
 @cap:gateway @feat:rest-invocation @rule:import-export @type:regression @dep:publisher @legacy:APIImportExportTestCase
  Scenario Outline: A re-imported API is invocable through the gateway before and after import as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ieInvApiId" and deployed it
    When I publish the "apis" resource with id "ieInvApiId"
    Then The lifecycle status of API "ieInvApiId" should be "Published"
    When I retrieve the "apis" resource with id "ieInvApiId"
    And I extract response field "name" and store it as "ieInvApiName"
    And I extract response field "context" and store it as "ieInvContext"

    # INVOKE BEFORE export → 200 carrying the BACKEND payload (the original API is functionally invocable, i.e.
    # it actually reaches node-customer-service — a bare 200 would not distinguish that from a wrong dispatch).
    When I have set up application with keys, subscribed to API "ieInvApiId", and obtained access token for "ieInvSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{ieInvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # Export → delete → re-import the archive. The subscription must be removed first — an API with an active
    # subscription cannot be deleted (409 "active subscriptions exist").
    When I export the API "ieInvApiId" to an archive as "ieInvArchive"
    When I delete the subscription with id "ieInvSubId"
    Then The response status code should be 200
    When I delete the "apis" resource with id "ieInvApiId"
    Then The response status code should be 200
    When I import the exported archive "ieInvArchive" with additional properties "{}" as "ieInvImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{ieInvApiName}}" and store its id as "ieInvImportedApiId"
    Then The response status code should be 200

    # Deploy + publish the RE-IMPORTED API, re-subscribe, and INVOKE AGAIN → 200 (still invocable after import).
    When I deploy the API with id "ieInvImportedApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "ieInvImportedApiId"
    Then The lifecycle status of API "ieInvImportedApiId" should be "Published"
    When I retrieve the "apis" resource with id "ieInvImportedApiId"
    And I extract response field "context" and store it as "ieInvImportedContext"
    When I have set up application with keys, subscribed to API "ieInvImportedApiId", and obtained access token for "ieInvImportedSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{ieInvImportedContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

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
    # The body pins that the version-first URL actually routed THROUGH to the backend: routing is the subject
    # here, so a 200 that never reached node-customer-service would be a vacuous pass.
    When I invoke the API at gateway context "{{vfContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the invocation half of DynamicAPIContextTestCase — an API whose context carries {version} in the MIDDLE
  # of the path (api/developer/{version}) deploys with the version resolved into that position and is invocable at
  # api/developer/1.0.0/... . Distinct from the version-FIRST case above (where {version} leads); here the template
  # variable sits mid-path. The publisher returns the template verbatim; the gateway resolves {version} at deploy,
  # so it is substituted for the invocation URL. (The legacy search-by-templated-context assertion is a thin
  # publisher-search facet not ported here — the routing of the templated context is the regression subject.)
  @cap:gateway @feat:rest-invocation @rule:dynamic-context @type:regression @dep:publisher @legacy:DynamicAPIContextTestCase
  Scenario Outline: An API with a mid-path {version} context template is invocable at the resolved context as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_dynamic_context_api.json" as "dcApiId" and deployed it
    When I publish the "apis" resource with id "dcApiId"
    Then The lifecycle status of API "dcApiId" should be "Published"
    When I retrieve the "apis" resource with id "dcApiId"
    And I extract response field "context" and store it as "dcContext"
    And I replace "{version}" with "1.0.0" in context "dcContext"
    When I have set up application with keys, subscribed to API "dcApiId", and obtained access token for "dcSub"
    Then The response status code should be 200
    # As above: the mid-path {version} resolution is only proven if the call reaches the backend, so pin the body.
    When I invoke the API at gateway context "{{dcContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

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
    # Baseline: the existing GET resource is invocable AND serves the backend payload. The API's other operation
    # (DELETE /customers/{id}) answers 200 with an EMPTY body, so the body is what separates "GET matched" from
    # "some other operation on the same template matched".
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    # POST to an as-yet-undefined resource is refused (405 — path matches GET /customers/{id}, POST not allowed).
    # Body pinned too: the gateway distinguishes "URI matched, verb did not" from "no URI matched" with two
    # different messages, so status alone cannot tell the two dispatch outcomes apart (CORSRequestHandler
    # #handleResourceNotFound → METHOD_NOT_FOUND_ERROR_MSG here, RESOURCE_NOT_FOUND_ERROR_MSG below).
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/name" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 405 within 60 seconds
    Then The response status code should be 405
    And The response should contain "Method not allowed for given API resource"
    # Add a POST /customers/name operation and redeploy.
    When I retrieve the "apis" resource with id "resApiId"
    And I put the response payload in context as "resApiPayload"
    When I update the "apis" resource "resApiId" and "resApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET"},{"target":"/customers/{id}","verb":"DELETE"},{"target":"/customers/name","verb":"POST"}]
      """
    Then The response status code should be 200
    When I deploy the API with id "resApiId"
    Then The response status code should be 201
    # The newly added POST resource is now invocable and routes to the backend.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/name" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Tom"
    # An undefined path is refused by the gateway — status AND the gateway's own message (legacy's
    # INVALID_RESOURCE_INVOCATION constant, which is APIMgtGatewayConstants.RESOURCE_NOT_FOUND_ERROR_MSG). A bare
    # 404 would also be produced by the BACKEND for an unmapped upstream path, so the message is what proves the
    # refusal came from the gateway's resource dispatcher rather than from node-customer-service.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/invalid" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The response should contain "No matching resource found for given API Request"

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

    # PATCH is forwarded to the backend echo route → 200 whose body is the PATCH body echoed back. A marker body
    # is sent (rather than the empty one a status-only check allowed) because that is what makes the echo
    # observable: the API's other operations are GET/HEAD /hello, which answer 200 with "Hello World", so an empty
    # PATCH could not distinguish "the PATCH operation matched and its body was forwarded" from a wrong dispatch.
    When I put the following JSON payload in context as "phPatchBody"
    """
    {"patched":"by-gateway"}
    """
    When I invoke the API at gateway context "{{phContext}}/1.0.0/reflect-body" with method "PATCH" using access token "generatedAccessToken" and payload "phPatchBody" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"patched\":\"by-gateway\"}"

    # HEAD on a GET-backed resource routes without the historical NPE. No body assertion is possible or wanted
    # here — a HEAD response carries no entity body by definition, so the status IS the whole observable.
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
    # The invoke gated on "echo/prod"; also pin the OTHER endpoint's signature ABSENT, so this phase's pass is
    # unreachable from the sandbox endpoint and the two phases cannot collapse into each other.
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

  # Swapping the endpoint URL of an ALREADY-DEPLOYED API repoints it at a DIFFERENT upstream. Ports
  # ChangeAPIEndPointURLTestCase (testAPIInvocationBeforeChangeTheEndPointURL + testEditEndPointURL +
  # testInvokeAPIAfterChangeAPIEndPointURLWithNewEndPointURL, which are one arc on one API).
  #
  # The API's context, resource set, revision lineage and subscription all stay put; only endpointConfig changes.
  # That is what separates this from default_version_routing (which routes to a changed backend on a NEW version)
  # and from the endpoint-security scenarios (which change credentials, not the upstream).
  #
  # Legacy observed the switch with a second Synapse API returning "HelloWSO2" from name-check1_SB/name. That
  # backend exists here (nodebackend:3009) but serves ONLY GET /name, so it cannot answer this API's
  # /customers/{id} resource. The switch is therefore observed between two EXISTING backends, the same way the
  # API-product endpoint-change scenario does it (gateway/api_product_invocation.feature):
  #   * node-customer-service (nodebackend:3001, /jaxrs_basic/services/customers/customerservice/) answers
  #     GET /customers/123 with {"id":123,"name":"John"};
  #   * wildcard            (nodebackend:3017) answers ANY path/method with the plain text "Hello World".
  # Each phase asserts its OWN backend's signature present AND the other's absent — legacy's assertTrue(API2)
  # + assertFalse(API1) pair — so neither phase can pass against the wrong upstream and the "after" state is
  # unreachable from the "before" state. No new backend was added.
  @cap:gateway @feat:rest-invocation @rule:endpoint-routing @type:regression @dep:publisher @legacy:ChangeAPIEndPointURLTestCase
  Scenario Outline: Swapping a deployed API's endpoint URL repoints it to the new backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ceApiId" and deployed it
    When I publish the "apis" resource with id "ceApiId"
    Then The lifecycle status of API "ceApiId" should be "Published"
    When I retrieve the "apis" resource with id "ceApiId"
    And I extract response field "context" and store it as "ceContext"
    When I have set up application with keys, subscribed to API "ceApiId", and obtained access token for "ceSubId"
    Then The response status code should be 200

    # BEFORE: the original upstream (node-customer-service) serves the call.
    When I invoke the API at gateway context "{{ceContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    And The response should not contain "Hello World"

    # Change ONLY the endpoint URL (production and sandbox both) to the wildcard backend, then redeploy.
    When I retrieve the "apis" resource with id "ceApiId"
    And I put the response payload in context as "ceApiPayload"
    When I put the following JSON payload in context as "ceNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3017/"},"sandbox_endpoints":{"url":"http://nodebackend:3017/"}}
    """
    When I update the "apis" resource "ceApiId" and "ceApiPayload" with configuration type "endpointConfig" and value:
      """
      ceNewEndpoint
      """
    Then The response status code should be 200
    When I deploy the API with id "ceApiId"
    Then The response status code should be 201

    # AFTER: the SAME context, resource and token now reach the NEW upstream, and the old backend's body is gone.
    When I invoke the API at gateway context "{{ceContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Hello World" within 120 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    And The response should not contain "\"name\":\"John\""

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
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    # WITH a trailing slash → routes to the SAME resource → the SAME backend payload. Equivalence is the subject,
    # so both forms must be shown to produce the same backend response; two bare 200s would not establish that
    # (the API's DELETE /customers/{id} also answers 200, with an empty body).
    When I invoke the API at gateway context "{{tsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Reserved-character encoding during uri-template endpoint expansion. Ports the path-param cases of
  # UriTemplateReservedCharacterEncodingTest. The resource uses RFC-6570 reserved expansion (target "/{+val}"),
  # which the v4 publisher API accepts and which lets the gateway MATCH a path segment carrying a reserved char
  # (":"). The BACKEND ENDPOINT template then controls encoding: a plain {uri.var.val} percent-encodes the ":" to
  # %3A on expansion, while a reserved-expansion {+uri.var.val} preserves it. The reflect backend echoes the
  # expanded outbound path (verified live). Sent RAW (the ":" verbatim) since a normalizing client would alter it.
  @cap:gateway @feat:rest-invocation @rule:reserved-char-encoding @type:regression @dep:publisher @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: A reserved character in a templated endpoint is <encoding> by the <mode> expansion as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "rcApiId" and deployed it
    When I publish the "apis" resource with id "rcApiId"
    Then The lifecycle status of API "rcApiId" should be "Published"
    When I retrieve the "apis" resource with id "rcApiId"
    And I extract response field "context" and store it as "rcCtx"
    When I have set up application with keys, subscribed to API "rcApiId", and obtained access token for "rcSub"
    Then The response status code should be 200
    # Invoke a path segment carrying the reserved ':' — the endpoint expansion is echoed by the backend.
    When I invoke the API at raw gateway context "{{rcCtx}}/1.0.0/AAA:BBB" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expected>"

    Examples:
      | mode    | encoding         | payload                                                    | expected     | actor             |
      | default | percent-encoded  | artifacts/payloads/create_apim_reservedchar_default_api.json | subAAA%3ABBB | admin             |
      | default | percent-encoded  | artifacts/payloads/create_apim_reservedchar_default_api.json | subAAA%3ABBB | admin@tenant1.com |
      | escape  | preserved        | artifacts/payloads/create_apim_reservedchar_escape_api.json  | subAAA:BBB   | admin             |
      | escape  | preserved        | artifacts/payloads/create_apim_reservedchar_escape_api.json  | subAAA:BBB   | admin@tenant1.com |

  # Comma-separated values route to the backend, as a PATH segment AND as a QUERY parameter. Ports
  # URLMappingRESTTestCase's comma case, which the legacy COMMENTED OUT "until synapse is upgraded". The fix is a
  # reserved-expansion resource template — /courses/{+courseInfo} for the path, /qenc?queryParam={+value} for the
  # query — which the v4 publisher API accepts and which lets the gateway MATCH a comma-bearing value (a plain
  # template returns "No matching resource", verified live). The {+} endpoint preserves the commas to the backend.
  # Sent RAW (commas verbatim).
  @cap:gateway @feat:rest-invocation @rule:comma-params @type:regression @dep:publisher @legacy:URLMappingRESTTestCase
  Scenario Outline: A comma-separated value is routed to the backend via a <location> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "commaApiId" and deployed it
    When I publish the "apis" resource with id "commaApiId"
    Then The lifecycle status of API "commaApiId" should be "Published"
    When I retrieve the "apis" resource with id "commaApiId"
    And I extract response field "context" and store it as "commaCtx"
    When I have set up application with keys, subscribed to API "commaApiId", and obtained access token for "commaSub"
    Then The response status code should be 200
    When I invoke the API at raw gateway context "{{commaCtx}}/1.0.0/<invokePath>" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expected>"

    Examples:
      | location        | payload                                                | invokePath                          | expected                       | actor             |
      | path segment    | artifacts/payloads/create_apim_comma_params_api.json   | courses/course1,CE001,sem1          | course1,CE001,sem1             | admin             |
      | path segment    | artifacts/payloads/create_apim_comma_params_api.json   | courses/course1,CE001,sem1          | course1,CE001,sem1             | admin@tenant1.com |
      | query parameter | artifacts/payloads/create_apim_query_escape_api.json   | qenc?queryParam=course1,CE001,sem1  | queryParam=course1,CE001,sem1  | admin             |
      | query parameter | artifacts/payloads/create_apim_query_escape_api.json   | qenc?queryParam=course1,CE001,sem1  | queryParam=course1,CE001,sem1  | admin@tenant1.com |

  # Reserved-character encoding for a QUERY parameter (companion to the path-param scenario above). The resource
  # template binds the query param via reserved expansion (target "/qenc?queryParam={+value}", accepted by the v4
  # publisher API); the BACKEND ENDPOINT template then controls encoding of the bound value: a plain
  # {uri.var.value} percent-encodes ':' to %3A, while {+uri.var.value} preserves it. Ports the query-param cases of
  # UriTemplateReservedCharacterEncodingTest. The 'sub?queryParam=' prefix isolates the endpoint-expanded value from
  # the raw postfix the gateway also appends.
  @cap:gateway @feat:rest-invocation @rule:reserved-char-encoding @type:regression @dep:publisher @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: A reserved character in a query parameter is <encoding> by the <mode> expansion as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "qcApiId" and deployed it
    When I publish the "apis" resource with id "qcApiId"
    Then The lifecycle status of API "qcApiId" should be "Published"
    When I retrieve the "apis" resource with id "qcApiId"
    And I extract response field "context" and store it as "qcCtx"
    When I have set up application with keys, subscribed to API "qcApiId", and obtained access token for "qcSub"
    Then The response status code should be 200
    When I invoke the API at raw gateway context "{{qcCtx}}/1.0.0/qenc?queryParam=APIM:WSO2" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expected>"

    Examples:
      | mode    | encoding        | payload                                                   | expected                    | actor             |
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | sub?queryParam=APIM%3AWSO2  | admin             |
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | sub?queryParam=APIM%3AWSO2  | admin@tenant1.com |
      | escape  | preserved       | artifacts/payloads/create_apim_query_escape_api.json      | sub?queryParam=APIM:WSO2    | admin             |
      | escape  | preserved       | artifacts/payloads/create_apim_query_escape_api.json      | sub?queryParam=APIM:WSO2    | admin@tenant1.com |

  # Ports APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase — an API with TWO overlapping resource paths
  # distinguished only by HTTP verb (GET /comp/cartes/* and POST /comp/cartes/op/*) routes each verb to the correct
  # operation. A path that matches BOTH templates (/comp/cartes/op/123) resolves by verb: GET → the GET operation,
  # POST → the POST operation, each returning 200 from the wildcard backend. (Legacy asserted verb-specific echo
  # bodies against a mock backend; the v2 wildcard backend does not echo the verb/path, so the subject — correct
  # verb-based routing of overlapping paths — is asserted via the 200 on each verb.)
  @cap:gateway @feat:rest-invocation @rule:verb-routing @type:regression @dep:publisher @legacy:APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase
  Scenario Outline: Overlapping resource paths are routed by HTTP verb as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_similar_resources_api.json" as "svApiId" and deployed it
    When I publish the "apis" resource with id "svApiId"
    Then The lifecycle status of API "svApiId" should be "Published"
    When I retrieve the "apis" resource with id "svApiId"
    And I extract response field "context" and store it as "svContext"
    When I have set up application with keys, subscribed to API "svApiId", and obtained access token for "svSub"
    Then The response status code should be 200

    # GET on the shared path resolves to the GET operation.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/comp/cartes/op/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    # POST on the same path resolves to the POST operation.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/comp/cartes/op/123" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIResourceWithSpecialCharactersInvocation — a resource whose path NAME contains non-reserved special
  # characters (comma, hyphen, period, underscore, tilde per RFC 3986 §2.2: /special,-._~resource) is routed to the
  # backend and invoked successfully (200). The comma is the crux: it must NOT be treated as a delimiter (GraphQL
  # operations use a comma delimiter, but a plain REST resource name containing a comma must be used verbatim).
  @cap:gateway @feat:rest-invocation @rule:special-char-resource @type:regression @dep:publisher @legacy:APIResourceWithSpecialCharactersInvocation
  Scenario Outline: A resource path with non-reserved special characters is invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_special_char_resource_api.json" as "scApiId" and deployed it
    When I publish the "apis" resource with id "scApiId"
    Then The lifecycle status of API "scApiId" should be "Published"
    When I retrieve the "apis" resource with id "scApiId"
    And I extract response field "context" and store it as "scContext"
    When I have set up application with keys, subscribed to API "scApiId", and obtained access token for "scSub"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{scContext}}/1.0.0/special,-._~resource" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports LoadBalancedEndPointTestCase (commented-out in the legacy suite) — an API with a load-balanced production
  # endpoint (endpoint_type "load_balance", RoundRobin) distributes requests across THREE distinguishable backends
  # (File 1 / File 2 / File 3). Uses name-checkOne/Two/Three (ports 3014/3015/3016).
  #
  # Two assertions of increasing strength. First, reachability: each "until response body contains File N" poll
  # drives the round-robin until that backend answers, so all three are shown to serve traffic. Second — and this is
  # what closes the legacy's ORDER assertion — nine consecutive counted calls must round-robin, i.e. EVERY window of
  # three consecutive responses hits three DISTINCT members. That is strictly stronger than reachability (which a
  # degenerate 7:1:1 split would satisfy) while remaining start-offset independent, so it does not inherit the
  # flakiness of the legacy's fixed 1,2,3,1,2,3 sequence: the preceding polls have already moved the RR cursor to an
  # unknown position, and the sliding-window property holds for every rotation. The API is unique to this scenario,
  # so no sibling scenario shares its RR cursor.
  @cap:gateway @feat:rest-invocation @rule:load-balance @type:regression @dep:publisher @legacy:LoadBalancedEndPointTestCase
  Scenario Outline: A load-balanced endpoint distributes requests across all backends as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_loadbalanced_api.json" as "lbApiId" and deployed it
    When I publish the "apis" resource with id "lbApiId"
    Then The lifecycle status of API "lbApiId" should be "Published"
    When I retrieve the "apis" resource with id "lbApiId"
    And I extract response field "context" and store it as "lbContext"
    When I have set up application with keys, subscribed to API "lbApiId", and obtained access token for "lbSub"
    Then The response status code should be 200

    # Repeated invocations round-robin across the three backends — each distinct body is reached within a few calls.
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 1" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 2" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 3" within 60 seconds
    Then The response status code should be 200

    # ORDER: over nine consecutive calls every window of three hits three DISTINCT members — real round-robin, not
    # merely "all three are reachable".
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" 9 times round-robin across backend markers "File 1,File 2,File 3"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The SANDBOX axis of LoadBalancedEndPointTestCase#testRoundRobinAlgorithmInProductionAndSandboxEndpoints, which
  # the corpus never touched — the three `_SB` load-balance members (ports 3009/3011/3013) existed but no feature
  # referenced them, so nothing proved a load-balanced SANDBOX endpoint group is round-robinned at all. The API
  # carries BOTH groups with per-member AND per-key-type distinguishable bodies (production answers "File N",
  # sandbox "File N_Sandbox"), so a SANDBOX-key token asserted against the "_Sandbox" markers proves two things at
  # once: the key type selected the sandbox group, and that group is round-robinned across all three of its members.
  # The same nine-call sliding-window property as the production axis pins the ORDER.
  @cap:gateway @feat:rest-invocation @rule:load-balance @type:regression @dep:publisher @legacy:LoadBalancedEndPointTestCase
  Scenario Outline: A load-balanced SANDBOX endpoint group round-robins across all sandbox members as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_loadbalanced_sandbox_api.json" as "lbsApiId" and deployed it
    When I publish the "apis" resource with id "lbsApiId"
    Then The lifecycle status of API "lbsApiId" should be "Published"
    When I retrieve the "apis" resource with id "lbsApiId"
    And I extract response field "context" and store it as "lbsContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "lbsApp"
    And I create an application with payload "lbsApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "lbsSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "lbsApiId" using application "createdAppId" with payload "lbsSub" as "lbsSubId"
    Then The response status code should be 201

    # A SANDBOX key + token — the key type is what routes to the sandbox endpoint group.
    When I put the following JSON payload in context as "lbsSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "lbsSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "lbsSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "lbsSandboxToken"
    Then The response status code should be 200

    # Each of the three SANDBOX members is reached (and the "_Sandbox" suffix proves it is the sandbox group, not
    # the production one, that answered).
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 1_Sandbox" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 2_Sandbox" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 3_Sandbox" within 60 seconds
    Then The response status code should be 200

    # ORDER across the sandbox group: nine consecutive calls, every window of three distinct.
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" 9 times round-robin across backend markers "File 1_Sandbox,File 2_Sandbox,File 3_Sandbox"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIMANAGER5326CustomStatusMsgTestCase — the gateway must relay the BACKEND's HTTP status line verbatim,
  # including a NON-STANDARD reason phrase, rather than regenerating the canonical phrase for the status code. The
  # upstream is the raw-socket backend on :3024, which answers every request with the literal status line
  # "HTTP/1.1 400 Custom response".
  #
  # The reason phrase is the whole point: the status code alone (400) is what a gateway that had normalised the
  # phrase to "Bad Request" would also return, so a code-only assertion is blind to exactly the regression this
  # test exists for. The body is asserted too, proving the backend's response reached the client intact rather than
  # being replaced by a gateway-generated 400 fault.
  @cap:gateway @feat:rest-invocation @rule:status-line @type:regression @dep:publisher @legacy:APIMANAGER5326CustomStatusMsgTestCase
  Scenario Outline: The gateway relays a backend's non-standard status reason phrase as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_custom_status_api.json" as "csApiId" and deployed it
    When I publish the "apis" resource with id "csApiId"
    Then The lifecycle status of API "csApiId" should be "Published"
    When I retrieve the "apis" resource with id "csApiId"
    And I extract response field "context" and store it as "csContext"
    When I have set up application with keys, subscribed to API "csApiId", and obtained access token for "csSub"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{csContext}}/1.0.0/custom-status" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    # The backend's own reason phrase, NOT the canonical "Bad Request".
    And The response reason phrase should be "Custom response"
    # The backend's body arrived intact — this is not a gateway-generated fault.
    And The response should contain "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A tenant API is addressed at the gateway as /t/<domain>/<context>, and the domain is matched EXACTLY — so a
  # mis-cased variant ("/t/Tenant1.com/") resolves to no tenant and the gateway rejects the call with a 500.
  # The third leg is the actual subject: a failed tenant resolution must not poison tenant routing, so the
  # correctly-cased path must still serve the backend afterwards. Legs 1 and 2 are the control and the trigger.
  # Ports TenantDomainValidationTestCase#testAPIInvokeWithTenants (valid 200, mis-cased 500, valid 200 again).
  # Tenant-only, so a plain Scenario rather than the actor outline used above: a super-tenant API's context
  # carries no /t/ segment at all, so there is nothing to mis-case in the carbon.super row.
  # Subscribed on Unlimited (as the legacy did) so the three back-to-back invocations cannot hit a tier limit.
  @cap:gateway @feat:rest-invocation @rule:tenant-domain @type:regression @dep:publisher @legacy:TenantDomainValidationTestCase
  Scenario: A mis-cased tenant domain in the gateway path is rejected and leaves the tenant's routing intact
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tdvApiId" and deployed it
    When I publish the "apis" resource with id "tdvApiId"
    Then The lifecycle status of API "tdvApiId" should be "Published"

    # Capture the tenant API's full gateway context — it already carries the /t/tenant1.com prefix
    When I retrieve the "apis" resource with id "tdvApiId"
    And I extract response field "context" and store it as "tdvContext"
    When I have set up application with keys, subscribed to API "tdvApiId" with plan "Unlimited", and obtained access token for "tdvSub"
    Then The response status code should be 200

    # Leg 1 (control) — the correctly-cased tenant domain routes through to the backend
    When I invoke the API at gateway context "{{tdvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # Leg 2 (trigger) — the same resource with only the tenant domain's case changed is rejected. The context is
    # copied first so the correctly-cased value survives for leg 3 (the replace step rewrites in place).
    When I copy context value "tdvContext" to "tdvMisCasedContext"
    And I replace "/t/tenant1.com/" with "/t/Tenant1.com/" in context "tdvMisCasedContext"
    And I invoke the API at gateway context "{{tdvMisCasedContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500

    # Leg 3 (the point) — the correctly-cased path still reaches the backend, so tenant routing was not poisoned
    When I invoke the API at gateway context "{{tdvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
