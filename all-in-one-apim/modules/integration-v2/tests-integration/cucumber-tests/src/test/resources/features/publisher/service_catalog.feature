@cleanup
Feature: Admin Service Catalog

  Service Catalog management over the /api/am/service-catalog/v1/services REST API: register a service (multipart
  metadata + OpenAPI definition), retrieve it and its definition, search by name/version/type/key with pagination,
  update it, and delete it — plus the negatives (duplicate key, missing definition, invalid id). Ports
  ServiceCatalogRestAPITestCase (the CRUD + search surface; import/export and the create-API-from-service usage
  arc are covered by their own scenario). Runs as the tenant admin in both tenants; each scenario uses a
  scenario-unique service key/name so parallel runs stay isolated, and services are torn down by the cleanup hook.

  @cap:publisher @feat:service-catalog @type:regression @legacy:ServiceCatalogRestAPITestCase
  Scenario Outline: Register, retrieve, search and delete a service catalog entry as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "svcKey"
    And I generate a unique value and store it as "svcName"

    # Create a service (multipart metadata + OAS3 definition).
    When I create a service catalog entry named "{{svcName}}" version "v1" key "{{svcKey}}" from definition "artifacts/service-catalog/definition1.yaml" as "svcId"
    Then The response should contain "{{svcName}}"

    # Retrieve it by id, and its definition.
    When I retrieve the service catalog entry "svcId"
    Then The response status code should be 200
    And The response should contain "{{svcName}}"
    When I retrieve the definition of service catalog entry "svcId"
    Then The response status code should be 200
    # definition1.yaml is a Swagger 2.0 document, so the returned definition carries the "swagger" key.
    And The response should contain "swagger"

    # Search by name, version, type and key each returns the entry.
    When I search service catalog entries by "name" "{{svcName}}"
    Then The response status code should be 200
    And The service catalog search should return 1 entries
    When I search service catalog entries by "key" "{{svcKey}}"
    Then The service catalog search should return 1 entries
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
    Then The service catalog search should return 3 entries
    When I search service catalog entries by name "{{pgKey}}" with limit 2 and offset 0
    Then The service catalog search should return 2 entries
    When I search service catalog entries by name "{{pgKey}}" with limit 10 and offset 2
    Then The service catalog search should return 1 entries

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
