@cleanup
Feature: Publisher Streaming API Design

  Publisher-plane streaming API design: create + deploy a WebSocket API and publish it, plus AsyncAPI (v2/v3)
  definition import (advertise-only / third-party) with parser validation. Asserts only publisher-plane outcomes —
  WebSocket invocation is covered by gateway invocation, and advertise-only AsyncAPIs are not gateway-routed.
  Self-contained scenarios, torn down by the per-scenario cleanup hook. This feature's block sets
  use_legacy_async_parser=false (the v2 default is the legacy parser, which skips strict AsyncAPI-v2 validation).

  @cap:publisher @feat:streaming-design @type:smoke @legacy:WebSocketAPITestCase
  Scenario Outline: Create, deploy and publish a WebSocket API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_websocket_api.json" as "websocketApiId" and deployed it
    When I publish the "apis" resource with id "websocketApiId"
    Then The lifecycle status of API "websocketApiId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # SSE (type=SSE): create + deploy + publish, mirroring the WebSocket scenario. Management-plane only — SSE
  # gateway invocation needs an SSE backend (a separate backlog infra item), so it is out of scope here. The
  # create payload uses an ordinary HTTP endpoint (endpoint_type "http") and the AsyncUnlimited subscription
  # tier. Runs in both tenants (×2). Ports ServerSentEventsAPITestCase#testPublishSseApi.
  @cap:publisher @feat:streaming-design @type:smoke @legacy:ServerSentEventsAPITestCase
  Scenario Outline: Create, deploy and publish an SSE API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_sse_api.json" as "sseApiId" and deployed it
    When I publish the "apis" resource with id "sseApiId"
    Then The lifecycle status of API "sseApiId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # WebSub (type=WEBSUB): create + deploy + publish, carrying the websubSubscriptionConfiguration (secret /
  # signingAlgorithm SHA1 / signatureHeader x-hub-signature) inline in the create payload, mirroring the legacy
  # DTO update. Uses the AsyncWHUnlimited subscription tier. Management-plane only — WebSub gateway invocation
  # (webhook subscribe/publish) needs a callback-receiver backend (a separate backlog infra item), so it is out
  # of scope here. Runs in both tenants (×2). Ports WebSubAPITestCase#testPublishWebSubApi.
  @cap:publisher @feat:streaming-design @type:smoke @legacy:WebSubAPITestCase
  Scenario Outline: Create, deploy and publish a WebSub API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_websub_api.json" as "websubApiId" and deployed it
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # AsyncAPI import (advertise-only / third-party): an ASYNC API can only be created as a third-party API, so the
  # import carries advertiseInfo.advertised=true. Covers the new v2 & v3 AsyncAPI parser (import → 201 → publish),
  # the third-party-only guard, and spec-validation rejections. Management-plane only — an advertise-only API is
  # not gateway-routed (revisions/deploy are unsupported), so no runtime is needed. Runs in both tenants (×2).
  # Ports AsyncAPITestWithValidationCase.
  @cap:publisher @feat:streaming-design @rule:async-import @type:regression @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Import and publish an advertise-only AsyncAPI v2 definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/asyncapiv2.yaml" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json" as "asyncV2ApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "asyncV2ApiId"
    Then The lifecycle status of API "asyncV2ApiId" should be "Published"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:async-import @type:regression @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Import and publish an advertise-only AsyncAPI v3 definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/asyncapiv3.yaml" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json" as "asyncV3ApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "asyncV3ApiId"
    Then The lifecycle status of API "asyncV3ApiId" should be "Published"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # ---- Advertise-only consequences: no revisions, but still discoverable and subscribable ----------------
  # W6. Three behaviours legacy asserted that v2 did not: revision creation is refused, the published API is
  # visible in the publisher listing, and a subscription to it lands UNBLOCKED. None of them depends on which
  # AsyncAPI parser is configured, which is why they are covered once here rather than in a second container
  # (see the parser note below).
  #
  # THE PARSER CONFLICT, RESOLVED — read this before "fixing" the :105 rejection row. The audit flagged that
  # legacy's async/asyncapi.yaml is byte-identical to v2's and yet legacy asserts it imports 201 while
  # streaming_design.feature asserts the SAME file is rejected 400 "Implicit OAuth Flow is missing", concluding
  # "both cannot hold on one build". They never were on one build: legacy has TWO classes with TWO tomls —
  # AsyncAPITestCase runs configFiles/streamingAPIs/legacyAsync (use_legacy_async_parser = TRUE, lenient, so the
  # file imports) and AsyncAPITestWithValidationCase runs configFiles/streamingAPIs/async
  # (use_legacy_async_parser = FALSE, strict, so it is rejected). This block carries the strict setting via the
  # backendJwtUrlSafe overlay, so the 400 here is correct. Both assertions are right.
  @cap:publisher @feat:streaming-design @rule:async-import @type:negative @legacy:AsyncAPITestCase @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Creating a revision of an advertise-only AsyncAPI is refused (<def>) as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "<def>" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json" as "asyncRevApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "asyncRevPayload"
    """
    {"description":"advertise-only revision attempt"}
    """
    # Asserted EXACTLY, unlike legacy. Legacy wrapped this in try/catch and asserted
    #   e.getMessage().contains("Error while adding new API Revision for API : ")
    #     || e.getMessage().contains("Creating API Revisions is not supported")
    # which (a) accepts either message and (b) passes SILENTLY when no exception is thrown at all — the
    # rejection could stop happening and legacy would stay green. 903225 is the third-party-revision guard,
    # the same code the Solace work pinned for an advertised API.
    And I attempt to create a revision for "apis" resource "asyncRevApiId" with payload "asyncRevPayload"
    Then The response status code should be 400
    And The error response should have code "903225" and message "Creating API Revisions is not supported for third party APIs"

    Examples:
      | actor                     | def                                      |
      | publisherUser             | artifacts/payloads/async/asyncapiv2.yaml |
      | publisherUser             | artifacts/payloads/async/asyncapiv3.yaml |
      | publisherUser@tenant1.com | artifacts/payloads/async/asyncapiv2.yaml |
      | publisherUser@tenant1.com | artifacts/payloads/async/asyncapiv3.yaml |

  # Legacy asserted this with APIMTestCaseUtils.isAPIAvailable over the publisher's full API list, i.e. that an
  # advertise-only API is a first-class publisher artifact despite never being deployed to a gateway.
  @cap:publisher @feat:streaming-design @rule:async-import @type:regression @legacy:AsyncAPITestCase
  Scenario Outline: A published advertise-only AsyncAPI appears in the publisher API listing as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/asyncapiv2.yaml" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json" as "asyncListApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "asyncListApiId"
    Then The lifecycle status of API "asyncListApiId" should be "Published"
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "asyncListApiId" should be in the list of all APIS

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Subscription to an advertise-only ASYNC API. Tagged @cap:devportal but kept in this publisher file on
  # purpose (CLAUDE.md §2): it is an intrinsic facet of the cohesive async-import arc — import -> publish ->
  # subscribe, the same chain legacy expressed with dependsOnMethods — not a devportal scenario merely parked
  # here. The acting actor is admin rather than publisherUser because a
  # publisherUser holds no application-manage scope and POST /applications answers 401 for it.
  # UNBLOCKED is the load-bearing value: it says the subscription is immediately usable, NOT parked ON_HOLD by a
  # workflow — asserted as the status FIELD rather than a body substring, since ON_HOLD would also contain the
  # API's id. AsyncWHUnlimited is the tier the advertised props declare; a tier outside `policies` is refused
  # 400 before any subscription is created.
  @cap:devportal @feat:streaming-design @rule:async-import @type:regression @dep:publisher @legacy:AsyncAPITestCase @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Subscribing to an advertise-only AsyncAPI lands UNBLOCKED (<def>) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import asyncapi definition from "<def>" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json" as "asyncSubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "asyncSubApiId"
    Then The lifecycle status of API "asyncSubApiId" should be "Published"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "asyncSubAppPayload"
    And I create an application with payload "asyncSubAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "asyncSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncWHUnlimited"}
    """
    And I subscribe to API "asyncSubApiId" using application "createdAppId" with payload "asyncSubPayload" as "asyncSubId"
    Then The response status code should be 201
    And The value of response field "status" should be "UNBLOCKED"

    Examples:
      | actor             | def                                      |
      | admin             | artifacts/payloads/async/asyncapiv2.yaml |
      | admin             | artifacts/payloads/async/asyncapiv3.yaml |
      | admin@tenant1.com | artifacts/payloads/async/asyncapiv2.yaml |
      | admin@tenant1.com | artifacts/payloads/async/asyncapiv3.yaml |

  # An ASYNC API imported WITHOUT advertiseInfo (i.e. as a normal managed API) is rejected — for both v2 and v3,
  # in both tenants.
  @cap:publisher @feat:streaming-design @rule:async-import @type:negative @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Creating an AsyncAPI as a non-advertised API is rejected (<def>) as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to import asyncapi definition from "<def>" with additional properties "artifacts/payloads/async/asyncapi_noadvertise_props.json"
    Then The response status code should be 400
    And The response should contain "ASYNC type APIs only can be created as third party APIs"

    Examples:
      | actor                     | def                                      |
      | publisherUser             | artifacts/payloads/async/asyncapiv2.yaml |
      | publisherUser             | artifacts/payloads/async/asyncapiv3.yaml |
      | publisherUser@tenant1.com | artifacts/payloads/async/asyncapiv2.yaml |
      | publisherUser@tenant1.com | artifacts/payloads/async/asyncapiv3.yaml |

  # Spec validation: the new parser rejects a malformed AsyncAPI v2 (implicit OAuth flow missing its authorization
  # URL) and a malformed AsyncAPI v3 (an operation whose channel $ref points to a non-existent channel).
  @cap:publisher @feat:streaming-design @rule:async-import @type:negative @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Importing an invalid AsyncAPI v2 definition is rejected by the parser as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to import asyncapi definition from "artifacts/payloads/async/asyncapi.yaml" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json"
    Then The response status code should be 400
    And The response should contain "Implicit OAuth Flow is missing"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:async-import @type:negative @legacy:AsyncAPITestWithValidationCase
  Scenario Outline: Importing an invalid AsyncAPI v3 definition is rejected by the parser as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to import asyncapi definition from "artifacts/payloads/async/asyncapiv3_invalid.yaml" with additional properties "artifacts/payloads/async/asyncapi_advertised_props.json"
    Then The response status code should be 400
    And The response should contain "Operation channel reference must point to valid channel."

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:streaming-design @type:negative @legacy:WebSocketAPITestCase
  # Uses a WS-TYPED payload, so the refusal is asserted for a streaming API rather than a REST one. Previously
  # this posted the plain REST payload, which made it byte-identical to the api-lifecycle copy — it proved
  # nothing about streaming-API creation while counting as streaming-design coverage.
  Scenario Outline: A subscriber-role user cannot create a WebSocket API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_websocket_api.json" in context as "subscriberWsPayload"
    And I attempt to create an "apis" resource with payload "subscriberWsPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # Negative: creating a WebSocket API whose context is malformed (an illegal {version} placement) is rejected
  # with 400 at the publisher. Ports WebSocketAPITestCase#testCreateWebSocketAPIWithMalformedContext.
  @cap:publisher @feat:streaming-design @rule:malformed-context @type:negative @legacy:WebSocketAPITestCase
  Scenario Outline: Creating a WebSocket API with a malformed context is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_ws_malformed_context_api.json" in context as "wsMalformedPayload"
    And I attempt to create an "apis" resource with payload "wsMalformedPayload"
    Then The response status code should be 400

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |
