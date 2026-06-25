@cleanup
Feature: Publisher API Documentation

  Publisher-plane API documentation: add a document to an API, list it, retrieve it, update it, and
  delete it. Documents are children of the API, so the per-scenario cleanup hook tearing down the API
  also removes its documents (the API is registered for cleanup by the create step).

  @cap:publisher @feat:docs @type:regression @legacy:APIMANAGERDocumentationTestCase
  Scenario Outline: Add, retrieve, update and delete an API document as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "docApiId" and deployed it

    # Add a new document
    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "Test content for inline document"
    And I add the document to API "docApiId"
    Then The response status code should be 201

    # List all documents
    When I retrieve all available documents for "docApiId"
    Then The response status code should be 200

    # Retrieve the specific document
    When I retrieve document with "documentID" for "docApiId"
    Then The response status code should be 200

    # Update the document
    When I prepare a new document payload with type "SAMPLES", sourceType "INLINE", and inlineContent "updated content"
    And I update the document with "documentID" for API "docApiId"
    Then The response status code should be 200
    And The response should contain "SAMPLES"

    # Delete the document
    When I delete the document with "documentID" for "docApiId"
    Then The response status code should be 200

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:docs @type:negative @legacy:APIMANAGERDocumentationTestCase
  Scenario Outline: A subscriber-role user cannot create an API to document as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
