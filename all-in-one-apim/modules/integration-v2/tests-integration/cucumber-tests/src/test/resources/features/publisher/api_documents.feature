Feature: API Documents
  This feature tests API Documents functionality including adding new documents,
  editing them and deleting them.

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Add a new Document for API
    # Step 1: Create base API - "createdApiId" stored in context
    Given I have created an api and deployed it

    # Step 2: Add new documents to the API- "documentID" stored in context
    When I prepare a new document payload with type "<type>", sourceType "<sourceType>", and inlineContent "<inlineContent>"
    And I add the document to API "<createdApiId>"
    Then The response status code should be 201

    # Step 3: Retrieve all available document for API
    When I retrieve all available documents for "<createdApiId>"
    Then The response status code should be 200

    # Step 4: Retrieve specific document for API
    When I retrieve document with "<documentID>" for "<createdApiId>"
    Then The response status code should be 200

    # Step 5: Update an existing document
    When I prepare a new document payload with type "SAMPLES", sourceType "INLINE", and inlineContent "updated content"
    And I update the document with "<documentID>" for API "<createdApiId>"
    Then The response status code should be 200
    And The response should contain "SAMPLES"

    # Step 6: Delete an existing document
    When I delete the document with "<documentID>" for "<createdApiId>"
    Then The response status code should be 200

    # Step 7:Delete the base API
    When I delete the API with id "<createdApiId>"
    Then The response status code should be 200

    Examples:
      | type    | sourceType | inlineContent                      |
      | HOWTO   | INLINE     | Test content for inline document   |