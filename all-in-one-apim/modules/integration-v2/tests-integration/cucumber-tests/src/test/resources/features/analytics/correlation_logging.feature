@cleanup
Feature: Correlation Logging Configuration

  The devops correlation-logging API (`/api/am/devops/v0/config/correlation`) and its effect on
  `repository/logs/correlation.log`. Ports CorrelationLoggingTest.

  The five components come from `<CorrelationLogComponents>` in api-manager.xml — http, jdbc, ldap, synapse,
  method-calls — and the API takes the whole document, so enabling one component always means disabling the
  rest. State is persisted in AM_CORRELATION_CONFIGS, which is what makes it survive a restart.

  SUPER TENANT ONLY, deliberately. The legacy @Factory declares two TestUserMode rows, but @BeforeClass calls
  the bare `super.init()`, which hard-sets userMode to SUPER_TENANT_ADMIN — so the "tenant" instance ran as
  the super admin against the same hardcoded super-tenant endpoints and asserted nothing a tenant could not
  already see. Adding a tenant actor here would claim coverage legacy never had, so there is none.

  About the legacy source: the class is COMMENTED OUT of the legacy testng.xml (line 472), so none of this was
  ever verified upstream — which shows in its matchers. Two are wrong, and this feature does not reproduce them:

    isHTTPLogLine       matched any line containing "HTTP". But `|HTTP|` also appears in the SYNAPSE
                        ROUND-TRIP/BACKEND LATENCY lines and in the gateway LogsHandler line — and LogsHandler
                        is gated on the METHOD-CALLS component, not http. So legacy's "http is logging" flag
                        was in practice satisfied by synapse or method-calls output. Its own source concedes
                        the point: the http half of testSpecificCorrelationLoggingConfigsTest is commented out
                        as "no HTTP logs related to API invocation are logged when only http correlation logs
                        are enabled". The http component IS the Tomcat
                        RequestCorrelationIdValve, which instruments the SERVLET container (9443) and never
                        sees gateway traffic, because that goes through the Synapse passthrough NIO transport
                        and never enters the Catalina pipeline. Driven against a management-plane request
                        instead, the component works, and the scenario below pins it — recovering coverage
                        legacy had commented out.
    isMethodCallsLogLine also matched the APPLICATION NAME, so any line merely mentioning the app satisfied it.

  The literals matched below are the ones the product actually emits: `|HTTP-In-Request|` (Tomcat valve),
  `|HTTP State Transition|` / `|ROUND-TRIP LATENCY` / `|BACKEND LATENCY` (synapse passthrough), `|METHOD|`
  (the MethodTimeLogger aspects).

  @cap:analytics @feat:correlation-logging @rule:defaults @type:smoke @legacy:CorrelationLoggingTest
  Scenario: Correlation logging is off for every component by default
    Given The system is ready and I have valid publisher access tokens as "admin"
    When I retrieve the correlation logging configuration
    Then The response status code should be 200
    # All five declared components present and disabled, with jdbc carrying its seeded deniedThreads property.
    And The correlation configuration should have exactly the components "" enabled

  @cap:analytics @feat:correlation-logging @rule:enable-all @type:regression @dep:gateway @legacy:CorrelationLoggingTest
  Scenario: Enabling components makes correlation lines appear, and disabling them stops
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "correlationApiId" and deployed it
    When I publish the "apis" resource with id "correlationApiId"
    Then The lifecycle status of API "correlationApiId" should be "Published"
    When I retrieve the "apis" resource with id "correlationApiId"
    And I extract response field "context" and store it as "correlationApiContext"
    When I have set up application with keys, subscribed to API "correlationApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # jdbc is left off throughout: it logs on every statement the server executes, from every thread, so it
    # would flood correlation.log and make the negative half of this scenario meaningless. Legacy skipped it
    # for the same reason.
    When I enable only the correlation logging components "http,ldap,synapse,method-calls"
    Then The response status code should be 200
    And The correlation configuration should have exactly the components "http,ldap,synapse,method-calls" enabled
    # The PUT is acknowledged from the DB write; the gateway is switched asynchronously by an event-hub
    # CorrelationConfigEvent. Traffic sent before that lands is not instrumented.
    When I wait 15 seconds for the "correlation" configuration to reach the gateway

    When I mark the current end of the server log file "correlation.log"
    And I invoke the API at gateway context "{{correlationApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "activityid" set to "9e3ec6ed-2a37-4b20-8dd4-d5fbc754a7d9" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The synapse component instruments both ends of the passthrough transport, so one invocation produces the
    # connection state machine, the backend leg and the overall round trip.
    And The server log file "correlation.log" should gain a line containing "|HTTP State Transition|" within 60 seconds
    And The server log file "correlation.log" should gain a line containing "|BACKEND LATENCY" within 60 seconds
    And The server log file "correlation.log" should gain a line containing "|ROUND-TRIP LATENCY" within 60 seconds
    # The correlation id travels in the `activityid` request header and is stamped into the appender's
    # %X{Correlation-ID} field, so it must appear on the emitted lines.
    And The server log file "correlation.log" should gain a line containing "9e3ec6ed-2a37-4b20-8dd4-d5fbc754a7d9" within 60 seconds

    # Now switch everything off and repeat the identical invocation: the same lines must stop.
    When I enable only the correlation logging components ""
    Then The response status code should be 200

    And The correlation configuration should have exactly the components "" enabled
    # Same asynchrony in the other direction — verified: without this wait the next invocation still produced
    # the full set of synapse and method-calls lines despite a 200 from the disable.
    When I wait 15 seconds for the "correlation" configuration to reach the gateway

    When I mark the current end of the server log file "correlation.log"
    And I invoke the API at gateway context "{{correlationApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "activityid" set to "6b1d0f42-77c8-4a9e-9d3b-1f5a8c2e4b70" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "correlation.log" should gain no line containing "|HTTP State Transition|" within 20 seconds
    And The server log file "correlation.log" should gain no line containing "|ROUND-TRIP LATENCY" within 1 seconds
    And The server log file "correlation.log" should gain no line containing "6b1d0f42-77c8-4a9e-9d3b-1f5a8c2e4b70" within 1 seconds

  @cap:analytics @feat:correlation-logging @rule:specific-component @type:regression @dep:gateway @legacy:CorrelationLoggingTest
  Scenario: A single enabled component logs while the others stay silent
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "singleComponentApiId" and deployed it
    When I publish the "apis" resource with id "singleComponentApiId"
    Then The lifecycle status of API "singleComponentApiId" should be "Published"
    When I retrieve the "apis" resource with id "singleComponentApiId"
    And I extract response field "context" and store it as "singleComponentApiContext"
    When I have set up application with keys, subscribed to API "singleComponentApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # The two components are driven by DIFFERENT traffic, which is the whole point of separating them:
    #   http         the Tomcat RequestCorrelationIdValve -> SERVLET traffic (9443 management plane) only
    #   method-calls the MethodTimeLogger aspects         -> the gateway/keymgt request path
    # Both established empirically on this block: with only http enabled, a publisher REST call appended
    # exactly two lines (HTTP-In-Request/Response) and no |METHOD| at all; a gateway invocation appends
    # |METHOD| lines from CORSRequestHandler, APIKeyValidator and TokenValidationContext.

    # http alone. This is the half legacy commented out ("no HTTP logs related to API invocation are logged"),
    # — it is simply the wrong traffic. Driven at the management plane, it works.
    When I enable only the correlation logging components "http"
    Then The response status code should be 200
    When I wait 15 seconds for the "correlation" configuration to reach the gateway
    And I mark the current end of the server log file "correlation.log"
    And I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200
    And The server log file "correlation.log" should gain a line containing "|HTTP-In-Request|" within 60 seconds

    # ...and the gateway stays silent under the same setting: no synapse transitions, no method calls.
    When I mark the current end of the server log file "correlation.log"
    And I invoke the API at gateway context "{{singleComponentApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "correlation.log" should gain no line containing "|HTTP State Transition|" within 20 seconds
    And The server log file "correlation.log" should gain no line containing "|METHOD|" within 1 seconds

    # method-calls alone, driven by gateway traffic.
    When I enable only the correlation logging components "method-calls"
    Then The response status code should be 200
    When I wait 15 seconds for the "correlation" configuration to reach the gateway
    And I mark the current end of the server log file "correlation.log"
    And I invoke the API at gateway context "{{singleComponentApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "correlation.log" should gain a line containing "|METHOD|" within 60 seconds
    # synapse is off, so the transport state machine must not be logged even though gateway traffic flowed.
    And The server log file "correlation.log" should gain no line containing "|HTTP State Transition|" within 1 seconds

    # ...and the Tomcat valve is off again, so the http marker must not reappear on a management-plane call.
    When I mark the current end of the server log file "correlation.log"
    And I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200
    And The server log file "correlation.log" should gain no line containing "|HTTP-In-Request|" within 20 seconds

    # Everything off: neither marker, from either kind of traffic.
    When I enable only the correlation logging components ""
    Then The response status code should be 200
    When I wait 15 seconds for the "correlation" configuration to reach the gateway
    And I mark the current end of the server log file "correlation.log"
    And I invoke the API at gateway context "{{singleComponentApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200
    And The server log file "correlation.log" should gain no line containing "|METHOD|" within 20 seconds
    And The server log file "correlation.log" should gain no line containing "|HTTP-In-Request|" within 1 seconds

  @cap:analytics @feat:correlation-logging @rule:persistence @type:regression @dep:gateway @legacy:CorrelationLoggingTest
  Scenario: Correlation configuration survives a graceful restart
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "persistedApiId" and deployed it
    When I publish the "apis" resource with id "persistedApiId"
    Then The lifecycle status of API "persistedApiId" should be "Published"
    When I retrieve the "apis" resource with id "persistedApiId"
    And I extract response field "context" and store it as "persistedApiContext"
    When I have set up application with keys, subscribed to API "persistedApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    When I enable only the correlation logging components "http,method-calls"
    Then The response status code should be 200
    And The correlation configuration should have exactly the components "http,method-calls" enabled

    When I gracefully restart the API Manager server

    # The configuration is held in AM_CORRELATION_CONFIGS, not in a file, and is reloaded on boot — so it must
    # read back unchanged.
    When I retrieve the correlation logging configuration
    Then The response status code should be 200
    And The correlation configuration should have exactly the components "http,method-calls" enabled

    # ...and still be IN FORCE, which is the part that actually matters, so both components are re-driven with
    # the traffic that exercises them: a management-plane call for http, a gateway invocation for method-calls.
    # Legacy instead asserted the restart by looking for "Started log handler" in correlation.log — but the
    # LogsHandler constructor emits that line unconditionally at every boot, whatever the component settings,
    # so it only ever proved the server came back, never that anything persisted.
    When I mark the current end of the server log file "correlation.log"
    And I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200
    And The server log file "correlation.log" should gain a line containing "|HTTP-In-Request|" within 60 seconds

    When I mark the current end of the server log file "correlation.log"
    And I invoke the API at gateway context "{{persistedApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "correlation.log" should gain a line containing "|METHOD|" within 60 seconds
    # synapse was NOT enabled before the restart, so it must not have come back on.
    And The server log file "correlation.log" should gain no line containing "|HTTP State Transition|" within 1 seconds

    # Leave the container as it was found, so a sibling class in this block is not silently logging.
    When I enable only the correlation logging components ""
    Then The response status code should be 200

  # The correlation switch is one server-global document (no tenant path and no tenant key in
  # AM_CORRELATION_CONFIGS). This scenario pins the distinct question a tenant actor adds: whether a tenant
  # administrator holding apim:admin may read and write that shared document. Verified behavior: GET is allowed,
  # while PUT is refused by the logging-specific permission gate with 403 / 900915.
  @cap:analytics @feat:correlation-logging @rule:tenant-admin-access @type:regression
  Scenario: A tenant administrator can read but cannot modify the global correlation configuration
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I retrieve the correlation logging configuration
    Then The response status code should be 200
    When I enable only the correlation logging components ""
    Then The response status code should be 403
    And The error response should have code "900915" and message "Invalid Permission"
