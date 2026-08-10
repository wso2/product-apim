@cleanup
Feature: Publisher GraphQL API Creation from a URL

  Publisher-plane GraphQL API creation from a URL instead of an uploaded schema file — ports the "create using
  endpoint" (introspection) and SDL-URL paths of GraphqlTestCase. The gateway derives the schema from the URL:
  by introspecting a live GraphQL endpoint, or by fetching an SDL served at a URL. Uses the in-network node
  GraphQL backend (no third-party). Both paths run in BOTH tenants, as the legacy Factory did.

  # The derived schema is asserted through the publisher's SCHEMA TYPE LIST, not with a substring match on the raw
  # SDL. `The response should contain "languages"` — what this file asserted before — passes even if every other
  # type and field were dropped, and it cannot be replaced by whole-file equality either: a schema RECONSTRUCTED
  # by introspection is semantically equal to the original but textually different (field order, indentation and
  # directive placement are the server's, not the file's). The type list is the format-independent form of the
  # same assertion: it pins every object type and, per type, that type's exact field set.
  @cap:publisher @feat:graphql-design @rule:create-from-url-introspection @type:regression @dep:gateway @legacy:GraphqlTestCase
  Scenario Outline: Create a GraphQL API by introspecting a live endpoint as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Introspect the live node GraphQL endpoint to derive the SDL, then create the API from that schema
    When I validate the GraphQL schema from endpoint URL "http://nodebackend:3003/graphql-full" with introspection "true" and store schema as "introspectedSchema"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlUrlPayload"
    And I create a GraphQL API with schema "introspectedSchema" and additional properties "gqlUrlPayload" as "gqlUrlApiId"
    Then The response status code should be 201
    # The schema was derived from the endpoint via introspection — every type and field must have come across.
    When I retrieve the publisher GraphQL schema type list of API "gqlUrlApiId"
    Then The response status code should be 200
    And The response field "typeList[*].type" should be exactly the list "Language,Query"
    And The response field "typeList[?(@.type=='Language')].fieldList[*]" should be exactly the list "code,name"
    And The response field "typeList[?(@.type=='Query')].fieldList[*]" should be exactly the list "languages,language"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:graphql-design @rule:create-from-url-sdl @type:regression @dep:gateway @legacy:GraphqlTestCase
  Scenario Outline: Create a GraphQL API by fetching an SDL served at a URL as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # The node backend serves the raw SDL at /sdl; APIM fetches it and creates the API (useIntrospection=false path)
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlSdlPayload"
    And I create a GraphQL API from endpoint URL "http://nodebackend:3003/sdl" with additional properties "gqlSdlPayload" as "gqlSdlApiId"
    Then The response status code should be 201
    When I retrieve the publisher GraphQL schema type list of API "gqlSdlApiId"
    Then The response status code should be 200
    And The response field "typeList[*].type" should be exactly the list "Language,Query"
    And The response field "typeList[?(@.type=='Language')].fieldList[*]" should be exactly the list "code,name"
    And The response field "typeList[?(@.type=='Query')].fieldList[*]" should be exactly the list "languages,language"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
