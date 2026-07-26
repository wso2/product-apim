@cleanup
Feature: Publisher GraphQL API Creation from a URL

  Publisher-plane GraphQL API creation from a URL instead of an uploaded schema file — ports the "create using
  endpoint" (introspection) and SDL-URL paths of GraphqlTestCase. The gateway derives the schema from the URL:
  by introspecting a live GraphQL endpoint, or by fetching an SDL served at a URL. Uses the in-network node
  GraphQL backend (no third-party). Asserts the derived schema contains the expected types.

  @cap:publisher @feat:graphql-design @rule:create-from-url-introspection @type:regression @dep:gateway @legacy:GraphqlTestCase
  Scenario: Create a GraphQL API by introspecting a live endpoint
    Given The system is ready
    And I have valid access tokens as "admin"
    # Introspect the live node GraphQL endpoint to derive the SDL, then create the API from that schema
    When I validate the GraphQL schema from endpoint URL "http://nodebackend:3003/graphql-full" with introspection "true" and store schema as "introspectedSchema"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlUrlPayload"
    And I create a GraphQL API with schema "introspectedSchema" and additional properties "gqlUrlPayload" as "gqlUrlApiId"
    Then The response status code should be 201
    # The schema was derived from the endpoint via introspection — it must contain the schema's types
    When I retrieve the GraphQL schema of API "gqlUrlApiId"
    Then The response status code should be 200
    And The response should contain "languages"

  @cap:publisher @feat:graphql-design @rule:create-from-url-sdl @type:regression @dep:gateway @legacy:GraphqlTestCase
  Scenario: Create a GraphQL API by fetching an SDL served at a URL
    Given The system is ready
    And I have valid access tokens as "admin"
    # The node backend serves the raw SDL at /sdl; APIM fetches it and creates the API (useIntrospection=false path)
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlSdlPayload"
    And I create a GraphQL API from endpoint URL "http://nodebackend:3003/sdl" with additional properties "gqlSdlPayload" as "gqlSdlApiId"
    Then The response status code should be 201
    When I retrieve the GraphQL schema of API "gqlSdlApiId"
    Then The response status code should be 200
    And The response should contain "languages"
