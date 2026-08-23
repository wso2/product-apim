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

    # List all documents — EXACTLY the one document just added. APIM714GetAllDocumentationTestCase pins the list
    # size (its assertion is list.size() == 1, not a bare 200), so a stray/duplicated document fails here.
    When I retrieve all available documents for "docApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"

    # Retrieve the specific document
    When I retrieve document with "documentID" for "docApiId"
    Then The response status code should be 200

    # Update the document's CONTENT (the /content resource — separate from the create payload's inlineContent
    # metadata), then its metadata. This is APIM714's exact order: content update, then document update, then list.
    # Pinned live: the content POST answers 201 (with the document DTO as its body), not the 200 the
    # publisher-api.yaml spec documents for this operation.
    When I add inline content "updated documentation content" to document "documentID" of API "docApiId"
    Then The response status code should be 201

    # Update the document
    When I prepare a new document payload with type "SAMPLES", sourceType "INLINE", and inlineContent "updated content"
    And I update the document with "documentID" for API "docApiId"
    Then The response status code should be 200
    And The response should contain "SAMPLES"

    # Neither update created a second document: the listing still carries EXACTLY one, now of type SAMPLES. This
    # is the assertion APIM714 exists for — an update that inserted instead of replacing would show count 2 here
    # while every individual call still returned its happy-path status.
    When I retrieve all available documents for "docApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The response should contain "SAMPLES"
    And The response should not contain "HOWTO"

    # Delete the document, then RE-LIST to confirm the deletion actually took effect — the documents list must be
    # empty. Ports the second half of APIM627.testRemoveDocumentationOtherTheAPI; without this re-read a delete
    # that answered 200 while leaving the document in place would pass unnoticed.
    When I delete the document with "documentID" for "docApiId"
    Then The response status code should be 200
    When I retrieve all available documents for "docApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "0"
    And The response array field "list" should have exactly 0 entries
    And The response field "list[*].documentId" should be exactly the list ""

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

    # Delete the document, then re-list to confirm the documents list is empty (see the REST/SOAP/WS outline).
    When I delete the document with "documentID" for "docApiId"
    Then The response status code should be 200
    When I retrieve all available documents for "docApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "0"
    And The response array field "list" should have exactly 0 entries
    And The response field "list[*].documentId" should be exactly the list ""

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Parameter tampering on the document-content path. Ports DocAPIParameterTamperingTest: a request for document
  # content whose apiId path segment carries an injected value must be REJECTED, and must not echo the injected
  # payload or leak a stack trace. The API/document ids here are deliberately literals, not context references —
  # the point is that they are not real ids. The rejection is 401 because ';' opens a JAX-RS matrix parameter, so
  # the apiId segment resolves to empty and the request matches no protected publisher resource, which the auth
  # handler answers as an unauthenticated request rather than a 404.
  @cap:publisher @feat:docs @rule:parameter-tampering @type:negative @legacy:DocAPIParameterTamperingTest
  Scenario Outline: A tampered API id on the document-content path is rejected without leaking a stack trace as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to retrieve publisher document content with tampered API id ";alert(1)" and document id "daf732d3-bda2-46da-b381-2c39d901ea61"
    Then The response status code should be 401
    And The response should not contain "alert(1)"
    And The response should not contain "java.lang."
    And The response should not contain "org.wso2.carbon"

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
    And The response array field "list" should have exactly 5 entries
    And The response field "list[*].type" should be exactly the list "HOWTO,SAMPLES,PUBLIC_FORUM,SUPPORT_FORUM,OTHER"

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
    And The response array field "list" should have exactly 3 entries
    And The response field "list[*].sourceType" should be exactly the list "INLINE,URL,FILE"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Ports UsersAndDocsInAPIOverviewTestCase — the two facets of an API's overview: the "Users" tab (how many
  # applications/subscribers are subscribed) and the "Documentation" tab (how many documents). Two DIFFERENT users
  # each subscribe an application, so the publisher subscriptions list carries BOTH subscribers (count 2); two
  # documents are added, so the documents list carries both (count 2). Spans the provider plane (admin publishes,
  # lists subscriptions + docs) and the consumer plane (two subscribers), so it runs as the admin actor. ×2 tenant.
  #
  # The THIRD row makes the second subscriber a SECONDARY.COM user-store consumer (CLAUDE.md §12), closing the
  # legacy SUPER_TENANT_USER_STORE_USER mode for the subscriber-identity facet. It belongs here, on this scenario,
  # because this is the one place in the corpus that asserts a server-returned username EXACTLY: a store user's
  # physical name carries its store domain (SECONDARY.COM/subscriberUser1), so the subscriptions list naming it
  # verbatim proves the store-qualified identity survives the whole subscribe → publisher-read round trip. The
  # row stays in the super tenant: it is the store identity, not the tenant, that is the variable here, and the
  # ×2-tenant rows above already vary the tenant.
  @cap:publisher @feat:docs @type:regression @rule:api-overview @dep:devportal @legacy:UsersAndDocsInAPIOverviewTestCase
  Scenario Outline: An API overview reflects its subscription and documentation counts as <actor> for <subscriber>
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

    # Intermediate count: after the FIRST subscriber only, the publisher subscriptions list carries exactly one
    # entry, and it is attributed to that subscriber. Without this the final count-2 assertion cannot distinguish
    # "two subscribers arrived" from "one subscriber counted twice".
    # The subscriber recorded on the subscription is the application OWNER. Resolved from the actor registry
    # rather than hardcoded: the actor reference is not the username (admin vs admin@tenant1.com, and the
    # email-username mode adds an @email.com local part), so a literal would be wrong in at least one mode.
    When I store the username of actor "<actor>" as "ovSubscriber1Name"
    And I retrieve the subscriptions of API "ovApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "list[0].applicationInfo.subscriber" should be "{{ovSubscriber1Name}}"

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

    # The publisher subscriptions list now carries BOTH subscriptions (count 2) and names BOTH subscribers. The
    # per-subscriber assertion is what makes the count meaningful: a count of 2 alone is satisfied by one
    # subscriber holding two applications, which is not what this scenario set up.
    Given I act as "<actor>"
    When I store the username of actor "<subscriber>" as "ovSubscriber2Name"
    And I retrieve the subscriptions of API "ovApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The response should contain "\"subscriber\":\"{{ovSubscriber1Name}}\""
    And The response should contain "\"subscriber\":\"{{ovSubscriber2Name}}\""

    # Add two documents; the documents list carries both (count 2).
    When I prepare a document named "${UNIQUE:OverviewDoc1}" of type "HOWTO" with sourceType "INLINE" and content "test doc 1"
    And I add the document to API "ovApiId"
    Then The response status code should be 201
    When I prepare a document named "${UNIQUE:OverviewDoc2}" of type "HOWTO" with sourceType "INLINE" and content "test doc 2"
    And I add the document to API "ovApiId"
    Then The response status code should be 201
    When I retrieve all available documents for "ovApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "2"

    Examples:
      | actor             | subscriber                     |
      | admin             | subscriberUser                 |
      | admin@tenant1.com | subscriberUser@tenant1.com     |
      | admin             | SECONDARY.COM/subscriberUser1  |
