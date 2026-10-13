@cleanup
Feature: Distributed tracing exported over OTLP/HTTP to the real Moesif collector

  The gateway's spans exported over OTLP/HTTP to the REAL Moesif collector (api.moesif.net), with delivery
  asserted by reading the event back out of Moesif through its own Management API. This is the OTLP/HTTP leg.
  The hermetic OTLP/HTTP leg that pointed the exporter at a local stand-in is retired: Moesif's search API
  returns modelled events rather than the bytes the gateway sent, so the wire contract (method, path, auth
  header, content type, gzip, payload contents) is no longer asserted here, and the span-semantics assertions
  live in the OTLP/gRPC leg against a real Jaeger.

  Only one remote tracer is active in a server at startup, so this is its own block entirely, separate from the
  OTLP/gRPC leg.

  Delivery is not instantaneous: the exporter sends a batched POST roughly every 5s and Moesif then indexes the
  event, so the read-back polls. It also needs TWO credentials — the collector application id the gateway exports
  with, and a Management API key to read the result back — so the scenario skips itself when either is absent
  rather than failing on a missing secret.

  @cap:analytics @feat:tracing @rule:otlp-http @type:regression @dep:publisher
  Scenario Outline: OTLP/HTTP spans are delivered to Moesif and readable through its Management API as <actor>
    Given the Moesif live tracing credentials are available
    And The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tracingMoesifLiveApiId" and deployed it
    And the "apis" resource "tracingMoesifLiveApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "tracingMoesifLiveApiId"
    Then The lifecycle status of API "tracingMoesifLiveApiId" should be "Published"
    When I retrieve the "apis" resource with id "tracingMoesifLiveApiId"
    And I extract response field "context" and store it as "tracingMoesifLiveApiContext"
    When I have set up application with keys, subscribed to API "tracingMoesifLiveApiId", and obtained access token for "tracingMoesifLiveSubId"
    Then The response status code should be 200
    # The published context already includes the /t/<tenant> prefix for a tenant API, so it is invoked verbatim
    When I invoke the API at gateway context "{{tracingMoesifLiveApiContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The context is ${UNIQUE:}-resolved, so it is distinct per runner instance — which is what makes it a
    # reliable lookup key, since Moesif's from/to window is calendar-aligned and cannot express "last 90s".
    Then Moesif should have received the exported trace for the API context "{{tracingMoesifLiveApiContext}}"
    And that Moesif trace should carry the gateway's request span chain for the API created with payload "createApiPayload"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |