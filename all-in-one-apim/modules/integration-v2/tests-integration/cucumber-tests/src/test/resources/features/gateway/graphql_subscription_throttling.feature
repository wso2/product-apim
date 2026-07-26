@cleanup
Feature: Gateway GraphQL Subscription Throttling

  Gateway-plane throttling of a GraphQL subscription over WebSocket: an API-level advanced policy set as the
  subscription API's apiThrottlingPolicy is enforced per WebSocket frame — once the quota is exceeded the gateway
  returns a 4003 "Websocket frame throttled out" error on the graphql-ws stream. Two quota types are covered, both
  of which enforce here (the graphql-ws path is operation-aware, so the WS-inbound frame-throttle handler counts
  each `start` frame): REQUEST-COUNT and request-BANDWIDTH. Ports the throttling case of GraphqlSubscriptionTestCase
  (request-count) plus the documented common bandwidth quota. Single-tenant (super) — throttling is API-scoped and
  the graphql-ws handshake is expensive to repeat ×2. (Event-count throttling is NOT applicable to GraphQL per docs
  — it is a Streaming-API (WebSocket/SSE/WebSub) quota only.)

  @cap:gateway @feat:graphql-invocation @rule:subscription-throttling @type:regression @dep:admin @legacy:GraphqlSubscriptionTestCase
  Scenario: A GraphQL subscription is throttled once it exceeds its API-level request-count limit
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create an advanced throttling policy "${UNIQUE:gqlSubThr4}" allowing 4 requests per minute
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubPayload" as "gqlSubApiId"
    When I retrieve the "apis" resource with id "gqlSubApiId"
    And I put the response payload in context as "gqlSubApiPayload"
    When I update the "apis" resource "gqlSubApiId" and "gqlSubApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    And I deploy the API with id "gqlSubApiId"
    When I publish the "apis" resource with id "gqlSubApiId"
    Then The lifecycle status of API "gqlSubApiId" should be "Published"
    When I retrieve the "apis" resource with id "gqlSubApiId"
    And I extract response field "context" and store it as "gqlSubContext"
    When I have set up application with keys, subscribed to API "gqlSubApiId", and obtained access token for "gqlSubSubId"
    Then The response status code should be 200
    # Send 10 subscription frames on a 4/min API-level limit — the gateway must throttle a frame (4003)
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token "generatedAccessToken" sending 10 frames expecting frame throttling

  @cap:gateway @feat:graphql-invocation @rule:subscription-throttling @type:regression @dep:admin @legacy:GraphqlSubscriptionTestCase
  Scenario: A GraphQL subscription is throttled once it exceeds its API-level request-bandwidth quota
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create an advanced throttling policy "${UNIQUE:gqlSubBw1kb}" allowing 1 KB per minute
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubBwPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubBwPayload" as "gqlSubBwApiId"
    When I retrieve the "apis" resource with id "gqlSubBwApiId"
    And I put the response payload in context as "gqlSubBwApiPayload"
    When I update the "apis" resource "gqlSubBwApiId" and "gqlSubBwApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    And I deploy the API with id "gqlSubBwApiId"
    When I publish the "apis" resource with id "gqlSubBwApiId"
    Then The lifecycle status of API "gqlSubBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "gqlSubBwApiId"
    And I extract response field "context" and store it as "gqlSubBwContext"
    When I have set up application with keys, subscribed to API "gqlSubBwApiId", and obtained access token for "gqlSubBwSubId"
    Then The response status code should be 200
    # ~10 frames of ~250B each (~2.5 KB) exceed the 1 KB/min bandwidth quota → the gateway must throttle a frame (4003)
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubBwContext}}/1.0.0" with query "subscription { liftStatusChange { name id status night capacity } }" using access token "generatedAccessToken" sending 10 frames expecting frame throttling
