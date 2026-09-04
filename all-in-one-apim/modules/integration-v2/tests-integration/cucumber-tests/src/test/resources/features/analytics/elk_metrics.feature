@cleanup
Feature: ELK Analytics Metric Logging

  APIM analytics publishing through the ELK reporter and the metric events it writes to
  `repository/logs/apim_metrics.log`. Ports APIMAnalyticsTest and ELKAnalyticsWithRespondMediatorTestCase.

  The block's overlay is the whole enablement: `[apim.analytics] enable = true, type = "elk"`. The 4.7.0
  distribution's log4j2.properties already declares APIM_METRICS_APPENDER (writing apim_metrics.log) and the
  `org.wso2.am.analytics.publisher.reporter.elk` logger, so — unlike legacy, which copied in its own
  log4j2.properties and restarted the server — no logging configuration is applied here.

  The metric line's shape is the product's, and it was MEASURED on this build rather than guessed.
  ELKCounterMetric logs `"apimMetrics: " + <metric name> + ", properties :" + gson(eventMap)`, the metric name for
  a response event is the constant `apim:response`, and the appender pattern is `... %5p %c{1} %m%n` — hence the
  `INFO ELKCounterMetric apimMetrics: apim:response, properties :` prefix asserted below. The properties object is
  a HashMap keyed on the RESPONSE schema's 28 attributes, so its serialised field order is fixed by those key
  hashes; measured, it always begins `apiName`, `proxyResponseCode`, `destination`, `apiCreatorTenantDomain`. That
  is what lets ONE contiguous substring pin api + status + destination + creator tenant on a SINGLE line — a
  stronger claim than legacy's per-line multi-substring matcher.

  BOTH TENANTS. Legacy ran super tenant only (APIMAnalyticsTest's @Factory declares a single SUPER_TENANT_ADMIN
  row; ELKAnalyticsWithRespondMediatorTestCase calls the bare `super.init()`), but the event DOES carry a tenant
  dimension — `apiCreatorTenantDomain`, measured on this build — so each row asserts its OWN tenant inside the
  contiguous substring and a tenant row is a genuinely distinct claim. Examples rows run sequentially and each
  re-marks apim_metrics.log before invoking, so the tenant row's assertion reads only text appended after the
  super-tenant row's event; the unique per-row API name pins it a second time.

  @cap:analytics @feat:analytics-events @rule:response-event @type:smoke @dep:gateway @legacy:APIMAnalyticsTest
  Scenario Outline: An invocation writes an ELK response metric carrying the API name, proxy response code and creator tenant as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "elkApiId" and deployed it
    And the "apis" resource "elkApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "elkApiId"
    Then The lifecycle status of API "elkApiId" should be "Published"
    When I retrieve the "apis" resource with id "elkApiId"
    And I extract response field "context" and store it as "elkApiContext"
    And I extract response field "name" and store it as "elkApiName"
    When I have set up application with keys, subscribed to API "elkApiId", and obtained access token for "elkSubId"
    Then The response status code should be 200

    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{elkApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The event is published asynchronously after the response is returned, so this polls rather than reading once.
    # ALL FOUR facts are required on the SAME line, which is what makes them one event: it is THIS api's event, the
    # proxy response code it recorded is the 200 asserted above, the destination is the backend the request really
    # reached, and the creator tenant is THIS row's tenant. Required as separate markers rather than one contiguous
    # substring ON PURPOSE: the event is a HashMap serialised by Gson, so its FIELD ORDER is bucket order — adding
    # one analytics attribute upstream can resize the table and reorder the JSON, which would break a contiguous
    # match and report as "the log never gained the line", i.e. as if analytics had stopped publishing.
    # MEASURED, not guessed: destination is the production endpoint URL of create_apim_test_api.json verbatim,
    # INCLUDING its trailing slash (the ENDPOINT_ADDRESS message-context property is the configured address,
    # unnormalised) — contrast the respond-mediator scenario below, where the property is absent and the product
    # substitutes the placeholder.
    And The server log file "apim_metrics.log" should gain a line containing all of the following within 60 seconds
      | INFO ELKCounterMetric apimMetrics: apim:response, properties :                             |
      | "apiName":"{{elkApiName}}"                                                                 |
      | "proxyResponseCode":200                                                                    |
      | "destination":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"    |
      | "apiCreatorTenantDomain":"<tenantDomain>"                                                  |

    Examples:
      | actor             | tenantDomain |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |

  # The RESPOND-MEDIATOR short circuit. The GET operation carries a request-flow policy that is a bare synapse
  # <respond/>, so the request is answered at the gateway and the API's production endpoint is never called. The
  # point of the legacy case is that the analytics event is STILL published, and that its `destination` falls back
  # to the literal `dummy_endpoint_address` — SynapseAnalyticsDataProvider#getTarget substitutes it when the
  # ENDPOINT_ADDRESS message-context property is absent, which is exactly the state a short-circuited request
  # leaves behind. That substitution is the discriminator: an event produced by a request that DID reach the
  # backend carries the endpoint URL there instead.
  @cap:analytics @feat:analytics-events @rule:respond-mediator @type:regression @dep:gateway @dep:publisher @legacy:ELKAnalyticsWithRespondMediatorTestCase
  Scenario Outline: A respond-mediator short circuit still publishes a metric, with a placeholder destination, as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register the common operation policy first so the API can reference it by name.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/respond_mediator.j2" and "artifacts/payloads/policySpecFiles/respond_mediator.yaml" as "respondPolicyId"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_elk_respond_api.json" as "respondApiId" and deployed it
    And the "apis" resource "respondApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "respondApiId"
    Then The lifecycle status of API "respondApiId" should be "Published"
    When I retrieve the "apis" resource with id "respondApiId"
    And I extract response field "context" and store it as "respondApiContext"
    And I extract response field "name" and store it as "respondApiName"
    When I have set up application with keys, subscribed to API "respondApiId", and obtained access token for "respondSubId"
    Then The response status code should be 200

    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{respondApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Same same-line, order-independent form as above (see that scenario for why field order is not pinned): this
    # api's event, its proxy response code, the placeholder destination that proves no endpoint was contacted, and
    # this row's creator tenant.
    And The server log file "apim_metrics.log" should gain a line containing all of the following within 60 seconds
      | INFO ELKCounterMetric apimMetrics: apim:response, properties : |
      | "apiName":"{{respondApiName}}"                                |
      | "proxyResponseCode":200                                       |
      | "destination":"dummy_endpoint_address"                        |
      | "apiCreatorTenantDomain":"<tenantDomain>"                     |

    Examples:
      | actor             | tenantDomain |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |
