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

  @cap:publisher @feat:graphql-design @type:regression @legacy:GraphqlServerRestartTestCase
  Scenario: Retrieve and update a GraphQL API's schema definition
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "gqlDesignApiId"
    Then The response status code should be 201
    # Retrieve the schema.
    When I retrieve the GraphQL schema of API "gqlDesignApiId"
    Then The response status code should be 200
    And The response should contain "languages"
    # Update the schema, then re-retrieve to confirm the change took effect.
    When I update the GraphQL schema of API "gqlDesignApiId" with schema file "artifacts/payloads/updated_graphql_schema.graphql"
    Then The response status code should be 200
    When I retrieve the GraphQL schema of API "gqlDesignApiId"
    Then The response status code should be 200
    And The response should contain "languageCount"
    # Revision + deploy the updated schema.
    When I put the following JSON payload in context as "gqlDesignRevPayload"
    """
    {"description":"updated schema revision"}
    """
    And I make a request to create a revision for "apis" resource "gqlDesignApiId" with payload "gqlDesignRevPayload"
    And I deploy revision "revisionId" of "apis" resource "gqlDesignApiId"
    Then The response status code should be 201

  @cap:publisher @feat:graphql-design @type:regression @legacy:GraphqlServerRestartTestCase
  Scenario: Validate and create a GraphQL API from an interface-type schema
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I validate the GraphQL schema file "artifacts/payloads/graphql_schema_with_interface.graphql"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlInterfacePayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema_with_interface.graphql" and additional properties "gqlInterfacePayload" as "gqlInterfaceApiId"
    Then The response status code should be 201

  @cap:publisher @feat:graphql-design @type:negative @legacy:GraphQLAPITestCase
  Scenario Outline: A subscriber-role user cannot create an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
