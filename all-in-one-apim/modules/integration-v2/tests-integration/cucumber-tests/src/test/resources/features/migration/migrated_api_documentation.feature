Feature: Migrated API Documentation Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Migrated API Documentation Management
    # Step 1: Find the api
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"

    # Step 2: Add a new document with ID as "documentID"
    When I prepare a new document payload with type "HOWTO", sourceType "MARKDOWN", and inlineContent "Test content"
    And I add the document to API "<apiID>"
    Then The response status code should be 201

    # Step 3: Update an existing API document
    # First find the existing document with name as "existingDocumentID"
    When I retrieve all available documents for "<apiID>"
    Then The response status code should be 200
    And I find the document with name "<documentName>" as "existingDocumentID"

    When I retrieve document with "existingDocumentID" for "<apiID>"
    Then The response status code should be 200
    And I put the response payload in context as "documentPayload"

    # Step 3.1: Meta data update
    When I update the document "existingDocumentID" with "documentPayload" for "<apiID>" as "<config>" and value:
    """
    <configValue>
    """
    Then The response status code should be 200
    And The "apis" resource should reflect the updated "<config>" as:
      """
      <configValue>
      """

    # Step 4: Delete an existing API document
    When I delete the document with "<deleteDocument>" for "<apiID>"
    Then The response status code should be 200


    Examples:
      | apiName                  | apiVersion   | apiID         | documentName      | config       | configValue| deleteDocument      |
      | ADPRestAPI               | 1.0.0        | RestApiId     | adp-inline-doc    | sourceType   | INLINE     | documentID          |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  | NewTestDocument   | sourceType   | MARKDOWN   |existingDocumentID   |
      | ADPPhoneVerificationAPI  | 1.0.0        | SoapApiId     | NewTestDocument   | sourceType   | MARKDOWN   |existingDocumentID   |
      | ADPIfElseAPI             | 1.0.0        | AsyncApiId    | NewTestDocument   | sourceType   | MARKDOWN   |existingDocumentID   |