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

    # Publishing must also make the subscription API DISCOVERABLE on both planes — the create/publish statuses
    # above do not show that. Legacy publishGraphQLAPIWithSubscriptions published the API and asserted neither
    # listing, so a publish that succeeded while leaving the API invisible to a consumer would have gone unnoticed.
    Then The "publisher" listing should report API "gqlSubApiId" exactly once
    And The "devportal" listing should report API "gqlSubApiId" exactly once

    # Open a graphql-ws subscription through the gateway and assert the subscription data arrives
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token "generatedAccessToken" expecting data containing "Astra Express" within 120 seconds

    # Same subscription, credential presented as ?access_token= on the WS URL with NO Authorization header — the
    # AUTH_IN.QUERY half of legacy testGraphQLAPIInvocationWithJWTToken (which invoked BOTH ways; only the header
    # way was ported). A distinct gateway code path: the WS inbound must take the credential off the handshake's
    # query string, before any header is consulted.
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token query param "generatedAccessToken" expecting data containing "Astra Express" within 120 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: a subscription query the SCHEMA does not admit (an undefined field on Lift) is refused by the
  # gateway at the START frame with a graphql-ws error frame carrying code 4022 and reason "INVALID QUERY"
  # (FrameErrorConstants.GRAPHQL_INVALID_QUERY / GRAPHQL_INVALID_QUERY_MESSAGE; the gateway emits the message as
  # "INVALID QUERY : <detail>"). Ports GraphqlSubscriptionTestCase.testGraphQLAPIInvocationWithInvalidPayload,
  # which had NO v2 coverage. Code AND reason are matched, never frame presence: every frame-level rejection on
  # this channel has the identical {"type":"error","payload":[{"message":…,"code":…}]} shape, so a presence check
  # would pass on a throttling or scope rejection just as happily.
  @cap:gateway @feat:graphql-invocation @rule:subscription @type:negative @dep:publisher @legacy:GraphqlSubscriptionTestCase
  Scenario Outline: An invalid subscription query is refused with a 4022 INVALID QUERY frame as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlBadPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlBadPayload" as "gqlBadApiId"
    And I deploy the API with id "gqlBadApiId"
    When I publish the "apis" resource with id "gqlBadApiId"
    Then The lifecycle status of API "gqlBadApiId" should be "Published"
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "gqlBadApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "gqlBadApiId"
    And I extract response field "context" and store it as "gqlBadContext"
    When I have set up application with keys, subscribed to API "gqlBadApiId", and obtained access token for "gqlBadSubId"
    Then The response status code should be 200
    # `invalidField` is not a field of Lift, so the query fails schema validation at the gateway.
    When I invoke the GraphQL subscription at gateway ws context "{{gqlBadContext}}/1.0.0" with query "subscription { liftStatusChange { name invalidField } }" using access token "generatedAccessToken" expecting error code 4022 and reason "INVALID QUERY" within 90 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
