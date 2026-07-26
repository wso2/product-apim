@cleanup
Feature: Publisher API Documentation

  Publisher-plane API documentation across all four API types (REST, SOAP, WebSocket/Async and GraphQL):
  add a document to an API, list it, retrieve it, update it, and delete it. Documents are children of the
  API, so the per-scenario cleanup hook tearing down the API also removes its documents (the API is
  registered for cleanup by the create step). Each positive scenario runs as a least-privilege publisher in
  both the super tenant and tenant1.com. Base APIs are created inline per scenario (rather than via a shared
  fixture) because the document flow runs per-actor per-tenant and each scenario is self-contained under
  @cleanup.

  # REST, SOAP and WebSocket/Async share the standard create-and-deploy step, so they run as one outline
  # over the API type; GraphQL needs the schema-upload create step and is a separate scenario below.
  @cap:publisher @feat:docs @type:regression @legacy:APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase @legacy:APIM714GetAllDocumentationTestCase
  Scenario Outline: Add, retrieve, update and delete a document on a <label> API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "<payloadFile>" as "docApiId" and deployed it

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
      | label     | payloadFile                                            | actor                     |
      | REST      | artifacts/payloads/create_apim_test_api.json           | publisherUser             |
      | SOAP      | artifacts/payloads/create_apim_test_soap_api.json      | publisherUser             |
      | WebSocket | artifacts/payloads/create_apim_test_websocket_api.json | publisherUser             |
      | REST      | artifacts/payloads/create_apim_test_api.json           | publisherUser@tenant1.com |
      | SOAP      | artifacts/payloads/create_apim_test_soap_api.json      | publisherUser@tenant1.com |
      | WebSocket | artifacts/payloads/create_apim_test_websocket_api.json | publisherUser@tenant1.com |

  # GraphQL API — created via the schema-upload step; the document add/list/retrieve/update/delete flow is
  # identical (the document steps operate on the API id regardless of type).
  @cap:publisher @feat:docs @type:regression @legacy:APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase @legacy:APIM714GetAllDocumentationTestCase
  Scenario Outline: Add, retrieve, update and delete a document on a GraphQL API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "docApiId"

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
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:docs @type:negative @legacy:APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase
  Scenario Outline: A subscriber-role user cannot create an API to document as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # Doc-type breadth — all five documentation types can be added to one API (each with a unique name), then the
  # listing reflects every type. Ports APIM611/620/623/625/627 (HowTo / Samples-SDK / PublicForum / SupportForum
  # / Other). The API is created (not deployed — documents don't require a gateway deployment) and torn down by
  # @cleanup along with its documents.
  @cap:publisher @feat:docs @type:regression @legacy:APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase @legacy:APIM620AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase @legacy:APIM623AddDocumentationToAnAPIWithDocTypePublicForumThroughPublisherRestAPITestCase @legacy:APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase @legacy:APIM627AddDocumentationToAnAPIWithDocTypeOtherThroughPublisherRestAPITestCase
  Scenario Outline: All documentation types can be added to an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "docTypeApiPayload"
    And I create an "apis" resource with payload "docTypeApiPayload" as "docTypeApiId"
    When I prepare a document named "${UNIQUE:HowToDoc}" of type "HOWTO" with sourceType "INLINE" and content "howto content"
    And I add the document to API "docTypeApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:SamplesDoc}" of type "SAMPLES" with sourceType "INLINE" and content "samples content"
    And I add the document to API "docTypeApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:PublicForumDoc}" of type "PUBLIC_FORUM" with sourceType "INLINE" and content "public forum content"
    And I add the document to API "docTypeApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:SupportForumDoc}" of type "SUPPORT_FORUM" with sourceType "INLINE" and content "support forum content"
    And I add the document to API "docTypeApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:OtherDoc}" of type "OTHER" with sourceType "INLINE" and content "other content"
    And I add the document to API "docTypeApiId"
    Then The response status code should be 201
    When I retrieve all available documents for "docTypeApiId"
    Then The response status code should be 200
    And The response should contain "HOWTO"
    And The response should contain "SAMPLES"
    And The response should contain "PUBLIC_FORUM"
    And The response should contain "SUPPORT_FORUM"
    And The response should contain "OTHER"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Doc-source breadth — a document can be sourced inline, from a URL, or from an uploaded file. Ports the
  # inline/url variants (APIM611) and the file-source variant (APIM614). The file document is created as
  # metadata (sourceType FILE) then its content is uploaded via multipart.
  @cap:publisher @feat:docs @type:regression @legacy:APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase @legacy:APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
  Scenario Outline: A document can be added from inline, url and file sources as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "docSrcApiPayload"
    And I create an "apis" resource with payload "docSrcApiPayload" as "docSrcApiId"
    When I prepare a document named "${UNIQUE:InlineSrcDoc}" of type "HOWTO" with sourceType "INLINE" and content "inline source content"
    And I add the document to API "docSrcApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:UrlSrcDoc}" of type "HOWTO" with sourceType "URL" and content "https://wso2.com/documentation"
    And I add the document to API "docSrcApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:FileSrcDoc}" of type "HOWTO" with sourceType "FILE" and content ""
    And I add the document to API "docSrcApiId"
    Then The response status code should be 201
    When I upload the document file "artifacts/docs/sample-doc.txt" for document "documentID" of API "docSrcApiId"
    Then The response status code should be 201
    When I retrieve all available documents for "docSrcApiId"
    Then The response status code should be 200
    And The response should contain "INLINE"
    And The response should contain "URL"
    And The response should contain "FILE"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Ports UsersAndDocsInAPIOverviewTestCase — the two facets of an API's overview: the "Users" tab (how many
  # applications/subscribers are subscribed) and the "Documentation" tab (how many documents). Two DIFFERENT users
  # each subscribe an application, so the publisher subscriptions list carries BOTH subscribers (count 2); two
  # documents are added, so the documents list carries both (count 2). Spans the provider plane (admin publishes,
  # lists subscriptions + docs) and the consumer plane (two subscribers), so it runs as the admin actor. ×2 tenant.
  @cap:publisher @feat:docs @type:regression @rule:api-overview @dep:devportal @legacy:UsersAndDocsInAPIOverviewTestCase
  Scenario Outline: An API overview reflects its subscription and documentation counts as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ovApiId" and deployed it
    When I publish the "apis" resource with id "ovApiId"
    Then The lifecycle status of API "ovApiId" should be "Published"

    # First subscriber (the admin actor): create an application and subscribe it.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "ovApp1"
    And I create an application with payload "ovApp1"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "ovApp1Id"
    When I put the following JSON payload in context as "ovSub1"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "ovApiId" using application "ovApp1Id" with payload "ovSub1" as "ovSub1Id"
    Then The response status code should be 201

    # Second subscriber (a different consumer): switch actor, register its DCR client, mint its devportal token,
    # subscribe its own app.
    Given I act as "<subscriber>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "ovApp2"
    And I create an application with payload "ovApp2"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "ovApp2Id"
    When I put the following JSON payload in context as "ovSub2"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "ovApiId" using application "ovApp2Id" with payload "ovSub2" as "ovSub2Id"
    Then The response status code should be 201

    # The publisher subscriptions list now carries BOTH subscriptions (count 2).
    Given I act as "<actor>"
    When I retrieve the subscriptions of API "ovApiId"
    Then The response status code should be 200
    And The response should contain "\"count\":2"

    # Add two documents; the documents list carries both (count 2).
    When I prepare a document named "${UNIQUE:OverviewDoc1}" of type "HOWTO" with sourceType "INLINE" and content "test doc 1"
    And I add the document to API "ovApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:OverviewDoc2}" of type "HOWTO" with sourceType "INLINE" and content "test doc 2"
    And I add the document to API "ovApiId"
    Then The response status code should be 201
    When I retrieve all available documents for "ovApiId"
    Then The response status code should be 200
    And The response should contain "\"count\":2"

    Examples:
      | actor             | subscriber         |
      | admin             | subscriberUser     |
      | admin@tenant1.com | subscriberUser@tenant1.com |
