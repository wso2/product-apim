@cleanup
Feature: Gateway GraphQL API Invocation

  Gateway-plane runtime invocation of a published GraphQL API: subscribe an application, obtain an access
  token, and invoke a GraphQL query through the gateway expecting a 200. The backend is the in-network
  am-graphQL-sample (nodebackend:3003). Runs in both the super tenant and tenant1.com as the tenant admin.
  Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:graphql-invocation @type:smoke @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published GraphQL API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "graphqlApiId"
    Then The response status code should be 201

    When I retrieve the "apis" resource with id "graphqlApiId"
    And I put the response payload in context as "graphqlRetrievedPayload"
    And I extract response field "context" and store it as "graphqlApiContext"

    When I put the following JSON payload in context as "createRevisionPayload"
    """
    {
      "description":"Initial Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "graphqlApiId" with payload "createRevisionPayload"
    And I deploy revision "revisionId" of "apis" resource "graphqlApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "graphqlRetrievedPayload"
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "graphqlApiId" should be live on the gateway, redeploying if propagation is lost
    And I publish the "apis" resource with id "graphqlApiId"
    Then The lifecycle status of API "graphqlApiId" should be "Published"

    When I have set up application with keys, subscribed to API "graphqlApiId", and obtained access token for "graphqlSubscriptionId"
    Then The response status code should be 200

    When I put the following JSON payload in context as "graphqlQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{graphqlApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "graphqlQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # A 200 alone proves almost NOTHING for GraphQL: the protocol answers 200 for a FAILED query too (an error is
    # a {"errors":[...]} body at status 200), so a status-only assertion cannot tell a served query from a refused
    # one. The relayed body is therefore pinned EXACTLY — every language, both fields, no extra members — which is
    # the assertion legacy GraphqlTestCase made (assertEquals against its RESPONSE_DATA constant) and the earlier
    # v2 port dropped. An error envelope, a truncated list or an empty body all fail this.
    And The response body should equal the JSON file "artifacts/payloads/graphql_languages_expected_response.json"
    And The response should not contain "errors"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Token-type parity: invoke the GraphQL query with BOTH a JWT application token (the product default, validated
  # locally by the gateway) and an OAUTH/opaque application token (a reference token validated via key-manager
  # introspection — a DISTINCT gateway validation path). Ports the JWT-app + oauth-app invocation of GraphqlTestCase.
  # Single-tenant (super): token-type validation is server-wide; the ×2-tenant smoke above already covers routing.
  @cap:gateway @feat:graphql-invocation @rule:token-type @type:regression @dep:publisher @legacy:GraphqlTestCase
  Scenario Outline: Invoke a GraphQL API with a <tokenType> application token
    Given The system is ready
    And I have valid access tokens as "admin"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "graphqlApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "graphqlApiId"
    And I put the response payload in context as "graphqlRetrievedPayload"
    And I extract response field "context" and store it as "graphqlApiContext"
    When I put the following JSON payload in context as "createRevisionPayload"
    """
    {"description":"Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "graphqlApiId" with payload "createRevisionPayload"
    And I deploy revision "revisionId" of "apis" resource "graphqlApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "graphqlRetrievedPayload"
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "graphqlApiId" should be live on the gateway, redeploying if propagation is lost
    And I publish the "apis" resource with id "graphqlApiId"
    Then The lifecycle status of API "graphqlApiId" should be "Published"
    When I have set up a "<tokenType>" token type application with keys, subscribed to API "graphqlApiId" with plan "Unlimited", and obtained access token for "graphqlTokenTypeSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "graphqlQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{graphqlApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "graphqlQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Body pinned exactly for BOTH token types — see the rationale on the smoke scenario above. This is what makes
    # the token-type parity meaningful: a JWT-validated and an introspection-validated request must relay the SAME
    # backend payload, not merely both answer 200.
    And The response body should equal the JSON file "artifacts/payloads/graphql_languages_expected_response.json"
    And The response should not contain "errors"

    # A query the SCHEMA does not admit is refused by the GATEWAY, before the backend is reached: the
    # GraphQLAPIHandler validates the payload against the API's schema (QueryValidator.validatePayload) and
    # answers code 900422 / "INVALID QUERY". This is the HTTP counterpart of the graphql-ws 4022 frame, and it is
    # why the "GraphQL hides errors behind 200" trap does NOT apply to this class of failure over APIM: the gateway
    # converts it into a real HTTP error status. Observed status is 422, NOT 400 — the gateway derives the HTTP
    # status from the last three digits of the error code here (900422 -> 422), whereas the query-limit codes
    # 900821/900820 both surface as 400 (see graphql_query_limits.feature). Asserted per token type because the two
    # validation paths reach this handler differently.
    When I put the following JSON payload in context as "graphqlUndefinedFieldQuery"
    """
    {"query": "{languages{code name bogusUndefinedField}}"}
    """
    And I invoke the API at gateway context "{{graphqlApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "graphqlUndefinedFieldQuery" until response status code becomes 422 within 60 seconds
    Then The response status code should be 422
    And The response should contain "INVALID QUERY"
    And The error response should have code "900422" and message "INVALID QUERY"

    Examples:
      | tokenType |
      | JWT       |
      | OAUTH     |
