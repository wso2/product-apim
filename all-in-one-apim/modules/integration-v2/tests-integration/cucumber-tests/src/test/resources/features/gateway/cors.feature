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
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
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
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
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
  #
  # Also ports CORSHeadersTestCase's two header cases, which are default-CORS cases (its API is created with no
  # corsConfiguration): the pre-flight's EXACT default Access-Control-Allow-Headers list, and the assertNull the v2
  # corpus never had — Access-Control-Allow-Credentials is ABSENT both on the pre-flight and on a normal request,
  # because this API does not enable credentials. The wildcard allow-origin asserted on the very same response is
  # the positive control for each absence: the CORS handler demonstrably ran and STILL emitted no Allow-Credentials,
  # so a gateway that unconditionally answered "Allow-Credentials: true" (which every other CORS scenario here would
  # happily accept) fails exactly here.
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:APIMANAGER3965TestCase @legacy:CORSHeadersTestCase
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
    # The EXACT default allow-headers list, pinned verbatim rather than as a substring, and OBSERVED from the
    # container rather than derived. It is the six entries of [apim.cors] allow_headers in the shipped
    # deployment.toml, plus the gateway's configured authorization and api-key header names, which
    # CORSRequestHandler.initializeHeaders appends in that order — hence the capitalised "Authorization" and
    # "ApiKey" trailing the lower-case "authorization" that comes from the config. No integration-v2 TOML overlay
    # sets any [apim.cors] key, so the shipped default applies unchanged.
    #
    # CORSHeadersTestCase pins a SIX-entry list that omits "apikey" and "Internal-Key". That legacy expectation is
    # STALE: it predates those two defaults being added to the shipped deployment.toml. Do not "restore" it.
    And The response should contain the header "Access-Control-Allow-Headers" with value "authorization,Access-Control-Allow-Origin,Content-Type,SOAPAction,apikey,Internal-Key,Authorization,ApiKey"
    # No credentials configured -> the header is ABSENT, not "false".
    And The response should not contain the header "Access-Control-Allow-Credentials"

    # A NORMAL (non pre-flight) request with an Origin: the wildcard allow-origin is echoed (positive control) and
    # allow-credentials is STILL absent.
    When I invoke the API at gateway context "{{dcContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should contain the header "Access-Control-Allow-Origin" with value "*"
    And The response should not contain the header "Access-Control-Allow-Credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The security-relevant half of the allow-credentials contract, and the one the corpus was blind to: an API with
  # CORS explicitly ENABLED for a specific origin but accessControlAllowCredentials FALSE must NOT receive an
  # Access-Control-Allow-Credentials header at all. Distinct from the default-CORS case above: here the CORS handler
  # is configured (a concrete origin is echoed rather than "*"), so this pins the CONFIGURED-false path. The echoed
  # concrete origin is the positive control in the same scenario — it proves the configured handler produced this
  # response, so the missing Allow-Credentials is the config being honoured and not a dead route.
  # Ports the assertNull(ACCESS_CONTROL_ALLOW_CREDENTIALS) assertions of CORSHeadersTestCase for a configured API.
  @cap:gateway @feat:cors @rule:preflight @type:negative @dep:publisher @dep:devportal @legacy:CORSHeadersTestCase
  Scenario Outline: A CORS-enabled API that does not allow credentials returns no allow-credentials header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_nocred_api.json" as "ncApiId" and deployed it
    When I publish the "apis" resource with id "ncApiId"
    Then The lifecycle status of API "ncApiId" should be "Published"
    When I retrieve the "apis" resource with id "ncApiId"
    And I extract response field "context" and store it as "ncContext"
    When I have set up application with keys, subscribed to API "ncApiId", and obtained access token for "ncSubId"
    Then The response status code should be 200

    # Pre-flight: the CONFIGURED origin is echoed (positive control) and allow-credentials is absent.
    When I send a CORS preflight to gateway context "{{ncContext}}/1.0.0/customers/123/" with origin "http://localhost" and request method "GET" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "GET"
    And The response should not contain the header "Access-Control-Allow-Credentials"

    # And on a normal request too: origin echoed, no allow-credentials.
    When I invoke the API at gateway context "{{ncContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response should not contain the header "Access-Control-Allow-Credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIMANAGER3965TestCase#testAPICreationWithCorsConfiguration — the UPDATE path, which v2 never exercised
  # (every other CORS API here is CREATED with its corsConfiguration already set, so a gateway that only honoured
  # CORS at create time would pass them all). An API published with DEFAULT CORS is updated to carry a full
  # corsConfiguration and redeployed; the pre-flight then reflects the new config.
  #
  # The same scenario closes the METHOD-FILTERING assertion: the config allows POST, PUT, DELETE, PATCH, OPTIONS
  # AND GET, but the API itself only exposes a GET resource, so the pre-flight's Access-Control-Allow-Methods must
  # be filtered to GET alone. The pre-update pre-flight is the BASELINE (wildcard "*" origin, default CORS), so the
  # post-update assertions cannot pass vacuously — the origin demonstrably CHANGED from "*" to the configured
  # origin, proving the update reached the gateway.
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:APIMANAGER3965TestCase
  Scenario Outline: A CORS configuration applied by update and redeploy is honoured and filtered to the API's own verb as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_default_cors_api.json" as "cuApiId" and deployed it
    When I publish the "apis" resource with id "cuApiId"
    Then The lifecycle status of API "cuApiId" should be "Published"
    When I retrieve the "apis" resource with id "cuApiId"
    And I extract response field "context" and store it as "cuContext"
    And I put the response payload in context as "cuApiPayload"

    # BASELINE (default CORS, before the update): wildcard origin, no allow-credentials.
    When I send a CORS preflight to gateway context "{{cuContext}}/1.0.0/customers/123/" with origin "http://localhost:8080" and request method "GET" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "*"
    And The response should not contain the header "Access-Control-Allow-Credentials"

    # UPDATE the API to carry a CORS config: two specific origins, credentials enabled, and a method allow-list
    # that is DELIBERATELY broader than the API's own resource set (POST/PATCH/GET/DELETE/OPTIONS/PUT).
    When I update the "apis" resource "cuApiId" and "cuApiPayload" with configuration type "corsConfiguration" and value:
      """
      {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["https://localhost:9443","http://localhost:8080"],"accessControlAllowCredentials":true,"accessControlAllowHeaders":["Access-Control-Allow-Origin","authorization","Content-Type","SOAPAction"],"accessControlAllowMethods":["POST","PATCH","GET","DELETE","OPTIONS","PUT"]}
      """
    Then The response status code should be 200
    And The value of response field "corsConfiguration.corsConfigurationEnabled" should be "true"
    When I deploy the API with id "cuApiId"
    Then The response status code should be 201
    And I wait until "apis" "cuApiId" revision is deployed in the gateway

    # The pre-flight now reflects the UPDATED config: the request's origin is echoed (no longer "*"),
    # allow-credentials is true, and allow-methods is filtered to the API's OWN verb — GET only.
    # Polled on the HEADER, not the status: the pre-flight answers 200 with the OLD config too, so an until-200
    # poll would return the stale response and the assertions below would race artifact propagation.
    When I send a CORS preflight to gateway context "{{cuContext}}/1.0.0/customers/123/" with origin "http://localhost:8080" and request method "GET" until response header "Access-Control-Allow-Origin" becomes "http://localhost:8080" within 90 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost:8080"
    And The response should contain the header "Access-Control-Allow-Credentials" with value "true"
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
  # Access-Control-Allow-Origin "http://localhost", Access-Control-Allow-Methods "GET, POST, DELETE, PUT, OPTIONS,
  # HEAD" and Access-Control-Allow-Headers "Content-Type"; a 200 carrying those backend headers proves the OPTIONS
  # was routed to the backend. (The legacy asserted a different backend's exact header values; here we assert THIS
  # backend's values.)
  #
  # The Allow-Origin VALUE is the load-bearing assertion, and the reason the backend answers a CONCRETE origin
  # rather than "*": if the gateway had handled the pre-flight itself instead of routing it, its default CORS
  # response would carry "*" — so the exact concrete value is what separates routed-to-backend from
  # answered-at-gateway. Allow-Credentials is asserted ABSENT (the legacy's assertNull): the backend sets none, so
  # its presence would mean the gateway injected a CORS header of its own on a path where it must inject nothing.
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
    # The BACKEND's exact allow-origin — a gateway-handled pre-flight would answer "*" instead.
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "OPTIONS"
    And The response header "Access-Control-Allow-Headers" should contain "Content-Type"
    # The backend sets no allow-credentials, so the gateway must add none either.
    And The response should not contain the header "Access-Control-Allow-Credentials"

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
    And The response should contain "CheckPhoneNumberResponse"
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
    And The response should contain "Afrikaans"
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

  # Ports the AI-API facet of CORS handling: an AIAPI-subtype API (Mistral connector fronting a no-auth mock LLM)
  # with CORS enabled answers the pre-flight OPTIONS for its chat-completions resource with the configured allow
  # headers incl. X-BrowserSessionID, and a NORMAL POST (proxied to the mock LLM) carries none of the pre-flight-only
  # headers. Needs the admin to register the AI service provider first (@dep:admin). Runs in both tenants.
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @dep:admin @legacy:CORSHeadersForAllAPITypesTestCase
  Scenario Outline: A CORS pre-flight OPTIONS to an AI API returns the configured allow-headers incl. X-BrowserSessionID as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register a no-auth AI service provider (prerequisite for the AIAPI-subtype create); isolated per tenant org.
    When I create an AI service provider "CorsAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "corsAiProviderId"
    Then The response status code should be 201
    # Import the AIAPI-subtype API from the Mistral OpenAPI with CORS enabled (incl. the X-BrowserSessionID header).
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_cors_add_props.json" as "corsAiApiId"
    Then The response status code should be 201
    When I deploy the API with id "corsAiApiId"
    When I publish the "apis" resource with id "corsAiApiId"
    Then The lifecycle status of API "corsAiApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsAiApiId"
    And I extract response field "context" and store it as "corsAiContext"
    When I have set up application with keys, subscribed to API "corsAiApiId" with plan "Unlimited", and obtained access token for "corsAiSubId"
    Then The response status code should be 200

    # AI chat-completions is invoked over POST, so the pre-flight declares POST as the requested method.
    When I send a CORS preflight to gateway context "{{corsAiContext}}/1.0.0/v1/chat/completions" with origin "http://localhost" and request method "POST" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "POST"
    And The response header "Access-Control-Allow-Headers" should contain "X-BrowserSessionID"

    # Negative: a NORMAL (non pre-flight) POST to the mock LLM must NOT carry the pre-flight-only headers.
    When I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "corsMistralPayload"
    And I invoke the API at gateway context "{{corsAiContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "corsMistralPayload" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "chat.completion"
    And The response should not contain the header "Access-Control-Allow-Methods"
    And The response should not contain the header "Access-Control-Allow-Headers"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the "Expose a SOAP Service as a REST API" (Generate REST APIs / SOAPTOREST) facet: a REST API generated
  # from hello.wsdl (APIM generates POST /sayHello) with CORS enabled answers the pre-flight OPTIONS for the
  # generated resource with the configured allow headers incl. X-BrowserSessionID; the normal POST (converted
  # REST->SOAP to the soap-stub backend and back) carries none of the pre-flight-only headers. Runs in both tenants.
  @cap:gateway @feat:cors @rule:preflight @type:regression @dep:publisher @dep:devportal @legacy:CORSHeadersForAllAPITypesTestCase
  Scenario Outline: A CORS pre-flight OPTIONS to a SOAP-to-REST API returns the configured allow-headers incl. X-BrowserSessionID as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "corsS2RName"
    And I generate a unique value and store it as "corsS2RCtx"
    When I put the following JSON payload in context as "corsS2RAddProps"
    """
    {"name":"{{corsS2RName}}","context":"{{corsS2RCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}},"corsConfiguration":{"corsConfigurationEnabled":true,"accessControlAllowOrigins":["http://localhost"],"accessControlAllowCredentials":false,"accessControlAllowHeaders":["authorization","Access-Control-Allow-Origin","Content-Type","X-BrowserSessionID","apikey"],"accessControlAllowMethods":["GET","PUT","POST","DELETE","PATCH"]}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "corsS2RAddProps" and implementation type "SOAPTOREST" as "corsS2RApiId"
    Then The response status code should be 201
    When I deploy the API with id "corsS2RApiId"
    When I publish the "apis" resource with id "corsS2RApiId"
    Then The lifecycle status of API "corsS2RApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsS2RApiId"
    And I extract response field "context" and store it as "corsS2RContext"
    When I have set up application with keys, subscribed to API "corsS2RApiId" with plan "Unlimited", and obtained access token for "corsS2RSubId"
    Then The response status code should be 200

    # The generated REST resource for the sayHello operation is POST /sayHello.
    When I send a CORS preflight to gateway context "{{corsS2RContext}}/1.0.0/sayHello" with origin "http://localhost" and request method "POST" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response header "Access-Control-Allow-Methods" should contain "POST"
    And The response header "Access-Control-Allow-Headers" should contain "X-BrowserSessionID"

    # Negative: a NORMAL (non pre-flight) POST (JSON body converted to SOAP) must NOT carry the pre-flight-only headers.
    When I put the following JSON payload in context as "corsS2RBody"
    """
    {"name":"WSO2"}
    """
    And I invoke the API at gateway context "{{corsS2RContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "corsS2RBody" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"
    And The response should not contain the header "Access-Control-Allow-Methods"
    And The response should not contain the header "Access-Control-Allow-Headers"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A LITERAL resource sitting next to a TEMPLATED sibling — POST /payee/personal beside GET /payee/{id}. Ports
  # APILoggingTest#testSimilarTemplateInvocationWithLoggingTestcase, which sent both pre-flights and both real
  # calls and asserted only "200" four times (and, despite its name, never read the api.log it enabled, so it
  # asserted nothing whatever about logging). A bare 200 cannot tell WHICH resource served the call,
  # which is the only interesting property here, so each call's resolution is asserted directly:
  #   * The pre-flight's Access-Control-Allow-Methods is computed from the resolved resource's own verbs
  #     (CORSRequestHandler#setCORSHeaders → selectedResource.getMethods()), so it NAMES the resolution: the
  #     pre-flight for POST /payee/personal answers POST-and-not-GET (the literal), while the pre-flight for
  #     GET /payee/123 answers GET-and-not-POST (the template).
  #   * The two resources carry different authTypes (None on the literal, Application & Application User on the
  #     template), so the SAME path under a different verb proves the split: an unauthenticated POST
  #     /payee/personal is 200 (literal, unsecured) while an unauthenticated GET /payee/personal is 401 (it
  #     resolved to the secured template, not to the literal that happens to share the path).
  # Default CORS (no corsConfiguration on the API) is required for this: with a configured CORS block the
  # allow-methods header comes from the config, not from the resolved resource.
  @cap:gateway @feat:cors @rule:similar-template @type:regression @dep:publisher @dep:devportal @legacy:APILoggingTest
  Scenario Outline: A pre-flight and the real calls resolve to the right sibling when a literal resource sits next to a template as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_similar_template_cors_api.json" as "stApiId" and deployed it
    When I publish the "apis" resource with id "stApiId"
    Then The lifecycle status of API "stApiId" should be "Published"
    When I retrieve the "apis" resource with id "stApiId"
    And I extract response field "context" and store it as "stContext"
    When I have set up application with keys, subscribed to API "stApiId", and obtained access token for "stSubId"
    Then The response status code should be 200

    # Pre-flight for POST on the LITERAL path → resolves to POST /payee/personal, so allow-methods names POST only.
    When I send a CORS preflight to gateway context "{{stContext}}/1.0.0/payee/personal" with origin "https://localhost:9443" and request method "POST" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "*"
    And The response header "Access-Control-Allow-Methods" should contain "POST"
    And The response header "Access-Control-Allow-Methods" should not contain "GET"

    # Pre-flight for GET on a TEMPLATED path → resolves to GET /payee/{id}, so allow-methods names GET only.
    When I send a CORS preflight to gateway context "{{stContext}}/1.0.0/payee/123" with origin "https://localhost:9443" and request method "GET" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "*"
    And The response header "Access-Control-Allow-Methods" should contain "GET"
    And The response header "Access-Control-Allow-Methods" should not contain "POST"

    # The real calls: both siblings are invocable with a token.
    When I invoke the API at gateway context "{{stContext}}/1.0.0/payee/personal" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{stContext}}/1.0.0/payee/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Which resource served it: the literal is authType None, so POST /payee/personal needs no credential (200)…
    When I invoke the API at gateway context "{{stContext}}/1.0.0/payee/personal" with method "POST" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # …while a GET on the very same path resolves to the SECURED template and is rejected without a credential.
    When I invoke the API at gateway context "{{stContext}}/1.0.0/payee/personal" with method "GET" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
