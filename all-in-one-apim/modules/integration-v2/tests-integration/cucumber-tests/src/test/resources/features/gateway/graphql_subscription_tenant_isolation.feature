@cleanup
Feature: Gateway GraphQL Subscription Tenant Isolation Under Concurrent Handshakes

  Regression guard for a gateway WebSocket tenant-flow leak. The WS inbound's handshake handler starts a
  carbon tenant flow and sets the handshake's tenant on the shared netty event-loop thread but never ends it,
  so the tenant leaks onto that thread. A GraphQL-subscription FRAME auth starts no tenant flow of its own and
  therefore inherits whatever tenant a preceding handshake leaked; a valid carbon.super token then resolves the
  wrong key manager and is refused with WS close 4001 "Invalid JWT token". The defect is intermittent in CI —
  it needs a concurrent tenant handshake to land on the same event-loop thread between the victim's handshake
  and its first frame.

  This scenario reproduces it deterministically: publish a carbon.super GraphQL subscription API (the victim)
  and a tenant1.com WS API (the leak source), then open the victim subscription and, during the pre-init idle
  window, flood many concurrent tenant1.com handshakes (each reaches the gateway's tenant-flow start before
  failing auth, poisoning every event-loop thread) before sending connection_init. The assertion is the CORRECT
  invariant: the subscription must authenticate (connection_ack, no 4001) despite the flood. On the current
  (unfixed) gateway this assertion FAILS with the 4001 — that failure is the proof the bug reproduces; once the
  end-tenant-flow fix ships in the gateway jar it PASSES. Teardown via the per-scenario cleanup hook.

  A second scenario extends the guard past handshake auth to the subscribe-frame scope path. It wraps an ordinary
  subscription assertion in a SUSTAINED flood (background threads that keep every event-loop thread poisoned for the
  whole subscription arc): the subscription must still DELIVER DATA — data delivery requires the subscribe frame's
  scope check (authorizeGraphQLSubscriptionEvents) to resolve the correct tenant, so it fails if that path loses its
  tenant flow (exactly the second leak this suite surfaced). Together the two scenarios cover every WS frame path
  whose tenant flow is load-bearing: handshake auth + frame auth (scenario 1) and subscribe-frame scope resolution
  (scenario 2).

  Scope note (validated by removing each tenant line and rebuilding the gateway, matching the unit-test approach):
  a throttle guard and a negative-scope (RESOURCE_FORBIDDEN) guard were prototyped and DROPPED because neither
  discriminates fixed-from-broken. Removing doThrottle's tenant flow does not break frame throttling — the throttle
  keys already encode the tenant, so getThrottleStatus is a tenant-independent local lookup. And a token LACKING the
  scope is rejected with 4002 whether the scope check succeeds or errors (the null-tenant NPE is itself wrapped as
  4002 "Error while accessing backend services"), so a negative test passes on the broken image too. Query
  complexity/depth is likewise not tenant-scoped (the 4021/4020 QueryAnalyzer is schema-based; its limits come from
  the in-context infoDTO resolved during auth). Scenario 2 is therefore the sole valid guard for the scope path.

  @cap:gateway @feat:streaming-invocation @rule:tenant-isolation @type:regression @dep:publisher
  Scenario: A carbon.super GraphQL subscription authenticates despite a concurrent tenant handshake flood
    Given The system is ready

    # ---- Victim: a carbon.super GraphQL subscription API, published, subscribed, with a token ----
    And I have valid access tokens as "admin"
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubPayload" as "gqlSubApiId"
    And I deploy the API with id "gqlSubApiId"
    When I publish the "apis" resource with id "gqlSubApiId"
    Then The lifecycle status of API "gqlSubApiId" should be "Published"
    And the "apis" resource "gqlSubApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "gqlSubApiId"
    And I extract response field "context" and store it as "gqlSubContext"
    When I have set up application with keys, subscribed to API "gqlSubApiId", and obtained access token for "gqlSubSubId"
    Then The response status code should be 200

    # ---- Leak source: a tenant1.com WS API, deployed and routable so flood handshakes reach the tenant flow ----
    When I act as "admin@tenant1.com"
    And I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "tenantWsApiId" and deployed it
    When I publish the "apis" resource with id "tenantWsApiId"
    Then The lifecycle status of API "tenantWsApiId" should be "Published"
    And the "apis" resource "tenantWsApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "tenantWsApiId"
    And I extract response field "context" and store it as "tenantWsContext"

    # ---- Assert the correct invariant: the super-tenant subscription authenticates despite the flood ----
    When I open a GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token "generatedAccessToken", the subscription authenticates despite 200 concurrent handshakes to tenant ws context "{{tenantWsContext}}/1.0.0"

  @cap:gateway @feat:streaming-invocation @rule:tenant-isolation @type:regression @dep:publisher
  Scenario: A carbon.super GraphQL subscription still DELIVERS DATA despite a sustained tenant handshake flood
    Given The system is ready

    # ---- Victim: a carbon.super GraphQL subscription API, published, subscribed, with a token ----
    And I have valid access tokens as "admin"
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubPayload" as "gqlSubApiId"
    And I deploy the API with id "gqlSubApiId"
    When I publish the "apis" resource with id "gqlSubApiId"
    Then The lifecycle status of API "gqlSubApiId" should be "Published"
    And the "apis" resource "gqlSubApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "gqlSubApiId"
    And I extract response field "context" and store it as "gqlSubContext"
    When I have set up application with keys, subscribed to API "gqlSubApiId", and obtained access token for "gqlSubSubId"
    Then The response status code should be 200

    # ---- Leak source: a tenant1.com WS API, deployed and routable so flood handshakes reach the tenant flow ----
    When I act as "admin@tenant1.com"
    And I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "tenantWsApiId" and deployed it
    When I publish the "apis" resource with id "tenantWsApiId"
    Then The lifecycle status of API "tenantWsApiId" should be "Published"
    And the "apis" resource "tenantWsApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "tenantWsApiId"
    And I extract response field "context" and store it as "tenantWsContext"

    # ---- Under a SUSTAINED flood, the subscription must AUTHENTICATE and DELIVER DATA. Data delivery drives the
    #      subscribe frame's scope check (authorizeGraphQLSubscriptionEvents) on a poisoned thread — so this fails on
    #      the unfixed scope path (null-tenant NPE -> no data) and passes once that path owns its tenant flow. ----
    When I start a sustained flood of 8 concurrent tenant handshakes to tenant ws context "{{tenantWsContext}}/1.0.0" using access token "generatedAccessToken"
    And I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token "generatedAccessToken" expecting data containing "Astra Express" within 120 seconds
    Then I stop the sustained flood
