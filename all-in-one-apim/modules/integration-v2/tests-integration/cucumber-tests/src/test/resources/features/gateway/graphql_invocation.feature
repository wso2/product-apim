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

    Examples:
      | tokenType |
      | JWT       |
      | OAUTH     |
