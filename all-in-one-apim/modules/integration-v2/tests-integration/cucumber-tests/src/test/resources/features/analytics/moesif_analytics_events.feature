@moesif-analytics
Feature: Moesif Analytics Event Publishing

  What the gateway actually publishes to Moesif for a gateway invocation, and how the publisher behaves when
  Moesif is unreachable or is sampling.

  Covers the gateway-side request/response body capture (the capture itself, the unbuildable-content-type guard
  and the credential strip) together with the analytics-publisher side that maps those onto Moesif's native body
  fields, plus the publisher's dynamic sampling and retry queue. Every assertion here is one no unit test can
  make: the units on both sides mock the message builder and the Moesif SDK away, so nothing else in either repo
  proves the configuration reaches the publisher, that the body survives the proxy, or that the two repositories'
  property-key literals agree.

  ASSERTIONS READ A MOCK COLLECTOR. The block points the publisher's base URL at the moesif-collector node app,
  which serves the three routes the Moesif SDK calls and records what it receives. Because analytics is a
  container-global switch, that collector sees an event for every invocation in this block, so each scenario takes
  its own trace id and stamps it into a request header the collector buckets by. Counts are asserted SETTLED
  rather than on reaching a value, so a duplicate delivery on the retry queue's recovery drain fails rather than
  passing.

  WHAT IS ASSERTED ABOUT THE FORWARDED BODY. Capture forces the message to be built, which re-serializes it on the
  way out. Content types with a matched builder/formatter pair keep their content but not necessarily their bytes:
  whitespace, attribute and namespace ordering, JSON key formatting and chunking may all differ, so a signature
  computed over raw body bytes may fail to verify at the backend while capture is on. Scenarios that send such a
  body assert CONTENT equality. Exact bytes are asserted, through digest headers the backend sets, only where they
  legitimately hold: a body that is never built, or one whose formatter writes it back verbatim. Check which of the
  two applies before tightening an assertion.

  Background:
    Given The system is ready
    # The acting actor leaks across scenarios and a Scenario Outline leaves it on its LAST Examples row, so it is
    # pinned here. The outlines below override it per row.
    And I act as "admin"
    # Fixtures every scenario draws from, defined once so no multi-line literal is repeated per scenario.
    And I put the following JSON payload in context as "moesifJsonBody"
    """
    {"orderId": "moesif-order-1", "quantity": 7}
    """
    And I put the following text in context as "moesifBinaryBody"
    """
    moesif-binary-payload
    """
    And I put the following text in context as "moesifSkipBody"
    """
    moesif-skipped-body
    """
    And I put the following text in context as "moesifXmlBody"
    """
    <order><first>alpha</first><second>beta</second></order>
    """

  @cap:analytics @feat:analytics-events @rule:config-plumbing @type:smoke
  Scenario: Retry queue and dynamic sampling report the configured values at startup
    # The cheapest scenario here and the only test anywhere, unit or integration, that proves the UNDERSCORED
    # property names survive the whole chain from deployment.toml through the rendered api-manager.xml and the
    # configuration holder into the reporter's constructor. Those keys were renamed from dotted to underscored
    # form late in development and nothing verifies the rename took; a silent revert would leave every value at
    # its default, which every other scenario here would still pass with. The values asserted are exactly the ones
    # the block overlay sets, so a default creeping back in fails this.
    # Log-only, no gateway traffic. Needs the block's log4j2 fixture: these are INFO lines from the publisher,
    # which the distribution's ERROR root logger would otherwise swallow.
    # WHOLE-FILE, not gain-since-a-mark: both lines are written when the gateway component binds the analytics
    # configuration at BOOT, so any mark this scenario could take is already after them.
    Then The server log file "wso2carbon.log" should contain a line containing "Moesif retry queue enabled (capacity=10000 events per Moesif key, check interval=2s, log every 2 checks, catch-up burst=5 sends with 100ms delay between sends)" within 60 seconds
    And The server log file "wso2carbon.log" should contain a line containing "Moesif dynamic sampling enabled (refresh=5000ms, fallbackRate=100)" within 60 seconds

  @cap:analytics @feat:analytics-events @rule:body-capture @type:smoke
  Scenario Outline: A JSON request and response body reach Moesif as native structured bodies as <actor>
    # The end-to-end wire contract. The gateway stashes the request body under its own namespaced message-context
    # keys and republishes it under the wire keys the publisher reads; the publisher removes those from the event
    # properties and maps them onto Moesif's native request and response body fields. Those key literals are
    # duplicated across the two repositories and no test in either checks they match, so this is what fails if one
    # side is renamed.
    # Structured, not stringified, is half the assertion: a JSON body is meant to arrive as a deserialized object
    # so it is searchable in Moesif. A body that arrived as an opaque string would satisfy a substring check.
    Given I act as "<actor>"
    And I have a Moesif analytics trace key stored as "jsonBodyTrace"
    When I invoke the API at gateway context "{{moesifApiContext<suffix>}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken<suffix>" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{jsonBodyTrace}}" until response status code becomes 200 within 60 seconds
    Then the Moesif event for trace key "jsonBodyTrace" should be the only one and stored as "jsonBodyEvent"
    # Request side: the exact object that was sent, as an object.
    And the Moesif event "jsonBodyEvent" should have "request" body field "orderId" equal to "moesif-order-1"
    And the Moesif event "jsonBodyEvent" should have "request" body field "quantity" equal to "7"
    # Response side: the backend echoed it back with the same Content-Type, so it is structured too.
    And the Moesif event "jsonBodyEvent" should have "response" body field "orderId" equal to "moesif-order-1"
    # The captured media type overrides the publisher's default placeholder, which is what lets Moesif parse the
    # body rather than treating it as opaque text.
    And the Moesif event "jsonBodyEvent" should have "request" header "Content-Type" equal to "application/json"
    # The content types deliberately REMAIN in metadata after the bodies are removed from it: unlike a body, a
    # content type is useful metadata. This is the assertion that fails if the body removal ever over-reaches and
    # takes the content type with it.
    And the Moesif event "jsonBodyEvent" should have metadata "requestContentType" equal to "application/json"

    Examples:
      | actor             | suffix       |
      | admin             |              |
      | admin@tenant1.com | @tenant1.com |

  @cap:analytics @feat:analytics-events @rule:body-capture @type:regression
  Scenario: A binary request body is published Base64-encoded and flagged base64
    # application/octet-stream has a registered message builder in the distribution, so this reaches the binary
    # branch: the gateway Base64-encodes the decoded bytes and flags the transfer encoding, and the publisher
    # passes that string through VERBATIM rather than encoding it again. A double-encoding regression is exactly
    # what the decode below catches.
    # Decoded with a MIME decoder because the SDK's encoder line-wraps; the payload is ASCII because that same
    # encoder uses the platform default charset.
    Given I have a Moesif analytics trace key stored as "binaryBodyTrace"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body" with method "POST" using access token "moesifAccessToken" and payload "moesifBinaryBody" with content type "application/octet-stream" and request headers "X-Trace-Id={{binaryBodyTrace}}" until response status code becomes 200 within 60 seconds
    Then the Moesif event for trace key "binaryBodyTrace" should be the only one and stored as "binaryBodyEvent"
    And the Moesif event "binaryBodyEvent" should have a base64 "request" body decoding to "moesif-binary-payload"

  @cap:analytics @feat:analytics-events @rule:body-capture @type:negative
  Scenario: A body one byte over the limit is dropped whole and still reaches the backend in full
    # The size limit is a DROP, never a truncation: Moesif gets a whole valid body or none. The block sets the
    # limit to 2000 bytes, so 2001 is one byte over.
    # Both halves are the point. That no request body is published proves the pre-build length gate fired. That
    # the backend echoed all 2001 characters back proves the gate dropped only the ANALYTICS copy and left the
    # proxied message alone, which is the failure a body-capture bug would actually cause in production.
    Given I have a Moesif analytics trace key stored as "oversizeTrace"
    And I put a 2001 byte text payload in context as "moesifOversizeBody"
    # Marked BEFORE the invocation: the drop line is caused by this call, and an identical line from an earlier
    # scenario must not satisfy the assertion.
    And I mark the current end of the server log file "wso2carbon.log"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-sensitive" with method "POST" using access token "moesifAccessToken" and payload "moesifOversizeBody" with content type "text/plain" and request headers "X-Trace-Id={{oversizeTrace}}" until response status code becomes 200 within 60 seconds
    Then the backend should have received exactly the body "moesifOversizeBody"
    And the Moesif event for trace key "oversizeTrace" should be the only one and stored as "oversizeEvent"
    And the Moesif event "oversizeEvent" should have no "request" body
    # The documented troubleshooting signal for this case, and the block's payload-capture DEBUG logger is the
    # same one the documentation tells operators to switch on when a body is unexpectedly missing.
    # Names the REQUEST direction explicitly. The message is otherwise identical for the response leg, and the
    # response of this very call is also oversize — so a direction-less assertion passes on the wrong body, which
    # is exactly what happened on the first run.
    # This is the POST-build drop: the shared test client sends chunked (no Content-Length), so the pre-build gate
    # cannot fire and the body is materialised before the byte check rejects it. Whichever branch drops it, the
    # published event carries no body — that is the invariant the assertion above pins.
    And The server log file "wso2carbon.log" should gain a line containing "Dropping request text body from analytics: 2001 bytes exceeds payload_size_limit of 2000" within 60 seconds

  @cap:analytics @feat:analytics-events @rule:body-capture @type:regression
  Scenario: A body exactly at the limit is still captured
    # The other side of the boundary, so the drop above is attributable to the limit rather than to text capture
    # being broken outright. Exactly 2000 bytes against a limit of 2000: the comparison is strictly greater-than,
    # so this must be captured.
    Given I have a Moesif analytics trace key stored as "atLimitTrace"
    And I put a 2000 byte text payload in context as "moesifAtLimitBody"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body" with method "POST" using access token "moesifAccessToken" and payload "moesifAtLimitBody" with content type "text/plain" and request headers "X-Trace-Id={{atLimitTrace}}" until response status code becomes 200 within 60 seconds
    Then the Moesif event for trace key "atLimitTrace" should be the only one and stored as "atLimitEvent"
    # A text/plain body is published Base64-encoded and flagged base64. That is the Moesif SDK's convention, not a
    # gateway transformation: the gateway captures the text unchanged, and the publisher hands any body that is not
    # JSON to Moesif's BodyParser, which Base64-encodes it. Moesif decodes Base64 bodies for display, so the body
    # remains readable there. Decoding it here proves the whole body was captured rather than a prefix of it.
    And the Moesif event "atLimitEvent" should have a base64 "request" body decoding to "{{moesifAtLimitBody}}"

  @cap:analytics @feat:analytics-events @rule:body-capture @type:negative
  Scenario Outline: A <case> request body is never captured but still reaches the backend intact
    # The three ways a body is legitimately skipped by content type, each of which must leave the proxied message
    # untouched:
    #   form-urlencoded / multipart  on the gateway's content-type deny list
    #   unregistered                 no registered message builder for the type. This row is the regression test
    #                                for a real corruption bug: before that guard was added, the build was
    #                                ATTEMPTED for an arbitrary binary type, threw on the first non-UTF-8 byte,
    #                                and had already consumed the pass-through pipe, so the body forwarded to the
    #                                backend was damaged even though the exception was swallowed.
    # The echoed body is the intact-forwarding half. Asserting only the absent capture would pass on that bug.
    Given I have a Moesif analytics trace key stored as "skipTrace"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-sensitive" with method "POST" using access token "moesifAccessToken" and payload "moesifSkipBody" with content type "<contentType>" and request headers "X-Trace-Id={{skipTrace}}" until response status code becomes 200 within 60 seconds
    # Read from the digest headers the backend sets, NOT from the echoed body. Capture can rebuild and re-serialize
    # the message on the response leg too, so the echoed body cannot isolate the request leg. Headers are untouched
    # by the message builder.
    Then the backend should have received exactly the body "moesifSkipBody"
    And the Moesif event for trace key "skipTrace" should be the only one and stored as "skipEvent"
    And the Moesif event "skipEvent" should have no "request" body

    Examples:
      | case            | contentType                       |
      | form-urlencoded | application/x-www-form-urlencoded |
      | multipart       | multipart/form-data               |
      | unregistered    | application/vnd.test+weird        |

  @cap:analytics @feat:analytics-events @rule:body-capture @type:negative
  Scenario: A GET carries no request body into the analytics event
    # The no-entity-body guard. Cheap, and it is the case that shows an absent request body is a real decision
    # rather than capture never working for this API at all, since the scenarios above capture on the same API.
    Given I have a Moesif analytics trace key stored as "noBodyTrace"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "moesifAccessToken" and payload "" with content type "application/json" and request headers "X-Trace-Id={{noBodyTrace}}" until response status code becomes 200 within 60 seconds
    Then the Moesif event for trace key "noBodyTrace" should be the only one and stored as "noBodyEvent"
    And the Moesif event "noBodyEvent" should have no "request" body

  @cap:analytics @feat:analytics-events @rule:header-redaction @type:regression
  Scenario Outline: Credential and session headers never reach Moesif in either direction as <actor>
    # The security assertion of this feature, and the reason the strip is unconditional rather than configurable:
    # Moesif is a third party, so a leaked authorization header is a credential disclosure outside the deployment.
    # The credential and session headers are removed whatever the configuration says.
    # The api-key header is sent LOWER CASE deliberately: matching is case-insensitive precisely so an HTTP/2
    # client, which sends header names in lower case, is not treated differently. Asserting only the canonical
    # spelling would pass while the lower-case form leaked.
    # The trace header surviving with its exact value is the CONTROL. Without it every assertion here would also
    # pass if header publishing were simply switched off, which is the classic vacuous negative.
    Given I act as "<actor>"
    And I have a Moesif analytics trace key stored as "redactionTrace"
    When I invoke the API at gateway context "{{moesifApiContext<suffix>}}/1.0.0/reflect-body-sensitive" with method "POST" using access token "moesifAccessToken<suffix>" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{redactionTrace}},apikey=must-never-reach-moesif,Cookie=analyticsSession=must-never-reach-moesif" until response status code becomes 200 within 60 seconds
    # The backend really did set a cookie, so the response-side assertion below is about a header that existed.
    Then The response header "Set-Cookie" should contain "analyticsSession"
    And the Moesif event for trace key "redactionTrace" should be the only one and stored as "redactionEvent"
    And the Moesif event "redactionEvent" should have "request" header "X-Trace-Id" equal to "{{redactionTrace}}"
    And the Moesif event "redactionEvent" should have no "request" header "Authorization"
    And the Moesif event "redactionEvent" should have no "request" header "apikey"
    And the Moesif event "redactionEvent" should have no "request" header "Cookie"
    And the Moesif event "redactionEvent" should have no "response" header "Set-Cookie"

    Examples:
      | actor             | suffix       |
      | admin             |              |
      | admin@tenant1.com | @tenant1.com |

  @cap:analytics @feat:analytics-events @rule:header-redaction @type:regression
  Scenario: A rejected request publishes a fault event with no credential headers
    # The fault path is where the leak actually was. On a fault the transport-headers property can still be the
    # INBOUND REQUEST map, so publishing response headers raw disclosed the caller's authorization header; the
    # strip now runs on a copy of that map before it is published. The garbage token routes this through the fault
    # collector rather than the success one.
    # Exactly 401, not "401 or 403": garbage that no authenticator claims is a 401 at the gateway.
    Given I have a Moesif analytics trace key stored as "faultTrace"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{faultTrace}},Authorization=Bearer not-a-real-token" until response status code becomes 401 within 60 seconds
    Then the Moesif event for trace key "faultTrace" should be the only one and stored as "faultEvent"
    And the Moesif event "faultEvent" should have "request" header "X-Trace-Id" equal to "{{faultTrace}}"
    And the Moesif event "faultEvent" should have no "request" header "Authorization"
    And the Moesif event "faultEvent" should have no "response" header "Authorization"
    # Rejected before the backend call, so there is no request body to capture: the capture point is immediately
    # before the backend send.
    And the Moesif event "faultEvent" should have no "request" body

  @cap:analytics @feat:analytics-events @rule:header-redaction @type:regression
  Scenario: A configured header is masked in the event while its real value still reaches the backend
    # The masking half, which is the operator's answer to the one gap the unconditional strip leaves: a per-API
    # CUSTOM auth header name cannot be resolved at that layer, so it is not stripped and has to be masked by
    # configuration.
    # Asserted POSITIVELY as the mask token. Mask parsing fails SAFE: a mask configuration the gateway cannot
    # parse logs a warning and masks nothing, so "the real value is absent" would report that silent failure as a
    # pass. Sending the name lower case also pins the documented case-insensitive matching.
    # The reflect-headers leg is the other half: masking must change only what is PUBLISHED, never what the
    # backend receives.
    Given I have a Moesif analytics trace key stored as "maskTrace"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-sensitive" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{maskTrace}},x-custom-auth=real-custom-credential" until response status code becomes 200 within 60 seconds
    Then the Moesif event for trace key "maskTrace" should be the only one and stored as "maskEvent"
    And the Moesif event "maskEvent" should have "request" header "X-Custom-Auth" equal to "*****"
    And the Moesif event "maskEvent" should have "response" header "X-Account-Number" equal to "*****"
    When I have a Moesif analytics trace key stored as "maskBackendTrace"
    And I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "moesifAccessToken" and payload "" with content type "application/json" and request headers "X-Trace-Id={{maskBackendTrace}},x-custom-auth=real-custom-credential" until response status code becomes 200 within 60 seconds
    Then The response should contain "real-custom-credential"

  @cap:analytics @feat:analytics-events @rule:sampling @type:regression
  Scenario: A Moesif sample-rate change takes effect within one refresh window without a restart
    # The headline observable for dynamic sampling. The rates are NOT gateway configuration by design: they come
    # from Moesif's application config, which the publisher re-fetches on a fixed schedule. That is what makes
    # this assertable at all without bouncing the server, and what lets one container cover both states.
    # Rate 0 gives an exact, non-probabilistic assertion: nothing is published. A partial rate could not be
    # asserted by count, because the decision is taken per event at random.
    Given I have a Moesif analytics trace key stored as "sampledOutTrace"
    When the Moesif app config reports a sample rate of 0
    And I wait for the Moesif sampling config to be re-fetched within 60 seconds
    And I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{sampledOutTrace}}" until response status code becomes 200 within 60 seconds
    Then Moesif should have received exactly 0 events for trace key "sampledOutTrace" within 60 seconds
    # Back to publishing everything, so the zero above is attributable to the rate and not to the publisher having
    # stopped working.
    When the Moesif app config reports a sample rate of 100
    And I wait for the Moesif sampling config to be re-fetched within 60 seconds
    And I have a Moesif analytics trace key stored as "sampledInTrace"
    And I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{sampledInTrace}}" until response status code becomes 200 within 60 seconds
    Then Moesif should have received exactly 1 events for trace key "sampledInTrace" within 60 seconds
    # At full rate the weight is one, so Moesif extrapolates nothing.
    And every Moesif event for trace key "sampledInTrace" should have weight 1

  @cap:analytics @feat:analytics-events @rule:sampling @type:regression
  Scenario: A partially sampled event carries the extrapolation weight for its rate
    # The weight is what keeps aggregates correct when only a share of traffic is published, so it is the half of
    # sampling that silently breaks a customer's dashboard rather than their test. At half rate it is two.
    # Weight is only stamped while sampling is enabled, as it is in this block. With sampling disabled the publisher
    # leaves the Moesif SDK's default weight of 0 on the event; that is the SDK's own behaviour, so it is not
    # asserted here.
    # No count assertion here on purpose: at a partial rate the number of events that arrive is random per event,
    # so a count would be flaky by construction. The weight is deterministic, and asserting it on EVERY event that
    # did arrive is exact without being probabilistic. Ten invocations make an empty sample vanishingly unlikely,
    # and the step fails rather than passing vacuously if none arrived.
    Given I have a Moesif analytics trace key stored as "weightTrace"
    When the Moesif app config reports a sample rate of 50
    And I wait for the Moesif sampling config to be re-fetched within 60 seconds
    And I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{weightTrace}}" 10 times expecting status 200
    Then Moesif should have been offered at least 1 events for trace key "weightTrace" within 60 seconds
    And every Moesif event for trace key "weightTrace" should have weight 2

  @cap:analytics @feat:analytics-events @rule:retry-queue @type:regression
  Scenario: Events survive a Moesif outage and are delivered exactly once on recovery
    # The retry queue's whole arc: a retryable failure opens the circuit for that Moesif key, subsequent batches
    # are stashed without an HTTP call, a probe on a fixed interval retries the oldest batch, and on success a
    # catch-up burst drains the backlog.
    # 503 because the retryable set is the server errors plus request-timeout and too-many-requests. The offered
    # assertion is load-bearing: "0 accepted" on its own is equally consistent with the gateway never having
    # published, which would make this pass for the wrong reason.
    # The recovered count is asserted SETTLED, not on reaching three. The recovery drain is exactly where a
    # duplicate delivery would appear, and an accept-on-reaching-N loop returns the instant it sees three, leaving
    # a fourth that arrives moments later invisible.
    Given I have a Moesif analytics trace key stored as "outageTrace"
    And I mark the current end of the server log file "wso2carbon.log"
    When the Moesif collector starts rejecting events with status 503
    And I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{outageTrace}}" 3 times expecting status 200
    Then Moesif should have been offered at least 1 events for trace key "outageTrace" within 60 seconds
    And The server log file "wso2carbon.log" should gain a line containing "Cannot reach Moesif (key ...-key)" within 60 seconds
    When the Moesif collector starts accepting events again
    Then Moesif should have received exactly 3 events for trace key "outageTrace" within 120 seconds
    And The server log file "wso2carbon.log" should gain a line containing "is reachable again after" within 60 seconds

  @cap:analytics @feat:analytics-events @rule:retry-queue @type:negative
  Scenario: A non-retryable rejection drops the events instead of queueing them
    # The other side of the retry decision. A 401 from Moesif is permanent, since a bad collector key will never
    # start working by being retried, so those events are dropped and logged rather than queued. Widening the
    # retryable set would turn a misconfigured key into an unbounded in-heap queue, which is what this guards.
    # Asserting the count is still zero AFTER the collector is healthy again is what distinguishes dropped from
    # queued: a queued batch would drain the moment it recovered.
    Given I have a Moesif analytics trace key stored as "droppedTrace"
    And I mark the current end of the server log file "wso2carbon.log"
    When the Moesif collector starts rejecting events with status 401
    And I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifJsonBody" with content type "application/json" and request headers "X-Trace-Id={{droppedTrace}}" 2 times expecting status 200
    Then Moesif should have been offered at least 1 events for trace key "droppedTrace" within 60 seconds
    # The onFailure client-error branch, not the onSuccess "No retry." branch: a 401 makes the Moesif SDK's
    # validateResponse throw, so the callback arrives as a FAILURE carrying the status rather than as a
    # non-retryable success. Measured on the first run, which is why the wording is this specific.
    And The server log file "wso2carbon.log" should gain a line containing "publishing failed. Moesif returned 401 due to error" within 60 seconds
    When the Moesif collector starts accepting events again
    Then Moesif should have received exactly 0 events for trace key "droppedTrace" within 60 seconds

  @cap:analytics @feat:analytics-events @rule:body-capture @type:regression
  Scenario: Capture does not damage the message the backend receives
    # The single highest-value assertion in this feature, because it is the one thing every unit test on both
    # sides mocks away: the message builder is statically mocked in all of them, so no test anywhere proves the
    # forwarded body survives capture. Enabling capture makes the gateway build and then re-serialize the message
    # rather than streaming it, and this is where that goes wrong.
    # XML because it is the format re-serialization is most likely to alter, and because the XML branch
    # concatenates ALL child elements of the body rather than the first, so a regression there would silently
    # publish a partial body.
    # CONTENT equality, not byte equality: XML has a matched builder/formatter pair, so its content survives
    # re-serialization but its bytes are not guaranteed to (see the feature header). Do not tighten this.
    Given I have a Moesif analytics trace key stored as "fidelityTrace"
    When I invoke the API at gateway context "{{moesifApiContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "moesifAccessToken" and payload "moesifXmlBody" with content type "application/xml" and request headers "X-Trace-Id={{fidelityTrace}}" until response status code becomes 200 within 60 seconds
    # Every element the request carried came back from the backend, so nothing was lost in the round trip.
    Then The response should contain "<first>alpha</first>"
    And The response should contain "<second>beta</second>"
    And the Moesif event for trace key "fidelityTrace" should be the only one and stored as "fidelityEvent"
    And the Moesif event "fidelityEvent" should have "request" header "Content-Type" equal to "application/xml"
    # The captured body must contain BOTH elements. This is the all-children assertion: the XML branch concatenates
    # every child of the body rather than only the first, so dropping to one element would be a silent partial
    # capture. Asserted through the Base64 decode because the publisher, following the Moesif SDK's convention,
    # Base64-encodes any body that is not JSON — the same as plain text in the at-limit scenario.
    And the Moesif event "fidelityEvent" should have a base64 "request" body decoding to "{{moesifXmlBody}}"
