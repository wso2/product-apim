@cleanup
Feature: Gateway CORS

  Gateway-plane CORS handling: an API with CORS enabled (a specific allowed origin plus allow-credentials)
  returns the Access-Control-Allow-Origin and Access-Control-Allow-Credentials response headers when invoked
  with a matching Origin. Runs in the gateway block (backend + runtime invocation), in both tenants. Ports
  CORSAccessControlAllowCredentialsHeaderTestCase (the CORS-header case; the SDK-generation case is separate).

  @cap:gateway @feat:cors @type:regression @dep:publisher @dep:devportal @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario Outline: CORS allow-origin and allow-credentials headers are returned for a matching origin as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_api.json" as "corsApiId" and deployed it
    When I publish the "apis" resource with id "corsApiId"
    Then The lifecycle status of API "corsApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsApiId"
    And I extract response field "context" and store it as "corsContext"
    When I have set up application with keys, subscribed to API "corsApiId", and obtained access token for "corsSubId"
    Then The response status code should be 200

    # Invoke with a matching Origin — the gateway echoes it in Access-Control-Allow-Origin and, because the API
    # enables allow-credentials, returns Access-Control-Allow-Credentials: true.
    When I invoke the API at gateway context "{{corsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response should contain the header "Access-Control-Allow-Credentials" with value "true"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the pre-flight cases of CORSHeadersTestCase — an OPTIONS pre-flight request to a CORS-enabled API is
  # answered by the gateway (200) with the configured Access-Control-Allow-Origin / -Allow-Methods / -Allow-Headers
  # response headers. The API enables a specific origin (http://localhost) with the method and header allow-lists
  # from its CORS config. Runs in the gateway block, both tenants.
  # Also ports the REST facet of CORSHeadersForAllAPITypesTestCase: the pre-flight echoes a custom configured
  # allow-header (X-BrowserSessionID), and a NORMAL (non pre-flight) request does NOT carry the pre-flight-only
  # Access-Control-Allow-Methods / -Allow-Headers response headers. That negative is asserted once here (it is a
  # property of the CORS handler, independent of API type) rather than repeated per type.
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:CORSHeadersTestCase @legacy:CORSHeadersForAllAPITypesTestCase
  Scenario Outline: A CORS pre-flight OPTIONS request returns the configured allow-origin, allow-methods and allow-headers as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_api.json" as "corsPfApiId" and deployed it
    When I publish the "apis" resource with id "corsPfApiId"
    Then The lifecycle status of API "corsPfApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsPfApiId"
    And I extract response field "context" and store it as "corsPfContext"
    When I have set up application with keys, subscribed to API "corsPfApiId", and obtained access token for "corsPfSubId"
    Then The response status code should be 200

    # Pre-flight OPTIONS with a matching Origin and requested method (GET) → 200 with the CORS allow headers. No
    # access token is needed for a pre-flight (the gateway answers it before auth). The Access-Control-Request-Method
    # header is required: the CORS handler matches the target resource by the requested method, so without it the
    # request finds no acceptable resource and is rejected 405.
    When I send a CORS preflight to gateway context "{{corsPfContext}}/1.0.0/customers/123/" with origin "http://localhost" and request method "GET" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "GET"
    And The response header "Access-Control-Allow-Headers" should contain "authorization"
    # The custom configured allow-header is echoed in the pre-flight allow-headers.
    And The response header "Access-Control-Allow-Headers" should contain "X-BrowserSessionID"

    # Negative: a NORMAL (non pre-flight) request carrying an Origin gets a 200 but MUST NOT carry the
    # pre-flight-only Access-Control-Allow-Methods / -Allow-Headers response headers.
    When I invoke the API at gateway context "{{corsPfContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain the header "Access-Control-Allow-Methods"
    And The response should not contain the header "Access-Control-Allow-Headers"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Default CORS (no corsConfiguration on the API): the gateway still answers a pre-flight OPTIONS with a wildcard
  # Access-Control-Allow-Origin (*) and an Access-Control-Allow-Methods that reflects ONLY the API's actual
  # resource verbs — the single-GET API returns GET but NOT POST/PUT/DELETE/PATCH. Ports the default-CORS case of
  # APIMANAGER3965TestCase (distinct from the configured-CORS pre-flight above, which pins a specific origin and a
  # configured method allow-list).
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:APIMANAGER3965TestCase
  Scenario Outline: A default-CORS API's pre-flight returns wildcard origin and only the API's own methods as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_default_cors_api.json" as "dcApiId" and deployed it
    When I publish the "apis" resource with id "dcApiId"
    Then The lifecycle status of API "dcApiId" should be "Published"
    When I retrieve the "apis" resource with id "dcApiId"
    And I extract response field "context" and store it as "dcContext"
    When I have set up application with keys, subscribed to API "dcApiId", and obtained access token for "dcSubId"
    Then The response status code should be 200

    # Default-CORS pre-flight: wildcard allow-origin, allow-methods reflects only the API's GET resource.
    When I send a CORS preflight to gateway context "{{dcContext}}/1.0.0/customers/123/" with origin "http://localhost" and request method "GET" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "*"
    And The response header "Access-Control-Allow-Methods" should contain "GET"
    And The response header "Access-Control-Allow-Methods" should not contain "POST"
    And The response header "Access-Control-Allow-Methods" should not contain "PUT"
    And The response header "Access-Control-Allow-Methods" should not contain "DELETE"
    And The response header "Access-Control-Allow-Methods" should not contain "PATCH"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports CORSBackendTrafficRouteTestCase — when the API's gateway CORS handling is DISABLED and the API exposes an
  # OPTIONS resource, the gateway does NOT answer the pre-flight itself but ROUTES it to the backend, so the
  # backend's own CORS response headers pass through. The node-people-service OPTIONS /options route returns
  # Access-Control-Allow-Methods "GET, POST, DELETE, PUT, OPTIONS, HEAD" and Access-Control-Allow-Headers
  # "Content-Type"; a 200 carrying those backend headers proves the OPTIONS was routed to the backend. (The legacy
  # asserted a different backend's exact header values; here we assert THIS backend's values.)
  @cap:gateway @feat:cors @rule:backend-cors @type:regression @dep:publisher @dep:devportal @legacy:CORSBackendTrafficRouteTestCase
  Scenario Outline: With gateway CORS disabled an OPTIONS request is routed to the backend and its CORS headers pass through as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_backend_cors_api.json" as "bcApiId" and deployed it
    When I publish the "apis" resource with id "bcApiId"
    Then The lifecycle status of API "bcApiId" should be "Published"
    When I retrieve the "apis" resource with id "bcApiId"
    And I extract response field "context" and store it as "bcContext"
    When I have set up application with keys, subscribed to API "bcApiId", and obtained access token for "bcSubId"
    Then The response status code should be 200

    # OPTIONS routed to the backend (gateway CORS disabled) → the backend's CORS headers are returned.
    When I invoke the API at gateway context "{{bcContext}}/1.0.0/options" with method "OPTIONS" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response header "Access-Control-Allow-Methods" should contain "OPTIONS"
    And The response header "Access-Control-Allow-Headers" should contain "Content-Type"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the non-REST API-type facets of CORSHeadersForAllAPITypesTestCase: with CORS enabled (a specific origin
  # plus the custom X-BrowserSessionID allow-header), a pre-flight OPTIONS to a SOAP / GraphQL / SSE API is answered
  # by the gateway (200) with the configured allow-origin / -methods / -headers, and the allow-headers echoes
  # X-BrowserSessionID. The SSE case guards the Async-type pre-flight header regression (the reason the legacy class
  # exists). The legacy "Async API imported from a file" case was itself an SSE-typed API exposed over HTTP; the v2
  # asyncapi import path produces advertise-only, non-gateway-routed APIs, so that case collapses into this SSE
  # scenario. WebSub/WebHook APIs are intentionally not covered — their gateway resource only accepts the
  # subscription method and rejects the pre-flight OPTIONS before it reaches the CORS handler. Runs in both tenants.
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:CORSHeadersForAllAPITypesTestCase
  Scenario Outline: A CORS pre-flight OPTIONS to a SOAP API returns the configured allow-headers incl. X-BrowserSessionID as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_soap_api.json" as "corsSoapApiId" and deployed it
    When I publish the "apis" resource with id "corsSoapApiId"
    Then The lifecycle status of API "corsSoapApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsSoapApiId"
    And I extract response field "context" and store it as "corsSoapContext"
    When I have set up application with keys, subscribed to API "corsSoapApiId", and obtained access token for "corsSoapSubId"
    Then The response status code should be 200

    # SOAP resource verb is POST, so the pre-flight declares POST as the requested method.
    When I send a CORS preflight to gateway context "{{corsSoapContext}}/1.0.0" with origin "http://localhost" and request method "POST" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "POST"
    And The response header "Access-Control-Allow-Headers" should contain "X-BrowserSessionID"

    # Negative: a NORMAL (non pre-flight) POST carrying an Origin must NOT carry the pre-flight-only headers.
    When I invoke the API at gateway context "{{corsSoapContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain the header "Access-Control-Allow-Methods"
    And The response should not contain the header "Access-Control-Allow-Headers"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:CORSHeadersForAllAPITypesTestCase
  Scenario Outline: A CORS pre-flight OPTIONS to a GraphQL API returns the configured allow-headers incl. X-BrowserSessionID as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_cors_graphql_api.json" in context as "corsGraphQLPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "corsGraphQLPayload" as "corsGraphQLApiId"
    Then The response status code should be 201

    When I retrieve the "apis" resource with id "corsGraphQLApiId"
    And I put the response payload in context as "corsGraphQLRetrievedPayload"
    And I extract response field "context" and store it as "corsGraphQLContext"

    When I put the following JSON payload in context as "corsGraphQLRevisionPayload"
    """
    {
      "description":"Initial Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "corsGraphQLApiId" with payload "corsGraphQLRevisionPayload"
    And I deploy revision "revisionId" of "apis" resource "corsGraphQLApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "corsGraphQLRetrievedPayload"
    And I publish the "apis" resource with id "corsGraphQLApiId"
    Then The lifecycle status of API "corsGraphQLApiId" should be "Published"

    When I have set up application with keys, subscribed to API "corsGraphQLApiId", and obtained access token for "corsGraphQLSubId"
    Then The response status code should be 200

    # GraphQL is invoked over POST, so the pre-flight declares POST as the requested method.
    When I send a CORS preflight to gateway context "{{corsGraphQLContext}}/1.0.0" with origin "http://localhost" and request method "POST" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "POST"
    And The response header "Access-Control-Allow-Headers" should contain "X-BrowserSessionID"

    # Negative: a NORMAL (non pre-flight) POST query carrying an Origin must NOT carry the pre-flight-only headers.
    When I put the following JSON payload in context as "corsGraphQLNormalQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{corsGraphQLContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "corsGraphQLNormalQuery" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain the header "Access-Control-Allow-Methods"
    And The response should not contain the header "Access-Control-Allow-Headers"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:CORSHeadersForAllAPITypesTestCase
  Scenario Outline: A CORS pre-flight OPTIONS to a Server Sent Events (SSE) API returns the configured allow-headers incl. X-BrowserSessionID as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_sse_api.json" as "corsSseApiId" and deployed it
    When I publish the "apis" resource with id "corsSseApiId"
    Then The lifecycle status of API "corsSseApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsSseApiId"
    And I extract response field "context" and store it as "corsSseContext"
    When I have set up application with keys, subscribed to API "corsSseApiId" with plan "AsyncUnlimited", and obtained access token for "corsSseSubId"
    Then The response status code should be 200

    # SSE is consumed over GET, so the pre-flight declares GET as the requested method. This is the Async-type
    # pre-flight the legacy fix targeted — the gateway must still answer with the CORS allow headers.
    When I send a CORS preflight to gateway context "{{corsSseContext}}/1.0.0" with origin "http://localhost" and request method "GET" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "GET"
    And The response header "Access-Control-Allow-Headers" should contain "X-BrowserSessionID"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
