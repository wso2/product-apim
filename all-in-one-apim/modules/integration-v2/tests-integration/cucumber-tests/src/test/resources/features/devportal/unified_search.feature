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

  Search scenarios use a unique content token or context template so concurrent/sequential scenarios cannot
  inflate each other's counts, and each creates its own API inline. Document scenarios also create and delete
  documents. Torn down by the per-scenario cleanup hook.

  The templated-context case below exercises the Publisher /search endpoint with the raw context form
  context:<unique>/developer/{version}. It complements the Publisher /apis?query= probe in api_config.feature:
  the legacy DynamicAPIContextTestCase used unified /search, so matching only the API-list endpoint was incomplete.

  # Ports DynamicAPIContextTestCase#testAPISearchUsingTemplatedContext on the legacy endpoint. The query uses the
  # context template reconstructed from the API's stored context, and the result is checked by exact count and exact
  # API name. The scenario's unique context isolates it from APIs created concurrently.
  @cap:devportal @feat:discovery @rule:context-template @type:regression @dep:publisher @legacy:DynamicAPIContextTestCase
  Scenario Outline: Publisher unified search finds an API by its raw templated context as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_templated_context_search_api.json" as "usContextApiId" and deployed it
    And the "apis" resource "usContextApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "usContextApiId"
    Then The lifecycle status of API "usContextApiId" should be "Published"
    When I retrieve the "apis" resource with id "usContextApiId"
    Then The response status code should be 200
    And I extract response field "name" and store it as "usContextApiName"
    And I extract response field "context" and store it as "usContextApiContext"
    When I unified-search the "publisher" plane for "context:{{usContextApiContext}}/{version}" until the result count is 1 within 60 seconds
    Then The value of response field "list[0].name" should be "{{usContextApiName}}"

    Examples:
      | actor            |
      | superTenantAdmin |
      | admin@tenant1.com |

  # Ports ContentSearchTestCase#testDocumentContentSearch faithfully: a HOWTO / INLINE document whose CONTENT is
  # posted to the /content resource, then a free-text search for a token appearing ONLY in that content, asserted on
  # BOTH planes exactly as legacy does (count 1 each). The same logical tenant actors run in both standard and
  # email-user blocks; block-aware identity resolution selects the corresponding physical username.
  #
  # The third leg is the control that makes this a regression test rather than a coincidence, and it encodes the
  # mistake that hid this behaviour: the SAME token on /apis?query= returns 0, because documents are not APIs. If
  # someone "simplifies" a future document-content assertion onto /apis, that leg is what will stop them.
  @cap:devportal @feat:discovery @rule:document-content @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: A document's content is found by unified search on both planes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "usApiId" and deployed it
    And the "apis" resource "usApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "usApiId"
    Then The lifecycle status of API "usApiId" should be "Published"
    When I retrieve the "apis" resource with id "usApiId"
    And I extract response field "name" and store it as "usApiName"
    And I wait until the DevPortal API index contains API "usApiId" named "{{usApiName}}" within 120 seconds
    # Legacy ContentSearchTestCase first warmed the Publisher API index in its preceding search test. Keep that
    # prerequisite explicit here so document-content indexing is not started while the Publisher index is still cold.
    And I search Publisher APIs with content query "name:{{usApiName}}" until the result count is 1 within 120 seconds

    # Legacy's fixture shape: HOWTO + INLINE, content written through the /content resource. The create payload's
    # inlineContent is METADATA ONLY on this build (an INLINE doc 404s on /content until this POST), so the content
    # search below is necessarily exercising the /content write path.
    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I add the document to API "usApiId"
    Then The response status code should be 201
    When I add inline content "This is a sample API to test unified search feature - <token>" to document "documentID" of API "usApiId"
    Then The response status code should be 201

    # Legacy's two assertions, in legacy's order. Publisher search is the first exact-count content-index
    # convergence gate; only after it succeeds do we assert the tenant-aware DevPortal search surface.
    When I unified-search the "publisher" plane for "<token>" until the result count is 1 within 120 seconds
    And I unified-search the "devportal" plane for "<token>" until the result count is 1 within 120 seconds
    # CONTROL — the same token on /apis?query= is 0 BY CONTRACT, not by defect.
    And I search DevPortal APIs with query "<token>" and limit 25 until the result count is 0 within 60 seconds

    Examples:
      | actor             | token           |
      | superTenantAdmin | zzdocalphatoken |
      | admin@tenant1.com | zzdocbetatoken |

  # The ordinary SECONDARY.COM mode is separate from emailUserMode. It exercises the same role-filtered Publisher
  # search contract as ContentSearchTestCase's SUPER_TENANT_USER_STORE_USER factory row using scenario-owned
  # secondary-store roles/users, with both super-tenant and tenant actors. The role/user cleanup is registered
  # before the API is created, so a later assertion failure cannot leak store principals into the block.
  @cap:publisher @feat:visibility @rule:access-control @type:regression @legacy:ContentSearchTestCase
  Scenario Outline: Publisher unified search filters a secondary-store user by access-control role in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    When I provision a secondary user-store role "SECONDARY.COM/ssAclRole" in tenant "<tenant>" with visibility permissions "false"
    And I provision secondary user-store user "SECONDARY.COM/ssAclIn" with password "Secondary@123" and roles "Internal/creator,Internal/publisher,SECONDARY.COM/ssAclRole" as an actor in tenant "<tenant>"
    And I provision secondary user-store user "SECONDARY.COM/ssAclOut" with password "Secondary@123" and roles "Internal/creator,Internal/publisher" as an actor in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "SECONDARY.COM/ssAclIn<suffix>"
    And The system is ready and I have valid publisher access tokens as "SECONDARY.COM/ssAclOut<suffix>"

    Given I act as "admin<suffix>"
    And I generate a unique value and store it as "ssAclDesc"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ssAclPayload"
    And I set the description of context payload "ssAclPayload" to "Secondary access search {{ssAclDesc}}"
    When I have created an api from context payload "ssAclPayload" with restricted access control for roles "SECONDARY.COM/ssAclRole" as "ssAclApiId" and deployed it
    And the "apis" resource "ssAclApiId" should be live on the gateway, redeploying if propagation is lost
    And I publish the "apis" resource with id "ssAclApiId"
    Then The lifecycle status of API "ssAclApiId" should be "Published"

    When I act as "SECONDARY.COM/ssAclIn<suffix>"
    And I unified-search the "publisher" plane for "{{ssAclDesc}}" until the result count is 2 within 60 seconds
    When I act as "SECONDARY.COM/ssAclOut<suffix>"
    And I unified-search the "publisher" plane for "{{ssAclDesc}}" until the result count is 0 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # The ordinary secondary-store subscriber users below differ only by the API's configured Store visibility
  # role. This covers the legacy secondary-store Store-search filter without conflating it with email-user mode.
  @cap:devportal @feat:discovery @rule:store-visibility @type:regression @legacy:ContentSearchTestCase
  Scenario Outline: DevPortal unified search filters a secondary-store user by Store-visibility role in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    When I provision a secondary user-store role "SECONDARY.COM/ssVisRole" in tenant "<tenant>" with visibility permissions "true"
    And I provision secondary user-store user "SECONDARY.COM/ssVisIn" with password "Secondary@123" and roles "Internal/subscriber,SECONDARY.COM/ssVisRole" as an actor in tenant "<tenant>"
    And I provision secondary user-store user "SECONDARY.COM/ssVisOut" with password "Secondary@123" and roles "Internal/subscriber" as an actor in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "SECONDARY.COM/ssVisIn<suffix>"
    And The system is ready and I have valid devportal access token as "SECONDARY.COM/ssVisOut<suffix>"

    Given I act as "admin<suffix>"
    And I generate a unique value and store it as "ssVisDesc"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ssVisPayload"
    And I set the description of context payload "ssVisPayload" to "Secondary visibility search {{ssVisDesc}}"
    When I have created an api from context payload "ssVisPayload" with restricted visibility for roles "SECONDARY.COM/ssVisRole" as "ssVisApiId" and deployed it
    And the "apis" resource "ssVisApiId" should be live on the gateway, redeploying if propagation is lost
    And I publish the "apis" resource with id "ssVisApiId"
    Then The lifecycle status of API "ssVisApiId" should be "Published"

    When I act as "SECONDARY.COM/ssVisIn<suffix>"
    And I unified-search the "devportal" plane for "{{ssVisDesc}}" until the result count is 2 within 60 seconds
    When I act as "SECONDARY.COM/ssVisOut<suffix>"
    And I unified-search the "devportal" plane for "{{ssVisDesc}}" until the result count is 0 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # This uses the SECONDARY.COM login path in both standard and email-user blocks. The logical actor remains the
  # same while the block-aware identity resolver selects its physical username. Search for a unique term occurring
  # only in document content and assert the specific DOC -> API identity, not a global result count. The authorized
  # lookup warms/confirms the indexed document before the unauthorized lookup checks visibility.
  @secondary-store-document-content-search @cap:devportal @feat:discovery @rule:document-content @rule:store-visibility @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: Ordinary secondary-store user finds only role-visible document content in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And The system is ready and I have valid publisher access tokens as "SECONDARY.COM/publisherUser1<suffix>"
    And The system is ready and I have valid devportal access token as "SECONDARY.COM/publisherUser1<suffix>"
    And The system is ready and I have valid devportal access token as "SECONDARY.COM/subscriberUser1<suffix>"

    Given I act as "admin<suffix>"
    And I generate a unique value and store it as "ssDocToken"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ssDocApiId" and deployed it
    And the "apis" resource "ssDocApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "ssDocApiId"
    Then The lifecycle status of API "ssDocApiId" should be "Published"
    When I retrieve the "apis" resource with id "ssDocApiId"
    And I extract response field "name" and store it as "ssDocApiName"
    # Ensure the parent API is visible in the Publisher index before adding/indexing its document, matching the
    # proven legacy ordering without depending on the shared search result count.
    And I unified-search the "publisher" plane for "name:{{ssDocApiName}}" until API "ssDocApiId" is present within 120 seconds
    And I update API "ssDocApiId" to restricted Store visibility for role "Internal/publisher" and no Publisher access restriction

    When I generate a unique value and store it as "ssDocIndexName"
    And I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I set the prepared document name to "{{ssDocIndexName}}"
    And I add the document to API "ssDocApiId"
    Then The response status code should be 201
    # Let the metadata-only registry index update settle before posting inline content, which triggers a second
    # update for the same document. This keeps the two asynchronous index events from racing each other.
    And I unified-search the "publisher" plane for "content:{{ssDocIndexName}}" until document "documentID" of API "ssDocApiId" is present within 120 seconds
    When I add inline content "ordinary secondary-store document {{ssDocToken}}" to document "documentID" of API "ssDocApiId"
    Then The response status code should be 201

    When I act as "SECONDARY.COM/publisherUser1<suffix>"
    And I unified-search the "publisher" plane for "{{ssDocToken}}" until the result count is 1 within 120 seconds
    And I unified-search the "publisher" plane for "{{ssDocToken}}" until the document linked to API "ssDocApiId" is present within 120 seconds
    And I unified-search the "devportal" plane for "{{ssDocToken}}" until the result count is 1 within 120 seconds
    And I unified-search the "devportal" plane for "{{ssDocToken}}" until the document linked to API "ssDocApiId" is present within 120 seconds
    When I act as "SECONDARY.COM/subscriberUser1<suffix>"
    And I unified-search the "devportal" plane for "{{ssDocToken}}" until the result count is 0 within 60 seconds
    And I unified-search the "devportal" plane for "{{ssDocToken}}" until the document linked to API "ssDocApiId" is absent within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # ContentSearchTestCase changes Store visibility to RESTRICTED before it adds/indexes the document. Keep that
  # ordering: a fresh unrestricted document would not prove that document-content results honor the restricted
  # API's visibleRoles. Publisher indexes the document for its owner; DevPortal exposes it only to the role bearer.
  @cap:devportal @feat:discovery @rule:document-content @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: Unified document search honors Store visibility already restricted on the API in <tenant>
    Given The system is ready
    And I have valid access tokens as "<owner>"
    And I generate a unique value and store it as "docVisRole"
    And I generate a unique value and store it as "docVisToken"
    And I provision store-visibility role "{{docVisRole}}" in tenant "<tenant>"
    And I provision user "docVisIn" with roles "Internal/subscriber,{{docVisRole}}" in tenant "<tenant>"
    And I provision user "docVisOut" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "docVisIn<suffix>"
    And The system is ready and I have valid devportal access token as "docVisOut<suffix>"

    Given I act as "<owner>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "docVisApiId" and deployed it
    And the "apis" resource "docVisApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "docVisApiId"
    Then The lifecycle status of API "docVisApiId" should be "Published"
    And I update API "docVisApiId" to restricted Store visibility for role "{{docVisRole}}" and no Publisher access restriction

    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I add the document to API "docVisApiId"
    Then The response status code should be 201
    When I add inline content "Restricted document content {{docVisToken}}" to document "documentID" of API "docVisApiId"
    Then The response status code should be 201
    When I unified-search the "publisher" plane for "{{docVisToken}}" until the result count is 1 within 120 seconds
    When I act as "docVisIn<suffix>"
    And I unified-search the "devportal" plane for "{{docVisToken}}" until the result count is 1 within 120 seconds
    When I act as "docVisOut<suffix>"
    And I unified-search the "devportal" plane for "{{docVisToken}}" until the result count is 0 within 120 seconds

    Examples:
      | tenant       | suffix       | owner            |
      | carbon.super |              | superTenantAdmin |
      | tenant1.com  | @tenant1.com | admin@tenant1.com |

  # The result SHAPE, which no other scenario pins. One query on an API's name returns THREE associated entities and
  # the discriminator that tells them apart — the API, its DEFINITION, and its DOC — each carrying the linkage back
  # to the API. This is what makes /search "unified", and it is the documented claim that the search spans the API,
  # its documents AND its definition. The DOC entry's docType / sourceType / visibility are asserted too, so a
  # regression that returned bare ids would fail here.
  @cap:devportal @feat:discovery @rule:result-types @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: Unified search returns API, DEFINITION and DOC entries for one API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "usShapeApiId" and deployed it
    And the "apis" resource "usShapeApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "usShapeApiId"
    And I extract response field "name" and store it as "usShapeApiName"
    When I publish the "apis" resource with id "usShapeApiId"
    Then The lifecycle status of API "usShapeApiId" should be "Published"
    When I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I add the document to API "usShapeApiId"
    Then The response status code should be 201
    When I add inline content "shape probe body zzdocshapetoken" to document "documentID" of API "usShapeApiId"
    Then The response status code should be 201

    # Match the legacy ordering: first wait for the document content in the Publisher index, then verify the
    # tenant-aware DevPortal index. The shape assertions below remain exact and unchanged.
    When I unified-search the "publisher" plane for "zzdocshapetoken" until the result count is 1 within 120 seconds
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

    Examples:
      | actor            |
      | superTenantAdmin |
      | admin@tenant1.com |

  # Deleting the document withdraws it from the index. Without this, the feature would only prove the index gets
  # POPULATED, not that it is MAINTAINED — a stale index would look identical on every other assertion here.
  @cap:devportal @feat:discovery @rule:document-content @type:regression @dep:publisher
  Scenario Outline: A deleted document is withdrawn from unified search as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "usDelApiId" and deployed it
    And the "apis" resource "usDelApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "usDelApiId"
    Then The lifecycle status of API "usDelApiId" should be "Published"
    When I generate a unique value and store it as "usDelDocIndexName"
    And I prepare a new document payload with type "HOWTO", sourceType "INLINE", and inlineContent "placeholder"
    And I set the prepared document name to "{{usDelDocIndexName}}"
    And I add the document to API "usDelApiId"
    Then The response status code should be 201
    # Wait for the created metadata entry before the inline-content update to avoid overlapping index events.
    And I unified-search the "devportal" plane for "content:{{usDelDocIndexName}}" until document "documentID" of API "usDelApiId" is present within 120 seconds
    When I add inline content "delete probe body zzdocdeltoken" to document "documentID" of API "usDelApiId"
    Then The response status code should be 201
    # Present first — so the zero below cannot pass against an index that never saw the document.
    When I unified-search the "devportal" plane for "zzdocdeltoken" until the result count is 1 within 120 seconds
    When I delete the document with "documentID" for "usDelApiId"
    Then The response status code should be 200
    When I unified-search the "devportal" plane for "zzdocdeltoken" until the result count is 0 within 120 seconds

    Examples:
      | actor            |
      | superTenantAdmin |
      | admin@tenant1.com |
