Feature: API Documents
  This feature tests API Documents functionality including adding new documents,
  editing them and deleting them.

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Create base APIs
  Scenario Outline: Create an API
    Given I have created an api from "<payloadFile>" as "<apiID>" and deployed it
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"

    Examples:
      |payloadFile                                            | apiID        | apiUpdatePayload|
      |artifacts/payloads/create_apim_test_api.json           |  RestAPIId   | RestAPIPayload  |
      |artifacts/payloads/create_apim_test_soap_api.json      |  SoapAPIId   | SoapAPIPayload  |
      |artifacts/payloads/create_apim_test_websocket_api.json | AsyncAPIId   | AsyncAPIPayload |

  Scenario: Create GraphQL API
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "GraphQLAPIId"
    And I retrieve the "apis" resource with id "<GraphQLAPIId>"
    And I put the response payload in context as "graphQLAPIPayload"

  Scenario Outline: Add a new Document for API
    # Step 2: Add new documents to the API- "documentID" stored in context
    When I prepare a new document payload with type "<type>", sourceType "<sourceType>", and inlineContent "<inlineContent>"
    And I add the document to API "<apiID>"
    Then The response status code should be 201

    # Step 3: Retrieve all available document for API
    When I retrieve all available documents for "<apiID>"
    Then The response status code should be 200

    # Step 4: Retrieve specific document for API
    When I retrieve document with "<documentID>" for "<apiID>"
    Then The response status code should be 200

    # Step 5: Update an existing document
    When I prepare a new document payload with type "SAMPLES", sourceType "INLINE", and inlineContent "updated content"
    And I update the document with "<documentID>" for API "<apiID>"
    Then The response status code should be 200
    And The response should contain "SAMPLES"

    # Step 6: Delete an existing document
    When I delete the document with "<documentID>" for "<apiID>"
    Then The response status code should be 200

    # Step 7:Delete the base API
    When I delete the "apis" resource with id "<apiID>"
    Then The response status code should be 200

    Examples:
     |  apiID            | type    | sourceType | inlineContent                      |
     |  RestAPIId        | HOWTO   | INLINE     | Test content for inline document   |
     |  SoapAPIId        | HOWTO   | INLINE     | Test content for inline document   |
     |  GraphQLAPIId     | HOWTO   | INLINE     | Test content for inline document   |
     |  AsyncAPIId       | HOWTO   | INLINE     | Test content for inline document   |
