@cleanup
Feature: Distributed tracing exported to a real Zipkin collector

  The gateway's spans, read back from a REAL Zipkin server that the APIM container exports to over the
  Zipkin v2 JSON path (POST /api/v2/spans). This leg's subject is the Zipkin BACKEND as much as the spans:
  ZipkinTelemetry composes the exporter endpoint from hostname + port and the collector stores what it
  receives, so the assertions here are made against Zipkin's own query API (GET /api/v2/traces), the same
  "real collector contradicts us" boundary as the Jaeger leg.

  Zipkin's v2 model differs from Jaeger's: parent/child linkage is a parentId field (no references array),
  span.kind is a top-level "kind" field rather than a tag, and attributes arrive as a flat string->string
  tags object. The tag KEYS and the exact rendering are the collector's, measured on the first run and pinned
  afterwards — the steps log every matched span's full JSON before asserting, so a difference shows up as
  observed data rather than a guess.

  The chain is the same product chain the OTLP/gRPC leg proves against Jaeger: the request span (renamed by
  the gateway to <apiName>--<version>--<tenantDomain>) is the root, the API resource span (<METHOD>--<template>)
  is its only child, and the backend call (API:Backend_Latency) hangs off the resource span — a GRANDCHILD of
  the request span.

  @cap:analytics @feat:tracing @rule:zipkin @type:regression @dep:publisher
  Scenario Outline: Gateway spans reach a real Zipkin collector over the Zipkin v2 JSON path as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tracingZipkinApiId" and deployed it
    And the "apis" resource "tracingZipkinApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "tracingZipkinApiId"
    Then The lifecycle status of API "tracingZipkinApiId" should be "Published"
    When I retrieve the "apis" resource with id "tracingZipkinApiId"
    And I extract response field "context" and store it as "tracingZipkinApiContext"
    When I have set up application with keys, subscribed to API "tracingZipkinApiId", and obtained access token for "tracingZipkinSubId"
    Then The response status code should be 200
    # The published context already includes the /t/<tenant> prefix for a tenant API, so it is invoked verbatim
    When I invoke the API at gateway context "{{tracingZipkinApiContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Polls Zipkin's query API for a trace mentioning this scenario's UNIQUE context, so a sibling scenario's
    # trace can never satisfy this one; logs the matched spans' full JSON for the exact-tag steps to pin from.
    Then the Zipkin collector should have received a trace for the API context "{{tracingZipkinApiContext}}"
    # The gateway RENAMES API:Response_Latency to <apiName>--<version>--<tenantDomain> and API:Resource to
    # <METHOD>--<template> when it finishes them, so the chain step derives the request span's name from the
    # creation payload plus the acting actor's tenant and walks parentId links — pinning the real chain rather
    # than a name the product deliberately replaces.
    And the Zipkin trace should have the gateway's request span chain for the API created with payload "createApiPayload"
    # The request span is the SERVER span the gateway opens for the whole request...
    And the span "request" in the Zipkin trace should have kind "SERVER"
    And the span "request" in the Zipkin trace should have the tag "http.request.method" with value "GET"
    # ...carrying the response status as a Zipkin tag (Zipkin v2 tags are strings)
    And the span "request" in the Zipkin trace should have the tag "http.response.status_code" with value "200"
    And the span "request" in the Zipkin trace should have the tag "url.path" whose value contains the API context stored as "tracingZipkinApiContext"
    # The renamed keys must be GONE, not merely supplemented.
    And the span "request" in the Zipkin trace should not have the tag "http.method"
    And the span "request" in the Zipkin trace should not have the tag "http.status_code"
    And the span "request" in the Zipkin trace should not have the tag "http.url"
    # The backend call is recorded as a CLIENT span hanging off the API resource span, which is itself a child
    # of the request span — so it is a GRANDCHILD of the request span, not a direct child.
    And the span "backend" in the Zipkin trace should have kind "CLIENT"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |