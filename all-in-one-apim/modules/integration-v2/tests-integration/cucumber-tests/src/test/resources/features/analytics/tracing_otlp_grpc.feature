@cleanup
Feature: Distributed tracing exported over OTLP/gRPC

  The gateway's spans, read back from a REAL Jaeger collector that the APIM container exports to
  over OTLP/gRPC. This is the leg that carries SPAN SEMANTICS — kind, parent/child linkage, attribute
  values and types, and the process resource attributes — because only a real collector decodes the
  payload. The OTLP/HTTP leg in tracing_moesif_otlp_http.feature asserts wire facts only.

  The two transports are separate test blocks because a server has ONE remote tracer and the protocol
  is read at startup, so a single container cannot be both.

  Attribute names here are the OpenTelemetry semantic-convention HTTP keys APIM now emits. The
  pre-patch names were http.method / http.status_code / http.url, and each is asserted ABSENT: a
  rename that leaves the old key in place would otherwise pass unnoticed.

  The traced spans and their kinds are the product's, not this test's: the request span is started as
  SpanKind.SERVER and the backend call as SpanKind.CLIENT. Note the chain is request -> API resource ->
  backend: APIMgtLatencyStatsHandler inserts the API resource span between the request span and the backend
  call, so the backend span is a GRANDCHILD of the request span. Asserting it as a direct child would fail
  against a correct build.

  Two of these spans are RENAMED by the gateway as it finishes them (API:Response_Latency becomes
  <apiName>--<version>--<tenantDomain>, API:Resource becomes <METHOD>--<template>), so the steps address them
  by role and the span-chain step derives the name.

  Path assertions are in their baseline form: url.path is required to carry this scenario's UNIQUE API
  context, but its surrounding string is not pinned. The collector logs every matched span's full tag
  set, and the first run is what establishes the exact rendering (in particular whether a tenant API's
  path carries the /t/<tenant> prefix). The exact-value steps are used from the second run on.

  @cap:analytics @feat:tracing @rule:otlp-grpc @type:regression @dep:publisher
  Scenario Outline: Gateway spans reach a real Jaeger collector with semantic-convention attributes as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tracingApiId" and deployed it
    And the "apis" resource "tracingApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "tracingApiId"
    Then The lifecycle status of API "tracingApiId" should be "Published"
    When I retrieve the "apis" resource with id "tracingApiId"
    And I extract response field "context" and store it as "tracingApiContext"
    When I have set up application with keys, subscribed to API "tracingApiId", and obtained access token for "tracingSubId"
    Then The response status code should be 200
    # The published context already includes the /t/<tenant> prefix for a tenant API, so it is invoked verbatim
    When I invoke the API at gateway context "{{tracingApiContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Polls the collector's query API for a trace mentioning this scenario's UNIQUE context, so a sibling
    # scenario's trace can never satisfy this one.
    Then the OTLP collector should have received a trace for the API context "{{tracingApiContext}}"
    # The gateway RENAMES two of its spans as it finishes them, so they are addressed by ROLE from here on:
    # API:Response_Latency is exported as <apiName>--<version>--<tenantDomain> and API:Resource as
    # <METHOD>--<template>. Asserting the literal constant names would fail against a correct build. This step
    # derives the request span's name from the creation payload plus the acting actor's tenant and walks the
    # parent/child links, so it pins the real chain rather than a name the product deliberately replaces.
    And the trace should have the gateway's request span chain for the API created with payload "createApiPayload"
    # The request span is the SERVER span the gateway opens for the whole request...
    And the span "request" in that trace should have the attribute "span.kind" with value "server"
    And the span "request" in that trace should have the attribute "http.request.method" with value "GET"
    # ...carrying the status as a NUMBER, not the string "200": the collector reports the type it received, and a
    # string-typed export would satisfy a text comparison while violating the semconv integer contract.
    And the span "request" in that trace should have the attribute "http.response.status_code" with numeric value 200
    And the span "request" in that trace should have the attribute "url.path" whose value contains the API context stored as "tracingApiContext"
    # The renamed keys must be GONE, not merely supplemented.
    And the span "request" in that trace should not have the attribute "http.method"
    And the span "request" in that trace should not have the attribute "http.status_code"
    And the span "request" in that trace should not have the attribute "http.url"
    # The backend call is recorded as a CLIENT span hanging off the API resource span, which is itself a child of
    # the request span — so it is a GRANDCHILD of the request span, not a direct child.
    And the span "backend" in that trace should have the attribute "span.kind" with value "client"
    # process.pid is a PROCESS-level resource attribute, so it is asserted on the trace's process rather
    # than on any span, and against the container's real pid rather than against itself.
    And the trace should have a numeric process tag "process.pid"
    And the carbon server process id should match the trace's process tag "process.pid"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
