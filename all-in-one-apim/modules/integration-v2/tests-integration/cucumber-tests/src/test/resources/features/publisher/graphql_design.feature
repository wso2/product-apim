@cleanup
Feature: Publisher GraphQL API Design

  Publisher-plane GraphQL API design: create a GraphQL API from a schema, take it through revision and
  deploy, and publish it. Asserts only publisher-plane outcomes — GraphQL invocation is covered by
  gateway/graphql-invocation. Self-contained scenario, torn down by the per-scenario cleanup hook.

  @cap:publisher @feat:graphql-design @type:smoke @legacy:GraphQLAPITestCase
  Scenario Outline: Create, deploy and publish a GraphQL API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "graphqlApiId"
    Then The response status code should be 201

    When I retrieve the "apis" resource with id "graphqlApiId"
    Then The response status code should be 200
    And The response should contain "GRAPHQL"
    And I put the response payload in context as "graphqlRetrievedPayload"

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

    When I publish the "apis" resource with id "graphqlApiId"
    Then The lifecycle status of API "graphqlApiId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # The publisher schema-definition endpoint must return the uploaded SDL in FULL — asserted by whole-schema
  # equality against the uploaded file (whitespace-normalised), not by a substring. A `contains "languages"` check
  # passes even if every other type in the schema were dropped, which is exactly what legacy
  # GraphqlTestCase.testRetrieveSchemaDefinitionAtPublisher / testUpdateSchemaDefinitionOfAPI asserted properly and
  # the earlier v2 port did not. ×2 tenant.
  @cap:publisher @feat:graphql-design @type:regression @legacy:GraphqlServerRestartTestCase @legacy:GraphqlTestCase
  Scenario Outline: Retrieve and update a GraphQL API's schema definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "gqlDesignApiId"
    Then The response status code should be 201
    # Retrieve the schema — it must equal the whole uploaded definition.
    When I retrieve the GraphQL schema of API "gqlDesignApiId"
    Then The response status code should be 200
    And The GraphQL schema definition in the response should equal the schema file "artifacts/payloads/graphql_schema.graphql"
    # Update the schema, then re-retrieve: the definition must now equal the whole UPDATED file.
    When I update the GraphQL schema of API "gqlDesignApiId" with schema file "artifacts/payloads/updated_graphql_schema.graphql"
    Then The response status code should be 200
    When I retrieve the GraphQL schema of API "gqlDesignApiId"
    Then The response status code should be 200
    And The GraphQL schema definition in the response should equal the schema file "artifacts/payloads/updated_graphql_schema.graphql"
    # Revision + deploy the updated schema.
    When I put the following JSON payload in context as "gqlDesignRevPayload"
    """
    {"description":"updated schema revision"}
    """
    And I make a request to create a revision for "apis" resource "gqlDesignApiId" with payload "gqlDesignRevPayload"
    And I deploy revision "revisionId" of "apis" resource "gqlDesignApiId"
    Then The response status code should be 201

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An interface-type schema validates, imports, and the imported API's schema comes back in full. ×2 tenant.
  # (Legacy createAndPublishGraphQLAPIUsingSchemaWithInterfaces also "published and waited", but its wait step
  # polled for the OTHER API created in its @BeforeClass, so the publish it appeared to assert was never
  # confirmed for this API. Publish/deploy of a GraphQL API is covered by the first scenario in this file, so the
  # bogus step is not reproduced.)
  @cap:publisher @feat:graphql-design @type:regression @legacy:GraphqlServerRestartTestCase @legacy:GraphqlTestCase
  Scenario Outline: Validate and create a GraphQL API from an interface-type schema as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the GraphQL schema file "artifacts/payloads/graphql_schema_with_interface.graphql"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlInterfacePayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema_with_interface.graphql" and additional properties "gqlInterfacePayload" as "gqlInterfaceApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gqlInterfaceApiId"
    Then The response status code should be 200
    And The response should contain "GRAPHQL"
    When I retrieve the GraphQL schema of API "gqlInterfaceApiId"
    Then The response status code should be 200
    And The GraphQL schema definition in the response should equal the schema file "artifacts/payloads/graphql_schema_with_interface.graphql"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The publisher GraphQL-complexity round trip. Legacy testAddGraphQLComplexity and
  # GraphqlSubscriptionTestCase.testGraphQLAPIInvocationForComplexity both begin by GETting the schema TYPE LIST
  # (the per-type/per-field inventory the weights are keyed by) and end by GETting the stored complexity back; the
  # v2 port only ever PUT the weights, so neither read was exercised and nothing proved the config was retained.
  #
  # Weights are deliberately DISTINCT PRIMES (2/3/5/7) rather than all-1 as the legacy fixtures used: with every
  # weight equal, a server that returned the right field names but defaulted, swapped or transposed their values
  # would still satisfy the assertions. Distinct values pin each field to its OWN weight, so any mix-up fails.
  @cap:publisher @feat:graphql-design @rule:complexity @type:regression @legacy:GraphQLQueryAnalysisTest @legacy:GraphqlSubscriptionTestCase
  Scenario Outline: Retrieve the schema type list and round-trip GraphQL complexity weights as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlCxPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlCxPayload" as "gqlCxApiId"
    Then The response status code should be 201

    # The type list must describe the uploaded schema EXACTLY — both object types and, for each, its own fields.
    # (graphql_schema.graphql declares nothing but Language and Query, so the expected set is unambiguous.)
    When I retrieve the publisher GraphQL schema type list of API "gqlCxApiId"
    Then The response status code should be 200
    And The response field "typeList[*].type" should be exactly the list "Language,Query"
    And The response field "typeList[?(@.type=='Language')].fieldList[*]" should be exactly the list "code,name"
    And The response field "typeList[?(@.type=='Query')].fieldList[*]" should be exactly the list "languages,language"

    # Write the weights, then read them back and pin each field to its own value.
    When I put JSON payload from file "artifacts/payloads/graphql_complexity_distinct_weights.json" in context as "gqlCxWeights"
    And I set the GraphQL complexity for API "gqlCxApiId" from payload "gqlCxWeights"
    Then The response status code should be 200
    When I retrieve the publisher GraphQL complexity of API "gqlCxApiId"
    Then The response status code should be 200
    And The response field "list[*].field" should be exactly the list "languages,language,code,name"
    And The response field "list[?(@.type=='Query' && @.field=='languages')].complexityValue" should be exactly the list "2"
    And The response field "list[?(@.type=='Query' && @.field=='language')].complexityValue" should be exactly the list "3"
    And The response field "list[?(@.type=='Language' && @.field=='code')].complexityValue" should be exactly the list "5"
    And The response field "list[?(@.type=='Language' && @.field=='name')].complexityValue" should be exactly the list "7"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Negative on the GraphQL SCHEMA-IMPORT endpoint specifically: a malformed context ({version} placed as a bare
  # path segment) must be refused with 400 before any API exists. Equivalent validation is already pinned for REST
  # (publisher/api_lifecycle) and WebSocket (publisher/streaming_design), but import-graphql-schema is a DIFFERENT
  # entry point — a multipart schema import, not a JSON create — so it needs its own row; nothing covered it.
  # Ports GraphqlTestCase.testCreateAndPublishGraphQLAPIUsingSchemaWithMalformedContext, whose context value
  # ("invalidContext{version}") is reproduced verbatim.
  @cap:publisher @feat:graphql-design @rule:malformed-context @type:negative @legacy:GraphqlTestCase
  Scenario Outline: Importing a GraphQL schema with a malformed context is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlBadCtxPayload"
    And I set the field "context" to "invalidContext{version}" in the payload "gqlBadCtxPayload"
    And I attempt to create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlBadCtxPayload"
    Then The response status code should be 400
    And The response should contain "The API context is malformed"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:graphql-design @type:negative @legacy:GraphQLAPITestCase
  # Drives the GRAPHQL creation path specifically — POST /apis/import-graphql-schema (multipart, type=GRAPHQL),
  # NOT the generic POST /apis. Previously this scenario posted the plain REST payload through the generic
  # endpoint, making it byte-identical to the api-lifecycle copy: it asserted nothing about GraphQL creation
  # while being counted as graphql-design coverage. The schema-import endpoint is a distinct authorisation
  # surface, so a subscriber-role token must be refused there in its own right.
  Scenario Outline: A subscriber-role user cannot create a GraphQL API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "subscriberGqlPayload"
    And I attempt to create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "subscriberGqlPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
