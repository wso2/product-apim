@cleanup
Feature: Gateway GraphQL Subscription Query Limits

  Gateway-plane GraphQL query-analysis limits on a subscription over graphql-ws (complexity 4021, depth 4020) —
  a DIFFERENT mechanism from rate throttling: the gateway computes the query's complexity/depth score and rejects
  it if it exceeds the limit configured on the subscription policy (graphQLMaxComplexity / graphQLMaxDepth, plus
  per-field complexity values). Ports the complexity/depth cases of GraphqlSubscriptionTestCase. Each scenario
  asserts the exact 4021/4020 error code. Single-tenant (super) — the limits are API/policy-scoped.

  @cap:gateway @feat:graphql-invocation @rule:subscription-query-limits @type:regression @dep:admin @legacy:GraphqlSubscriptionTestCase
  Scenario: Complexity limiting rejects an over-complex subscription (4021)
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a subscription throttling policy "${UNIQUE:gqlComplex3}" with max complexity 3 and max depth 8
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubPayload" as "gqlSubApiId"
    When I retrieve the "apis" resource with id "gqlSubApiId"
    And I put the response payload in context as "gqlSubApiPayload"
    And I extract response field "context" and store it as "gqlSubContext"
    When I update the "apis" resource "gqlSubApiId" and "gqlSubApiPayload" with configuration type "policies" and value:
    """
    ["AsyncUnlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/graphql_complexity.json" in context as "gqlComplexityPayload"
    And I set the GraphQL complexity for API "gqlSubApiId" from payload "gqlComplexityPayload"
    Then The response status code should be 200
    And I deploy the API with id "gqlSubApiId"
    When I publish the "apis" resource with id "gqlSubApiId"
    Then The lifecycle status of API "gqlSubApiId" should be "Published"
    When I have set up application with keys, subscribed to API "gqlSubApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlComplexSubId"
    Then The response status code should be 200
    # 6 fields at complexity 1 each exceeds max complexity 3 → QUERY TOO COMPLEX (4021)
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext}}/1.0.0" with query "subscription { liftStatusChange { name id status night capacity } }" using access token "generatedAccessToken" expecting error code 4021 within 90 seconds

  @cap:gateway @feat:graphql-invocation @rule:subscription-query-limits @type:regression @dep:admin @legacy:GraphqlSubscriptionTestCase
  Scenario: Depth limiting rejects an over-deep subscription (4020)
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a subscription throttling policy "${UNIQUE:gqlDepth1}" with max complexity 100 and max depth 1
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_subscription_api.json" in context as "gqlSubPayload2"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_subscription_schema.graphql" and additional properties "gqlSubPayload2" as "gqlSubApiId2"
    When I retrieve the "apis" resource with id "gqlSubApiId2"
    And I put the response payload in context as "gqlSubApiPayload2"
    And I extract response field "context" and store it as "gqlSubContext2"
    When I update the "apis" resource "gqlSubApiId2" and "gqlSubApiPayload2" with configuration type "policies" and value:
    """
    ["AsyncUnlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    And I deploy the API with id "gqlSubApiId2"
    When I publish the "apis" resource with id "gqlSubApiId2"
    Then The lifecycle status of API "gqlSubApiId2" should be "Published"
    When I have set up application with keys, subscribed to API "gqlSubApiId2" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlDepthSubId"
    Then The response status code should be 200
    # liftStatusChange { name } is depth 2, exceeds max depth 1 → QUERY TOO DEEP (4020)
    When I invoke the GraphQL subscription at gateway ws context "{{gqlSubContext2}}/1.0.0" with query "subscription { liftStatusChange { name } }" using access token "generatedAccessToken" expecting error code 4020 within 90 seconds
