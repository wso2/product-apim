@cleanup
Feature: Gateway GraphQL Query Limits (HTTP)

  Gateway-plane GraphQL query-analysis limits on an HTTP query (not a subscription): the gateway computes the
  query's complexity/depth and rejects an over-limit query BEFORE it reaches the backend. This is the documented
  HTTP variant (enforce-graphql-query-limits) of the complexity/depth limits — the subscription variant returns a
  graphql-ws 4021/4020 frame, whereas over HTTP the gateway returns a 400 with a "QUERY TOO COMPLEX / TOO DEEP"
  error. The limits are API/policy-scoped and are exercised in both organizations.

  @cap:gateway @feat:graphql-invocation @rule:query-limits @type:regression @dep:admin @legacy:GraphqlTestCase
  Scenario Outline: An over-complex HTTP GraphQL query is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "gqlQApiId" should be live on the gateway, redeploying if propagation is lost
    When I have set up application with keys, subscribed to API "gqlQApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlQComplexSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlComplexQuery"
    """
    {"query": "{ languages { code name } }"}
    """
    # complexity 3 (languages + code + name) exceeds max complexity 1 → rejected (400, QUERY TOO COMPLEX)
    And I invoke the API at gateway context "{{gqlQContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlComplexQuery" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    # Pin the WHOLE message and the code, not the substring "COMPLEX": the gateway routes every query-analysis
    # rejection through one fault handler, so "COMPLEX" alone cannot distinguish this from a DEEP rejection, and a
    # renumbered code would go unnoticed. 900821 / "QUERY TOO COMPLEX" are the
    # GraphQLConstants.GRAPHQL_QUERY_TOO_COMPLEX pair.
    And The response should contain "QUERY TOO COMPLEX"
    And The response should contain "900821"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:graphql-invocation @rule:query-limits @type:regression @dep:admin @legacy:GraphqlTestCase
  Scenario Outline: An over-deep HTTP GraphQL query is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "gqlQApiId2" should be live on the gateway, redeploying if propagation is lost
    When I have set up application with keys, subscribed to API "gqlQApiId2" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlQDepthSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlDeepQuery"
    """
    {"query": "{ languages { code name } }"}
    """
    # depth 2 (languages -> code/name) exceeds max depth 1 → rejected (400, QUERY TOO DEEP)
    And I invoke the API at gateway context "{{gqlQContext2}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlDeepQuery" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    # Whole message + code, for the reason given on the complexity scenario. 900820 / "QUERY TOO DEEP" are the
    # GraphQLConstants.GRAPHQL_QUERY_TOO_DEEP pair.
    And The response should contain "QUERY TOO DEEP"
    And The response should contain "900820"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # ── product-apim#11773: a query's complexity is MULTIPLIED by its slicing argument ──────────────────────────
  #
  # The gateway's FieldComplexityCalculatorImpl computes a field's cost as
  #     argumentsValue × (customComplexity + childComplexity)
  # where argumentsValue sums the INT values of the arguments named in
  # GraphQLConstants.QUERY_COMPLEXITY_SLICING_ARGS = [first, last, limit] and counts 1 for every other argument
  # (and is 1 when the field takes no arguments at all). So `limit:` is not decoration — it is a MULTIPLIER, and a
  # calculation that ignored it would under-count an expensive paginated query by three orders of magnitude and
  # wave it straight through. That under-counting was product-apim#11773.
  #
  # A single "big limit is rejected" row cannot catch the regression: a 400 could equally come from the field's own
  # weight, the depth rule, or a mis-set policy. The rows below are therefore a DISCRIMINATING SET against
  # max complexity 4 / max depth 2, all on the SAME field so only the argument differs:
  #   • no limit argument     `limitLanguage(where:{eq:100})`         → 1 × (1+0) =    1  ≤ 4  → served (200)
  #   • limit exactly at cap  `limitLanguage(limit:4)`                → 4 × (1+0) =    4  ≤ 4  → served (200)
  #   • limit one over cap    `limitLanguage(limit:5)`                → 5 × (1+0) =    5  > 4  → rejected (400)
  #   • the #11773 query      `limitLanguage(limit:1000,where:{eq:100})` → 1001 × (1+0) = 1001 > 4 → rejected (400)
  # The first two rows are the ones that give the last two their meaning: if the limit argument were ignored every
  # row would compute 1 and the two 400-expecting rows would fail. The 4-vs-5 pair additionally pins the
  # ARITHMETIC (a multiplier, not merely "large limits are suspicious") — an off-by-one or a clamp would break it.
  # Run for BOTH token types: JWT is validated locally by the gateway while OAUTH/opaque is validated by
  # key-manager introspection, and query analysis runs after authentication on each of those distinct paths.
  @cap:gateway @feat:graphql-invocation @rule:query-limits @type:regression @dep:admin @legacy:GraphQLQueryAnalysisTest
  Scenario Outline: A limit argument multiplies query complexity with a <tokenType> token (product-apim#11773)
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a subscription throttling policy "${UNIQUE:gqlLimitMul}" with max complexity 4 and max depth 2
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_query_analysis_api.json" in context as "gqlLimPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_query_analysis_schema.graphql" and additional properties "gqlLimPayload" as "gqlLimApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gqlLimApiId"
    And I put the response payload in context as "gqlLimApiPayload"
    And I extract response field "context" and store it as "gqlLimContext"
    When I update the "apis" resource "gqlLimApiId" and "gqlLimApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    # Every field weighted 1, so the ONLY thing that can vary a query's cost here is the argument multiplier.
    When I put JSON payload from file "artifacts/payloads/graphql_query_analysis_complexity.json" in context as "gqlLimComplexityPayload"
    And I set the GraphQL complexity for API "gqlLimApiId" from payload "gqlLimComplexityPayload"
    Then The response status code should be 200
    And I deploy the API with id "gqlLimApiId"
    When I publish the "apis" resource with id "gqlLimApiId"
    Then The lifecycle status of API "gqlLimApiId" should be "Published"
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "gqlLimApiId" should be live on the gateway, redeploying if propagation is lost
    When I have set up a "<tokenType>" token type application with keys, subscribed to API "gqlLimApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "gqlLimSubId"
    Then The response status code should be 200

    # Control 1 — the same field with a NON-slicing argument only: cost 1, served. Placed first so gateway warm-up
    # is absorbed by a 200-expecting call rather than masquerading as a rejection below.
    When I put the following JSON payload in context as "gqlLimNoLimitQuery"
    """
    {"query": "{limitLanguage(where:{eq:100})}"}
    """
    And I invoke the API at gateway context "{{gqlLimContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlLimNoLimitQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "QUERY TOO COMPLEX"

    # Control 2 — limit exactly AT the cap: cost 4, still served. This is the row that pins the multiplier's
    # arithmetic together with the next one.
    When I put the following JSON payload in context as "gqlLimAtCapQuery"
    """
    {"query": "{limitLanguage(limit:4)}"}
    """
    And I invoke the API at gateway context "{{gqlLimContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlLimAtCapQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "QUERY TOO COMPLEX"

    # One over the cap: cost 5, rejected. Differs from Control 2 by a single digit in the limit argument.
    When I put the following JSON payload in context as "gqlLimOverCapQuery"
    """
    {"query": "{limitLanguage(limit:5)}"}
    """
    And I invoke the API at gateway context "{{gqlLimContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlLimOverCapQuery" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    And The response should contain "QUERY TOO COMPLEX"
    And The response should contain "900821"

    # The verbatim #11773 query: cost 1001, rejected.
    When I put the following JSON payload in context as "gqlLimRegressionQuery"
    """
    {"query": "{limitLanguage(limit:1000, where:{eq:100})}"}
    """
    And I invoke the API at gateway context "{{gqlLimContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlLimRegressionQuery" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    And The response should contain "QUERY TOO COMPLEX"
    And The response should contain "900821"

    Examples:
      | tokenType |
      | JWT       |
      | OAUTH     |
