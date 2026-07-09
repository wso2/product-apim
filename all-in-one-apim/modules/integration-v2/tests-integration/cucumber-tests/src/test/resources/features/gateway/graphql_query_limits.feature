@cleanup
Feature: Gateway GraphQL Query Limits (HTTP)

  Gateway-plane GraphQL query-analysis limits on an HTTP query (not a subscription): the gateway computes the
  query's complexity/depth and rejects an over-limit query BEFORE it reaches the backend. This is the documented
  HTTP variant (enforce-graphql-query-limits) of the complexity/depth limits — the subscription variant returns a
  graphql-ws 4021/4020 frame, whereas over HTTP the gateway returns a 400 with a "QUERY TOO COMPLEX / TOO DEEP"
  error. Single-tenant (super) — the limits are API/policy-scoped.

  @cap:gateway @feat:graphql-invocation @rule:query-limits @type:regression @dep:admin @legacy:GraphqlTestCase
  Scenario: An over-complex HTTP GraphQL query is rejected
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a subscription throttling policy "${UNIQUE:gqlQComplex1}" with max complexity 1 and max depth 8
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlQPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlQPayload" as "gqlQApiId"
    When I retrieve the "apis" resource with id "gqlQApiId"
    And I put the response payload in context as "gqlQApiPayload"
    And I extract response field "context" and store it as "gqlQContext"
    When I update the "apis" resource "gqlQApiId" and "gqlQApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/graphql_query_complexity.json" in context as "gqlQComplexityPayload"
    And I set the GraphQL complexity for API "gqlQApiId" from payload "gqlQComplexityPayload"
    Then The response status code should be 200
    And I deploy the API with id "gqlQApiId"
    When I publish the "apis" resource with id "gqlQApiId"
    Then The lifecycle status of API "gqlQApiId" should be "Published"
    When I have set up application with keys, subscribed to API "gqlQApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlQComplexSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlComplexQuery"
    """
    {"query": "{ languages { code name } }"}
    """
    # complexity 3 (languages + code + name) exceeds max complexity 1 → rejected (400, QUERY TOO COMPLEX)
    And I invoke the API at gateway context "{{gqlQContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlComplexQuery" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    And The response should contain "COMPLEX"

  @cap:gateway @feat:graphql-invocation @rule:query-limits @type:regression @dep:admin @legacy:GraphqlTestCase
  Scenario: An over-deep HTTP GraphQL query is rejected
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a subscription throttling policy "${UNIQUE:gqlQDepth1}" with max complexity 100 and max depth 1
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlQPayload2"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlQPayload2" as "gqlQApiId2"
    When I retrieve the "apis" resource with id "gqlQApiId2"
    And I put the response payload in context as "gqlQApiPayload2"
    And I extract response field "context" and store it as "gqlQContext2"
    When I update the "apis" resource "gqlQApiId2" and "gqlQApiPayload2" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    And I deploy the API with id "gqlQApiId2"
    When I publish the "apis" resource with id "gqlQApiId2"
    Then The lifecycle status of API "gqlQApiId2" should be "Published"
    When I have set up application with keys, subscribed to API "gqlQApiId2" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlQDepthSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlDeepQuery"
    """
    {"query": "{ languages { code name } }"}
    """
    # depth 2 (languages -> code/name) exceeds max depth 1 → rejected (400, QUERY TOO DEEP)
    And I invoke the API at gateway context "{{gqlQContext2}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlDeepQuery" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    And The response should contain "DEEP"
