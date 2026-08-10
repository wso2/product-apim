@cleanup
Feature: Per-API Logging Configuration

  The devops per-API logging API (`/api/am/devops/v0/tenant-logs/{tenant}/apis`) and its effect on
  `repository/logs/api.log`. Ports APILoggingTest.

  A log level is set either for a whole API or scoped to one resource (verb + path). At `FULL` the gateway
  emits an API_LOG line per invocation; the log4j2 pattern is `[%d] %5p {%c} %X{apiName} - %m`, so those lines
  read `... INFO {API_LOG} <apiName> - ...` and carry the request's correlationId.

  The legacy @Factory's tenant row was phantom (`initialize()` hard-set SUPER_TENANT_ADMIN and every devops URL
  hardcoded `carbon.super`). These scenarios close that real gap by pairing each API owner with its own explicit
  `/tenant-logs/{tenant}/apis` path. The devops endpoint itself is super-admin-only (a tenant admin gets 403 /
  900915), so the tenant row creates/subscribes as the tenant admin, then configures that tenant's API as the
  super admin before invoking it with the already-issued tenant token.

  On the listing assertions: legacy asserted the WHOLE listing equalled a hardcoded document — first
  `{"apis":[]}`, then a one- or two-entry document. That only holds on a server whose entire API inventory
  belongs to this one test. The container is shared across a block here, so the portable form of the same
  claim is that the API under test appears with exactly the expected field values, paired with the negative
  that an API left at OFF is absent from the `log-level=full` listing — which is where legacy's exact-equality
  got its discriminating power from in the first place.

  Legacy's third method, testSimilarTemplateInvocationWithLoggingTestcase, is NOT here: despite its name it
  asserts nothing whatsoever about logging (it enables FULL, invokes, then truncates api.log and resets its
  counter without ever reading a line). Its only real assertions are that a literal sibling resource and a
  templated one both resolve — already covered as CORS pre-flight + real-call scenarios in
  gateway/cors.feature, which is where the workplan maps it.

  @cap:analytics @feat:per-api-logging @rule:api-wide @type:smoke @dep:gateway @legacy:APILoggingTest
  Scenario Outline: An API-wide FULL log level is listed and makes the gateway write API_LOG lines as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "loggingApiId" and deployed it
    When I publish the "apis" resource with id "loggingApiId"
    Then The lifecycle status of API "loggingApiId" should be "Published"
    When I retrieve the "apis" resource with id "loggingApiId"
    And I extract response field "context" and store it as "loggingApiContext"
    And I extract response field "name" and store it as "loggingApiName"
    When I have set up application with keys, subscribed to API "loggingApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    And I have valid access tokens as "<loggingActor>"

    # A newly created API is listed at OFF, both API-wide and once per resource.
    When I retrieve the per-API log levels for tenant "<tenantDomain>"
    Then The response status code should be 200
    And The per-API log listing should contain API "loggingApiId" at level "OFF" for resource "null" "null"
    And The per-API log listing should contain API "loggingApiId" at level "OFF" for resource "GET" "/customers/{id}"

    # ...and therefore does NOT appear when the listing is filtered to full.
    When I retrieve the per-API log levels for tenant "<tenantDomain>" filtered to level "full"
    Then The response status code should be 200
    And The per-API log listing should not contain API "loggingApiId"

    When I set the log level of API "loggingApiId" to "FULL" for tenant "<tenantDomain>"
    Then The response status code should be 200
    When I retrieve the per-API log levels for tenant "<tenantDomain>" filtered to level "full"
    Then The response status code should be 200
    # The API-wide entry is the one that flips; it carries no resource scoping.
    And The per-API log listing should contain API "loggingApiId" at level "FULL" for resource "null" "null"
    When I wait 15 seconds for the per-API logging configuration to reach the gateway

    When I mark the current end of the server log file "api.log"
    And I invoke the API at gateway context "{{loggingApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "api.log" should gain a line containing "INFO {API_LOG} {{loggingApiName}}" within 60 seconds
    And The server log file "api.log" should gain a line containing "correlationId" within 60 seconds

    # Leave the API at OFF so a sibling scenario's `log-level=full` listing is not polluted.
    When I set the log level of API "loggingApiId" to "OFF" for tenant "<tenantDomain>"
    Then The response status code should be 200

    Examples:
      | actor             | loggingActor | tenantDomain |
      | admin             | admin        | carbon.super |
      | admin@tenant1.com | admin        | tenant1.com  |

  @cap:analytics @feat:per-api-logging @rule:per-resource @type:regression @dep:gateway @legacy:APILoggingTest
  Scenario Outline: A FULL log level scoped to one resource is listed against that resource only as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_api_logging_api.json" as "resourceLogApiId" and deployed it
    When I publish the "apis" resource with id "resourceLogApiId"
    Then The lifecycle status of API "resourceLogApiId" should be "Published"
    When I retrieve the "apis" resource with id "resourceLogApiId"
    And I extract response field "context" and store it as "resourceLogApiContext"
    And I extract response field "name" and store it as "resourceLogApiName"
    When I have set up application with keys, subscribed to API "resourceLogApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    And I have valid access tokens as "<loggingActor>"

    # Scope FULL to GET /customers/{id} only, leaving GET /orders/{orderId} at OFF.
    When I set the log level of API "resourceLogApiId" to "full" for resource "GET" "/customers/{id}" in tenant "<tenantDomain>"
    Then The response status code should be 200
    When I wait 15 seconds for the per-API logging configuration to reach the gateway

    When I retrieve the per-API log levels for tenant "<tenantDomain>" filtered to level "full"
    Then The response status code should be 200
    # The entry that flipped is the RESOURCE-scoped one, carrying the verb and path it was scoped to...
    And The per-API log listing should contain API "resourceLogApiId" at level "FULL" for resource "GET" "/customers/{id}"

    # ...while the untouched sibling resource is still OFF, so it is absent from the full-filtered listing but
    # present in the unfiltered one. This pair is what proves the scoping is per-resource and not API-wide.
    When I retrieve the per-API log levels for tenant "<tenantDomain>"
    Then The response status code should be 200
    And The per-API log listing should contain API "resourceLogApiId" at level "OFF" for resource "GET" "/orders/{orderId}"

    When I mark the current end of the server log file "api.log"
    And I invoke the API at gateway context "{{resourceLogApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "api.log" should gain a line containing "INFO {API_LOG} {{resourceLogApiName}}" within 60 seconds
    And The server log file "api.log" should gain a line containing "correlationId" within 60 seconds

    # Setting it back to off stops the logging for that resource.
    When I set the log level of API "resourceLogApiId" to "off" for resource "GET" "/customers/{id}" in tenant "<tenantDomain>"
    Then The response status code should be 200
    When I retrieve the per-API log levels for tenant "<tenantDomain>" filtered to level "full"
    Then The response status code should be 200
    And The per-API log listing should not contain API "resourceLogApiId"

    Examples:
      | actor             | loggingActor | tenantDomain |
      | admin             | admin        | carbon.super |
      | admin@tenant1.com | admin        | tenant1.com  |

  @cap:analytics @feat:per-api-logging @rule:tenant-admin-access @type:negative @legacy:APILoggingTest
  Scenario: A tenant administrator cannot manage per-API logging
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I retrieve the per-API log levels for tenant "tenant1.com"
    Then The response status code should be 403
    And The response should contain "900915"
    And The response should contain "Invalid Permission"
