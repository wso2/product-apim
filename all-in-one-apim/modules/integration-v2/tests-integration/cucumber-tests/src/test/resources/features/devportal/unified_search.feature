@cleanup
Feature: DevPortal Unified Search

  Unified Search — {@code GET /search} on the devportal and publisher planes. A DIFFERENT endpoint from
  {@code GET /apis?query=}, and the only one that can report a match inside a DOCUMENT's content: /apis returns an
  APIListDTO (API objects only), whereas /search returns a SearchResultList of MIXED types. Its OAS tag is literally
  "Unified Search", summarised "Retrieve/Search APIs and API Documents by Content", and the product documentation
  describes it as the default search option, matching "across the API name, context, version, documents, and
  definition of all APIs".

  WHY THIS FEATURE EXISTS. Document-content search was previously recorded in this suite as unavailable on this
  build, with the legacy test judged unportable. That was a measurement error: the probes went to /apis?query=,
  where a document match cannot appear by contract, so the zeros they returned were correct behaviour rather than a
  defect. Legacy searched the right place all along — RestAPIStoreImpl/RestAPIPublisherImpl.searchAPIs call
  unifiedSearchApi.searchGetWithHttpInfo, i.e. /search. This feature covers that endpoint, which had NO coverage,
  no step and no URL builder before.

  Every scenario carries its own content token so concurrent/sequential scenarios cannot inflate each other's
  counts, and each creates its own API inline (a shared fixture cannot work: these scenarios mutate and delete
  documents). Torn down by the per-scenario cleanup hook.

  # Ports ContentSearchTestCase#testDocumentContentSearch faithfully: a HOWTO / INLINE document whose CONTENT is
  # posted to the /content resource, then a free-text search for a token appearing ONLY in that content, asserted on
  # BOTH planes exactly as legacy does (count 1 each). Runs in both tenants with a distinct token per row.
  #
  # The third leg is the control that makes this a regression test rather than a coincidence, and it encodes the
  # mistake that hid this behaviour: the SAME token on /apis?query= returns 0, because documents are not APIs. If
  # someone "simplifies" a future document-content assertion onto /apis, that leg is what will stop them.
  @cap:devportal @feat:discovery @rule:document-content @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: A document's content is found by unified search on both planes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "usApiId" and deployed it
    When I publish the "apis" resource with id "usApiId"
    Then The lifecycle status of API "usApiId" should be "Published"

    # Legacy's fixture shape: HOWTO + INLINE, content written through the /content resource. The create payload's
    # inlineContent is METADATA ONLY on this build (an INLINE doc 404s on /content until this POST), so the content
    # search below is necessarily exercising the /content write path.
    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I add the document to API "usApiId"
    Then The response status code should be 201
    When I add inline content "This is a sample API to test unified search feature - <token>" to document "documentID" of API "usApiId"
    Then The response status code should be 201

    # Legacy's two assertions, in legacy's order.
    When I unified-search the "devportal" plane for "<token>" until the result count is 1 within 120 seconds
    And I unified-search the "publisher" plane for "<token>" until the result count is 1 within 120 seconds
    # CONTROL — the same token on /apis?query= is 0 BY CONTRACT, not by defect.
    And I search DevPortal APIs with query "<token>" and limit 25 until the result count is 0 within 60 seconds

    Examples:
      | actor             | token           |
      | admin             | zzdocalphatoken |
      | admin@tenant1.com | zzdocbetatoken  |

  # The result SHAPE, which no other scenario pins. One query on an API's name returns THREE associated entities and
  # the discriminator that tells them apart — the API, its DEFINITION, and its DOC — each carrying the linkage back
  # to the API. This is what makes /search "unified", and it is the documented claim that the search spans the API,
  # its documents AND its definition. The DOC entry's docType / sourceType / visibility are asserted too, so a
  # regression that returned bare ids would fail here.
  @cap:devportal @feat:discovery @rule:result-types @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario: Unified search returns API, DEFINITION and DOC entries for one API
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "usShapeApiId" and deployed it
    When I retrieve the "apis" resource with id "usShapeApiId"
    And I extract response field "name" and store it as "usShapeApiName"
    When I publish the "apis" resource with id "usShapeApiId"
    Then The lifecycle status of API "usShapeApiId" should be "Published"
    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I add the document to API "usShapeApiId"
    Then The response status code should be 201
    When I add inline content "shape probe body zzdocshapetoken" to document "documentID" of API "usShapeApiId"
    Then The response status code should be 201

    # Wait for the DOC to be indexed before asserting the shape, so the three-type assertion cannot pass or fail on
    # indexing timing.
    When I unified-search the "devportal" plane for "zzdocshapetoken" until the result count is 1 within 120 seconds
    When I unified-search the "devportal" plane once for "{{usShapeApiName}}"
    Then The response status code should be 200
    And The response should contain "\"type\":\"API\""
    And The response should contain "\"type\":\"DEFINITION\""
    And The response should contain "\"type\":\"DOC\""
    # The DOC entry's own attributes, and its linkage back to the owning API.
    And The response should contain "\"docType\":\"HOWTO\""
    And The response should contain "\"sourceType\":\"INLINE\""
    And The response should contain "\"visibility\":\"API_LEVEL\""
    And The response should contain "\"apiUUID\":\"{{usShapeApiId}}\""

  # Deleting the document withdraws it from the index. Without this, the feature would only prove the index gets
  # POPULATED, not that it is MAINTAINED — a stale index would look identical on every other assertion here.
  @cap:devportal @feat:discovery @rule:document-content @type:regression @dep:publisher
  Scenario: A deleted document is withdrawn from unified search
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "usDelApiId" and deployed it
    When I publish the "apis" resource with id "usDelApiId"
    Then The lifecycle status of API "usDelApiId" should be "Published"
    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I add the document to API "usDelApiId"
    Then The response status code should be 201
    When I add inline content "delete probe body zzdocdeltoken" to document "documentID" of API "usDelApiId"
    Then The response status code should be 201
    # Present first — so the zero below cannot pass against an index that never saw the document.
    When I unified-search the "devportal" plane for "zzdocdeltoken" until the result count is 1 within 120 seconds
    When I delete the document with "documentID" for "usDelApiId"
    Then The response status code should be 200
    When I unified-search the "devportal" plane for "zzdocdeltoken" until the result count is 0 within 120 seconds
