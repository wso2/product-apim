Feature: Migrated API Documentation Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Migrated API Documentation Management
    # Step 1: Find the api
    When I find the API created with the name "<apiName>" and version "<apiVersion>"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "<apiID>"

    # Step 2: Add a new document with ID as "documentID"
    When I prepare a new document payload with type "HOWTO", sourceType "MARKDOWN", and inlineContent "Test content"
    And I add the document to API "<apiID>"
    And I wait until the response status code is 201
    And I extract response field "documentId" and store it as "<documentID>"


    # Step 3: Update an existing API document
    # First find the existing document with name as "existingDocumentID"
    When I retrieve all available documents for "<apiID>"
    And I wait until the response status code is 200
    And I find the "documentId" with name "<documentName>" as "existingDocumentID"

    When I retrieve document with "existingDocumentID" for "<apiID>"
    And I wait until the response status code is 200
    And I put the response payload in context as "documentPayload"

    # Step 3.1: Meta data update
    When I update the document "existingDocumentID" with "documentPayload" for "<apiID>" as "<config>" and value:
    """
    <configValue>
    """
    And I wait until the response status code is 200
    And The "apis" resource should reflect the updated "<config>" as:
      """
      <configValue>
      """

    # Step 4: Delete an existing API document
    When I delete the document with "<deleteDocument>" for "<apiID>"
    And I wait until the response status code is 200


    Examples:
      | apiName                  | apiVersion   | apiID         | documentName      | config       | configValue| deleteDocument      |
      | ADPRestAPI               | 1.0.0        | RestApiId     | adp-inline-doc    | sourceType   | INLINE     | documentID          |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  | NewTestDocument   | sourceType   | MARKDOWN   |existingDocumentID   |
      | ADPPhoneVerificationAPI  | 1.0.0        | SoapApiId     | NewTestDocument   | sourceType   | MARKDOWN   |existingDocumentID   |
      | ADPIfElseAPI             | 1.0.0        | AsyncApiId    | NewTestDocument   | sourceType   | MARKDOWN   |existingDocumentID   |
