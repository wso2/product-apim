@cleanup
Feature: Distributed tracing written to the API Manager open tracing log

  The gateway's spans written to wso2-apimgt-open-tracing.log inside the APIM container. The block's overlay
  turns the remote tracer off and the log tracer on (remote_tracer.enable = false, log_tracer.enable = true),
  so instead of POSTing spans to a collector the exporter writes one TRACE line per span — the file the
  distribution's log4j2.properties wires up via the OPEN_TRACING appender and the "tracer" logger at TRACE
  level. Nothing is exported anywhere, so the read-back IS the assertion target, read from the container.

  Each line is a JSON object with the exported span's operation name, latency and tags. The exact keys and the
  span/tag rendering are the product's: the step logs the whole file before asserting, so the first run pins
  the observed format rather than the layout in an outdated doc sample.

  The span set is the same product chain the Jaeger, Zipkin and Moesif legs prove: the request span (renamed
  by the gateway to <apiName>--<version>--<tenantDomain>) plus the gateway's API:*_Latency mediation spans and
  the API:Backend_Latency backend call.

  @cap:analytics @feat:tracing @rule:log-tracing @type:regression @dep:publisher
  Scenario Outline: The API Manager writes its gateway spans to the open tracing log as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tracingLogApiId" and deployed it
    And the "apis" resource "tracingLogApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "tracingLogApiId"
    Then The lifecycle status of API "tracingLogApiId" should be "Published"
    When I retrieve the "apis" resource with id "tracingLogApiId"
    And I extract response field "context" and store it as "tracingLogApiContext"
    When I have set up application with keys, subscribed to API "tracingLogApiId", and obtained access token for "tracingLogSubId"
    Then The response status code should be 200
    # The published context already includes the /t/<tenant> prefix for a tenant API, so it is invoked verbatim
    When I invoke the API at gateway context "{{tracingLogApiContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The exporter writes asynchronously (BatchSpanProcessor worker threads), so the read-back polls the log
    # file for a line carrying this scenario's UNIQUE API context; the whole file content is kept for the
    # chain assertions and is logged on failure so a format drift shows as data rather than a mystery.
    Then the API Manager open tracing log should record a span for the API context "{{tracingLogApiContext}}"
    # The gateway RENAMES API:Response_Latency to <apiName>--<version>--<tenantDomain> when it finishes the
    # request, so the request-span name is derived from the creation payload plus the acting actor's tenant.
    And the open tracing log should carry the gateway's request span for the API created with payload "createApiPayload"
    # The same trace id rides on every span line of one request — a 32-hex identifier is the OTLP trace id.
    And the open tracing log's spans for this request should share one trace id
    # The log cannot assert parent/child linkage (the lines carry no parent id), so the chain is asserted by
    # PRESENCE: the gateway's mediation spans and the backend call must all show up for this request.
    And the open tracing log should record the "API:Backend_Latency" span for the payload "createApiPayload"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |