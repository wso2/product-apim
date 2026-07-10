@cleanup
Feature: Publisher Streaming API Design

  Publisher-plane streaming (WebSocket) API design: create + deploy a WebSocket API and publish it.
  Asserts only publisher-plane outcomes — WebSocket invocation is covered by gateway invocation.
  Self-contained scenario, torn down by the per-scenario cleanup hook.

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
