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
  Scenario Outline: A subscriber-role user cannot create an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
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
