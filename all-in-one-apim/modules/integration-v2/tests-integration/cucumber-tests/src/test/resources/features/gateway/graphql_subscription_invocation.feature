@cleanup
Feature: Gateway GraphQL Subscription Invocation

  Gateway-plane runtime invocation of a published GraphQL API's SUBSCRIPTION operation over WebSocket. A GraphQL
  API is created from a schema carrying a Subscription type (liftStatusChange), published, and subscribed to;
  the test then opens a WebSocket through the gateway WS inbound with the graphql-ws subprotocol, performs the
  connection_init/connection_ack handshake, sends a subscription start, and asserts the backend emits the
  subscription data. This is the subscription counterpart of the GraphQL query invocation feature (which goes
  over HTTP). The backend is the graphql-ws handler on the node GraphQL sample. Runs in both the super tenant and
  tenant1.com as the tenant admin. Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:graphql-invocation @rule:subscription @type:regression @dep:publisher @legacy:GraphqlSubscriptionTestCase
  Scenario Outline: Invoke a published GraphQL subscription through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubPayload" as "gqlSubApiId"
    And I deploy the API with id "gqlSubApiId"
    When I publish the "apis" resource with id "gqlSubApiId"
    Then The lifecycle status of API "gqlSubApiId" should be "Published"
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "gqlSubApiId" should be live on the gateway, redeploying if propagation is lost

    # Capture the API's full gateway context (already carries /t/<tenant> for tenant APIs)
    When I retrieve the "apis" resource with id "gqlSubApiId"
    And I extract response field "context" and store it as "gqlSubContext"

    # Subscribe an application and obtain an access token
    When I have set up application with keys, subscribed to API "gqlSubApiId", and obtained access token for "gqlSubSubId"
    Then The response status code should be 200

    # Open a graphql-ws subscription through the gateway and assert the subscription data arrives
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token "generatedAccessToken" expecting data containing "Astra Express" within 120 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
