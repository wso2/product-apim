@cleanup
Feature: DevPortal GraphQL Complexity Discovery

  DevPortal-plane, CONSUMER-side reads of a published GraphQL API's query-analysis metadata: the per-field
  complexity weights and the schema type list. This is a different plane from the publisher's own read of the same
  two resources (publisher/graphql-design covers that): the devportal serves them read-only to a subscriber so a
  consumer can work out what a query will cost before sending it. Ports
  GraphQLQueryAnalysisTest.testRetrieveGraphQLComplexity, for which no devportal-plane GraphQL complexity or
  schema-type-list retrieval existed anywhere in the v2 suite. Teardown via the per-scenario cleanup hook.

  # The weights are DISTINCT PRIMES (2/3/5/7), not all-1 as the legacy fixture used, so each field is pinned to its
  # OWN value — with equal weights a server that defaulted or transposed them would still pass. The legacy test
  # asserted nothing but two 200 status codes, which cannot tell a populated response from an empty one.
  @cap:devportal @feat:discovery @rule:graphql-complexity @type:regression @dep:publisher @legacy:GraphQLQueryAnalysisTest
  Scenario Outline: A consumer retrieves a published GraphQL API's complexity and schema type list as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlDpPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlDpPayload" as "gqlDpApiId"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/graphql_complexity_distinct_weights.json" in context as "gqlDpWeights"
    And I set the GraphQL complexity for API "gqlDpApiId" from payload "gqlDpWeights"
    Then The response status code should be 200
    And I deploy the API with id "gqlDpApiId"
    When I publish the "apis" resource with id "gqlDpApiId"
    Then The lifecycle status of API "gqlDpApiId" should be "Published"

    # DevPortal visibility of a just-published API is eventually consistent, so this both GATES the reads below on
    # the API being indexed and asserts the listing membership itself.
    Then The "devportal" listing should report API "gqlDpApiId" exactly once

    # The consumer-side complexity read must carry the weights the publisher stored — each field its own value.
    When I retrieve the devportal GraphQL complexity of API "gqlDpApiId"
    Then The response status code should be 200
    And The response array field "list" should have exactly 4 entries
    And The response field "list[*].field" should be exactly the list "languages,language,code,name"
    And The response field "list[?(@.type=='Query' && @.field=='languages')].complexityValue" should be exactly the list "2"
    And The response field "list[?(@.type=='Query' && @.field=='language')].complexityValue" should be exactly the list "3"
    And The response field "list[?(@.type=='Language' && @.field=='code')].complexityValue" should be exactly the list "5"
    And The response field "list[?(@.type=='Language' && @.field=='name')].complexityValue" should be exactly the list "7"

    # The consumer-side type list must describe the uploaded schema exactly (Language and Query and no others).
    When I retrieve the devportal GraphQL schema type list of API "gqlDpApiId"
    Then The response status code should be 200
    And The response array field "typeList" should have exactly 2 entries
    And The response field "typeList[*].type" should be exactly the list "Language,Query"
    And The response field "typeList[?(@.type=='Language')].fieldList[*]" should be exactly the list "code,name"
    And The response field "typeList[?(@.type=='Query')].fieldList[*]" should be exactly the list "languages,language"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
