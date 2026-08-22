@cleanup
Feature: Publisher Service Catalog

  Service Catalog management over the /api/am/service-catalog/v1/services REST API: register a service (multipart
  metadata + OpenAPI definition), retrieve it and its definition, search by name/version/type/key with pagination
  and sorting, update it, import/export it, read which APIs consume it, and delete it — plus the negatives
  (duplicate key, missing definition, invalid id on the entry/definition/usage/update/delete endpoints, invalid
  search parameters, and deleting a service an API still uses). Ports ServiceCatalogRestAPITestCase in full. Runs as
  the tenant admin in both tenants; each scenario uses a scenario-unique service key/name/version so parallel runs
  stay isolated, and every scenario is self-contained — none depends on another's artifacts or on declaration order.
  Services and APIs are torn down by the cleanup hook, which sweeps APIs before services so a service still bound
  to an API is never left undeletable.

  @cap:publisher @feat:service-catalog @type:regression @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Register, retrieve, search and delete a service catalog entry as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "svcKey"
    And I generate a unique value and store it as "svcName"

    # Create a service (multipart metadata + OAS3 definition).
    When I create a service catalog entry named "{{svcName}}" version "v1" key "{{svcKey}}" from definition "artifacts/service-catalog/definition1.yaml" as "svcId"
    Then The value of response field "name" should be "{{svcName}}"

    # Retrieve it by id, and its definition.
    When I retrieve the service catalog entry "svcId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{svcName}}"
    When I retrieve the definition of service catalog entry "svcId"
    Then The response status code should be 200
    # definition1.yaml is a Swagger 2.0 document, so the returned definition carries the "swagger" key.
    And The response should contain "swagger"

    # Search by name, version, type and key each returns the entry.
    When I search service catalog entries by "name" "{{svcName}}"
    Then The response status code should be 200
    And The response list should have 1 entries
    When I search service catalog entries by "key" "{{svcKey}}"
    Then The response list should have 1 entries
    When I search service catalog entries by "definitionType" "OAS3"
    Then The response status code should be 200
    And The response should contain "{{svcName}}"

    # Delete it, then a re-fetch is 404.
    When I delete the service catalog entry "svcId"
    Then The response status code should be 204
    When I retrieve the service catalog entry "svcId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negatives: a duplicate service key is 409, a create with no definition file is 400, and an invalid/unknown id
  # is 404. Ports the negative assertions of testCreateAService + testGetServiceByUUID.
  @cap:publisher @feat:service-catalog @type:negative @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Service catalog entry validation errors as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "dupKey"
    And I generate a unique value and store it as "dupName"
    When I create a service catalog entry named "{{dupName}}" version "v1" key "{{dupKey}}" from definition "artifacts/service-catalog/definition1.yaml" as "dupSvcId"
    # A second entry reusing the same key is rejected 409.
    When I attempt to create a service catalog entry named "{{dupName}}2" version "v1" key "{{dupKey}}" from definition "artifacts/service-catalog/definition2.yaml"
    Then The response status code should be 409
    # A create with no definition file is rejected 400.
    When I attempt to create a service catalog entry named "{{dupName}}3" version "v1" key "{{dupKey}}3" without a definition
    Then The response status code should be 400
    # An unknown service id is 404.
    When I retrieve the service catalog entry with raw id "01234567-0123-0123-0123-012345678901"
    Then The response status code should be 404
    # The DEFINITION endpoint is a separate resource with its own lookup, so its unknown-id 404 is asserted
    # separately from the entry 404 above (ports the invalid-UUID half of testGetServiceDefinition).
    When I generate a random UUID and store it as "unknownSvcId"
    And I retrieve the definition of service catalog entry "unknownSvcId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Pagination: three entries sharing a scenario-unique name prefix — limit caps the page, offset skips. Ports the
  # limit/offset assertions of testSearchService (reduced to a deterministic 3-entry set).
  @cap:publisher @feat:service-catalog @type:regression @rule:pagination @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Service catalog search pagination as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "pgKey"
    When I create a service catalog entry named "{{pgKey}}0" version "v1" key "{{pgKey}}0" from definition "artifacts/service-catalog/definition1.yaml" as "pgSvc0"
    And I create a service catalog entry named "{{pgKey}}1" version "v1" key "{{pgKey}}1" from definition "artifacts/service-catalog/definition1.yaml" as "pgSvc1"
    And I create a service catalog entry named "{{pgKey}}2" version "v1" key "{{pgKey}}2" from definition "artifacts/service-catalog/definition1.yaml" as "pgSvc2"
    # All three share the prefix -> full search returns 3; limit 2 caps at 2; offset 2 returns the last 1.
    When I search service catalog entries by name "{{pgKey}}" with limit 10 and offset 0
    Then The response list should have 3 entries
    When I search service catalog entries by name "{{pgKey}}" with limit 2 and offset 0
    Then The response list should have 2 entries
    When I search service catalog entries by name "{{pgKey}}" with limit 10 and offset 2
    Then The response list should have 1 entries

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the import surface of ServiceCatalogRestAPITestCase (testImportService): a services archive (.zip
  # carrying serviceMetadata + a definition) is registered in one call. service1.zip carries the
  # Pizzashack-Endpoint-v2 service; the missing-file negative asserts the endpoint's own validation. Runs
  # x2-tenant (super + tenant1): the service catalog is tenant-scoped (the tenant is derived from the acting
  # actor's token), so each tenant imports the fixed-named service into its OWN isolated catalog with no collision.
  @cap:publisher @feat:service-catalog @type:regression @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Import a service catalog archive and reject an import with no file as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import a service catalog archive "artifacts/service-catalog/service1.zip" with overwrite "true" as "importedSvcId"
    Then The response status code should be 200
    And The response should contain "Pizzashack-Endpoint-v2"
    When I attempt to import a service catalog archive with no file
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Import overwrite semantics (testImportService), verified live end-to-end. The Service Catalog import is
  # HASH-based (the response carries an md5/SHA-256 of the service), so re-import behaviour depends on BOTH the
  # overwrite flag AND whether the incoming archive differs from the stored service:
  #   * identical content  -> 200, ZERO imported (idempotent no-op), regardless of the flag.
  #   * changed content, overwrite=false -> 400 "Cannot update existing services when overwrite is false".
  #   * changed content, overwrite=true  -> 200, ONE imported (the stored service is overwritten).
  # service2.zip is Pizzashack-Endpoint 1.0.0 (distinct from service1.zip's v2) and service2_modified.zip is the
  # SAME serviceKey with a changed description/serviceUrl/definition. The scenario is self-contained: the first
  # import creates the service and every later step operates on that one service (registered for teardown).
  # Runs x2-tenant (super + tenant1): the catalog is tenant-scoped, so the fixed serviceKey lives in each
  # tenant's OWN catalog and the two rows never collide.
  # NOTE: legacy ServiceCatalogRestAPITestCase.testImportService asserted 400 for the overwrite=false re-import,
  # but its assertEquals sat INSIDE a catch block that never fires when the call returns 200 — so on the
  # identical/no-op path it silently passed without ever verifying the 400 (a latent false-pass). This port pins
  # both the real 400 (changed content) and the idempotent 200 no-op (identical content).
  @cap:publisher @feat:service-catalog @type:regression @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Re-importing an existing service is gated by the overwrite flag and content changes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # First import creates the service.
    When I import a service catalog archive "artifacts/service-catalog/service2.zip" with overwrite "true" as "conflictSvcId"
    Then The response status code should be 200
    And The response should contain "Pizzashack-Endpoint"
    # Re-import of the IDENTICAL archive is a 200 no-op regardless of the flag: nothing changed, nothing imported,
    # no duplicate created.
    When I attempt to import a service catalog archive "artifacts/service-catalog/service2.zip" with overwrite "false"
    Then The response status code should be 200
    And The service catalog import should report 0 imported services
    When I attempt to import a service catalog archive "artifacts/service-catalog/service2.zip" with overwrite "true"
    Then The response status code should be 200
    And The service catalog import should report 0 imported services
    # Re-import of a CHANGED archive (same serviceKey, different content) with overwrite=false is REJECTED (400).
    When I attempt to import a service catalog archive "artifacts/service-catalog/service2_modified.zip" with overwrite "false"
    Then The response status code should be 400
    And The response should contain "Cannot update existing services when overwrite is false"
    # The same changed archive with overwrite=true is accepted and overwrites the stored service (one imported).
    When I attempt to import a service catalog archive "artifacts/service-catalog/service2_modified.zip" with overwrite "true"
    Then The response status code should be 200
    And The service catalog import should report 1 imported services

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports testUpdateService. The identity fields (name/version/serviceKey) survive an update untouched while the
  # mutable metadata (description) is replaced, and the entry keeps the SAME id — the update is in-place, not a
  # replace-with-new-row. The two negatives use the non-asserting attempt-variants so the status is asserted in the
  # feature: legacy put its 404/400 assertions inside catch blocks, which never fire if the call unexpectedly
  # succeeds and so passed silently. Legacy also asserted a "null serviceMetadata" case, but that assertion is on a
  # message thrown CLIENT-SIDE by the generated Java SDK before any request is made, not on product behaviour, so it
  # is deliberately not ported.
  @cap:publisher @feat:service-catalog @type:regression @rule:update @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Update a service catalog entry as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "updKey"
    And I generate a unique value and store it as "updName"
    When I create a service catalog entry named "{{updName}}" version "v1" key "{{updKey}}" from definition "artifacts/service-catalog/definition1.yaml" as "updSvcId"
    Then The value of response field "description" should be "Catalog entry"
    # A successful update returns the SAME id with name/version/serviceKey unchanged and the description replaced.
    When I attempt to update the service catalog entry "updSvcId" named "{{updName}}" version "v1" key "{{updKey}}" with description "Updated catalog entry" from definition "artifacts/service-catalog/definition1.yaml"
    Then The response status code should be 200
    And The value of response field "id" should be "{{updSvcId}}"
    And The value of response field "name" should be "{{updName}}"
    And The value of response field "version" should be "v1"
    And The value of response field "serviceKey" should be "{{updKey}}"
    And The value of response field "description" should be "Updated catalog entry"
    # The change is persisted, not just echoed.
    When I retrieve the service catalog entry "updSvcId"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated catalog entry"
    # Updating an unknown service id is 404 (the id is put in context so the same step covers both cases).
    When I generate a random UUID and store it as "unknownUpdSvcId"
    And I attempt to update the service catalog entry "unknownUpdSvcId" named "{{updName}}" version "v1" key "{{updKey}}" with description "Updated catalog entry" from definition "artifacts/service-catalog/definition1.yaml"
    Then The response status code should be 404
    # An update carrying no definition file is 400.
    When I attempt to update the service catalog entry "updSvcId" named "{{updName}}" version "v1" key "{{updKey}}" with description "Updated catalog entry" without a definition
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports testExportService. The export addresses the service by NAME + VERSION (not id) and returns a zipped
  # archive; the step proves it is a real, complete archive (non-empty, unzips, carries both metadata and
  # definition) rather than merely a 200. A wrong name with an existing version is 404.
  @cap:publisher @feat:service-catalog @type:regression @rule:export @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Export a service catalog entry as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "expKey"
    And I generate a unique value and store it as "expName"
    When I create a service catalog entry named "{{expName}}" version "v1" key "{{expKey}}" from definition "artifacts/service-catalog/definition1.yaml" as "expSvcId"
    When I export the service catalog entry named "{{expName}}" version "v1" expecting status 200
    # A name that matches no service is 404 even though the version exists.
    When I export the service catalog entry named "{{expName}}-absent" version "v1" expecting status 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports testCreateAnAPIThroughPublisher + testGetServiceUsage + testDeleteService as ONE ordered arc, because each
  # step's expectation only holds in a particular state: create service -> create an API bound to it via serviceInfo
  # -> usage lists exactly that API -> deleting the service is 409 WHILE the API exists -> delete the API -> deleting
  # the service is now 204. Splitting these across scenarios would make the 409 and the 204 depend on another
  # scenario's artifacts (forbidden) — the arc is self-contained and asserts unconditionally, unlike legacy, which
  # guarded each body with `if (!serviceIdOne.equals(""))` and so no-opped whenever an earlier step had failed.
  @cap:publisher @feat:service-catalog @type:regression @rule:usage @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: A service consumed by an API is listed in its usage and cannot be deleted until the API is gone as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "useKey"
    And I generate a unique value and store it as "useName"
    When I create a service catalog entry named "{{useName}}" version "v1" key "{{useKey}}" from definition "artifacts/service-catalog/definition1.yaml" as "useSvcId"
    # An API whose serviceInfo references the catalog entry: the created API echoes the binding back.
    When I put JSON payload from file "artifacts/payloads/create_apim_service_catalog_api.json" in context as "svcApiPayload"
    And I add service catalog entry "useSvcId" as serviceInfo to the payload "svcApiPayload"
    And I create an "apis" resource with payload "svcApiPayload" as "svcApiId"
    Then The value of response field "serviceInfo.name" should be "{{useName}}"
    And The value of response field "serviceInfo.key" should be "{{useKey}}"
    And I extract response field "name" and store it as "svcApiName"
    # The service's usage is EXACTLY that one API.
    When I retrieve the usage of service catalog entry "useSvcId"
    Then The response status code should be 200
    And The response list should have 1 entries
    And The value of response field "$.list[0].name" should be "{{svcApiName}}"
    # While the API is using it, deleting the service is a 409 conflict.
    When I delete the service catalog entry "useSvcId"
    Then The response status code should be 409
    # The service is still there — the rejected delete changed nothing.
    When I retrieve the service catalog entry "useSvcId"
    Then The response status code should be 200
    # Remove the consumer, and the same delete now succeeds with 204.
    When I delete the "apis" resource with id "svcApiId"
    Then The response status code should be 200
    When I delete the service catalog entry "useSvcId"
    Then The response status code should be 204
    # Usage and delete of an unknown service id are both 404.
    When I generate a random UUID and store it as "unknownUseSvcId"
    And I retrieve the usage of service catalog entry "unknownUseSvcId"
    Then The response status code should be 404
    When I delete the service catalog entry "unknownUseSvcId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the version-search and sort dimensions of testSearchService. The catalog is tenant-global, so both are
  # scoped to this scenario's own entries: a scenario-unique VERSION makes the version search return exactly one
  # row, and a scenario-unique name prefix scopes the sorted search so the returned ORDER is deterministic (a bare
  # sorted search would interleave other scenarios' services and could assert nothing exact).
  @cap:publisher @feat:service-catalog @type:regression @rule:sorting @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Search service catalog entries by version and sort them by name as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "srtPrefix"
    And I generate a unique value and store it as "srtVersion"
    When I create a service catalog entry named "{{srtPrefix}}alpha" version "{{srtVersion}}" key "{{srtPrefix}}alpha" from definition "artifacts/service-catalog/definition1.yaml" as "srtSvcAlpha"
    And I create a service catalog entry named "{{srtPrefix}}beta" version "v1" key "{{srtPrefix}}beta" from definition "artifacts/service-catalog/definition1.yaml" as "srtSvcBeta"
    # Search by version: only the alpha entry carries the scenario-unique version.
    When I search service catalog entries by "version" "{{srtVersion}}"
    Then The response status code should be 200
    And The response list should have 1 entries
    And The value of response field "$.list[0].name" should be "{{srtPrefix}}alpha"
    And The value of response field "$.list[0].version" should be "{{srtVersion}}"
    # sortBy=name ascending returns alpha before beta ...
    When I search service catalog entries by name "{{srtPrefix}}" sorted by "name" in "asc" order
    Then The response status code should be 200
    And The response list should have 2 entries
    And The value of response field "$.list[0].name" should be "{{srtPrefix}}alpha"
    And The value of response field "$.list[1].name" should be "{{srtPrefix}}beta"
    # ... and descending reverses exactly that order.
    When I search service catalog entries by name "{{srtPrefix}}" sorted by "name" in "desc" order
    Then The response status code should be 200
    And The response list should have 2 entries
    And The value of response field "$.list[0].name" should be "{{srtPrefix}}beta"
    And The value of response field "$.list[1].name" should be "{{srtPrefix}}alpha"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the invalid-parameter negatives of testSearchService, each pinning its own exact 400 (legacy asserted
  # these inside catch blocks, so a search that wrongly SUCCEEDED would have passed). The rejection happens on
  # parameter validation before any matching, so no fixture service is needed and the search is left unscoped.
  @cap:publisher @feat:service-catalog @type:negative @rule:sorting @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Service catalog search rejects invalid parameters as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # A definitionType outside the allowed enum.
    When I search service catalog entries by "definitionType" "OS3"
    Then The response status code should be 400
    # A sortBy naming a field that cannot be sorted on.
    When I search service catalog entries by name "" sorted by "defType" in "asc" order
    Then The response status code should be 400
    # A misspelled sortOrder.
    When I search service catalog entries by name "" sorted by "name" in "acs" order
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
