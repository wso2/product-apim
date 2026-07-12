@cleanup
Feature: Publisher API Definition Import

  Publisher-plane import of OpenAPI definitions (OAS 2, 3 and 3.1): import the definition to create an
  API, then take it through revision, deploy and publish. Asserts only publisher outcomes — invoking the
  imported API at the gateway is covered by gateway/rest-invocation. Each row is self-contained and torn
  down by the per-scenario cleanup hook.

  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Import an OpenAPI definition and publish it as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "<apiDefinition>" , additional properties from "<additionalProperty>" and create api as "importedApiId"
    Then The response status code should be 201

    When I retrieve the "apis" resource with id "importedApiId"
    Then The response status code should be 200
    And I put the response payload in context as "importedApiPayload"

    When I put the following JSON payload in context as "createRevisionPayload"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "importedApiId" with payload "createRevisionPayload"
    Then The response status code should be 201

    When I deploy revision "revisionId" of "apis" resource "importedApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "importedApiPayload"

    When I publish the "apis" resource with id "importedApiId"
    Then The response status code should be 200
    And The lifecycle status of API "importedApiId" should be "Published"

    Examples:
      | actor                     | apiDefinition                                   | additionalProperty                                     |
      | publisherUser             | artifacts/payloads/OAS/OAS2ApiDefinition.json   | artifacts/payloads/OAS/OAS2AdditionalProperties.json   |
      | publisherUser             | artifacts/payloads/OAS/OAS3ApiDefinition.json   | artifacts/payloads/OAS/OAS3AdditionalProperties.json   |
      | publisherUser             | artifacts/payloads/OAS/OAS3.1ApiDefinition.json | artifacts/payloads/OAS/OAS3.1AdditionalProperties.json |
      | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS2ApiDefinition.json   | artifacts/payloads/OAS/OAS2AdditionalProperties.json   |
      | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS3ApiDefinition.json   | artifacts/payloads/OAS/OAS3AdditionalProperties.json   |
      | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS3.1ApiDefinition.json | artifacts/payloads/OAS/OAS3.1AdditionalProperties.json |

  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: A subscriber-role user cannot create an API to import a definition into as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # Definition UPDATE — replace an existing API's OpenAPI definition (PUT /swagger) and confirm it is reflected.
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Update an API's OpenAPI definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "defUpdateApiPayload"
    And I create an "apis" resource with payload "defUpdateApiPayload" as "defUpdateApiId"
    When I update the swagger of "apis" resource "defUpdateApiId" from file "artifacts/payloads/OAS/oas_v3_update_definition.json"
    Then The response status code should be 200
    When I retrieve the swagger of "apis" resource "defUpdateApiId"
    Then The response status code should be 200
    And The response should contain "paths"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Advance endpoint configs survive a definition update (the OAS carries x-wso2 advance endpoint config).
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Advance endpoint configs are applied via a definition update as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "advCfgApiPayload"
    And I create an "apis" resource with payload "advCfgApiPayload" as "advCfgApiId"
    When I update the swagger of "apis" resource "advCfgApiId" from file "artifacts/payloads/OAS/oas_v3_advance_configs.json"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "advCfgApiId"
    Then The response status code should be 200
    And The response should contain "circuitBreakers"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Unsupported OpenAPI server blocks are stripped on import (the imported definition must not carry them).
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Unsupported server blocks are stripped when importing a definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "artifacts/payloads/OAS/oas_v3_unsupported_servers.json" , additional properties from "artifacts/payloads/OAS/OAS3AdditionalProperties.json" and create api as "unsupSrvApiId"
    Then The response status code should be 201
    When I retrieve the swagger of "apis" resource "unsupSrvApiId"
    Then The response status code should be 200
    And The response should not contain "test-unsupported.com"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An invalid OpenAPI definition (empty resource paths) is reported invalid by validation, rejected on import,
  # and rejected on update — ports the empty-resource-path validate / import / update trio.
  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: An invalid OpenAPI definition is reported invalid by validation as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the openapi definition from file "artifacts/payloads/OAS/oas_v3_invalid.json"
    Then The response status code should be 200
    And The response should contain "\"isValid\":false"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: Importing an invalid OpenAPI definition is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to import openapi definition from "artifacts/payloads/OAS/oas_v3_invalid.json" with additional properties from "artifacts/payloads/OAS/OAS3AdditionalProperties.json"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: Updating with an invalid OpenAPI definition is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "invUpdApiPayload"
    And I create an "apis" resource with payload "invUpdApiPayload" as "invUpdApiId"
    When I update the swagger of "apis" resource "invUpdApiId" from file "artifacts/payloads/OAS/oas_v3_invalid_update.json"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # I4a: import an API from an OpenAPI ARCHIVE (.zip) containing a remote $ref — the reference resolves into the
  # created API's definition. Ports APIM18 testCreateApiWithArchivesWithRemoteReferences.
  @cap:publisher @feat:definitions @rule:archive-import @type:regression @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase
  Scenario Outline: An API can be imported from an OpenAPI archive with remote references as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import api from archive "artifacts/swagger/swagger-archive.zip" with additional properties "artifacts/payloads/archive_additional_properties.json" as "archiveApiId"
    Then The response status code should be 201
    When I retrieve the swagger of "apis" resource "archiveApiId"
    Then The response status code should be 200
    And The response should contain "dataSetList"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # I4b (importing an INCORRECT archive, APIM18 ...WithIncorrectSwagger) is NOT asserted here.
  # verify-first: probed live on 4.7.0 — an archive whose master swagger is misnamed returns **500**
  # ({"code":500,"description":"Error occurred while validating API Definition"}), same as legacy. docs-apim
  # documents only the normal import flow, not this error path, and there is no clean value-added 4xx negative
  # to assert instead (it's a garbage-input path). Per the no-500-enshrinement principle this product
  # robustness issue is documented (increment-2 backlog), not asserted. The archive-import glue + fixture
  # (incorrect-swagger-archive.zip) are kept for when the product returns a clean 400.

  # Resource order in the OpenAPI definition is preserved through update + retrieve: paths declared in the
  # order /*, /post, /list keep that order in the returned swagger. Ports APIM4765ResourceOrderInSwagger.
  # Asserted as an ordering check (robust to server reformatting) rather than matching a verbatim block.
  @cap:publisher @feat:definitions @type:regression @legacy:APIM4765ResourceOrderInSwagger
  Scenario Outline: Resource order in the OpenAPI definition is preserved as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "orderApiPayload"
    And I create an "apis" resource with payload "orderApiPayload" as "orderApiId"
    When I update the swagger of "apis" resource "orderApiId" from file "artifacts/payloads/OAS/ordered_resources_api_oas.json"
    Then The response status code should be 200
    When I retrieve the swagger of "apis" resource "orderApiId"
    Then The response status code should be 200
    And The response should contain "/*" before "/post"
    And The response should contain "/post" before "/list"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # API export/import round-trip: export an API to an archive (GET /apis/export -> zip), delete it, then re-import
  # the archive (POST /apis/import) and confirm it is recreated with the same name. Ports the core archive
  # round-trip of APIImportExportTestCase. Uses a binary download so the zip is not corrupted. Runs as admin:
  # import (POST /apis/import) needs the apim:api_import_export scope the least-privilege publisher role lacks
  # (export/GET works for it, but import/POST returns 401).
  #
  # Metadata fidelity (ports APIImportExportTestCase#testAPIState): the re-imported API must preserve its
  # metadata through the round-trip — its tags, the throttling tiers (Gold/Bronze/Unlimited), the HTTP+HTTPS
  # transports, PUBLIC visibility, and the resource/verb set (GET + DELETE on /customers/{id}). Asserted on the
  # final retrieve of the re-imported API so the same round-trip covers both recreation and metadata parity.
 @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:APIImportExportTestCase
  Scenario Outline: An API can be exported to an archive and re-imported with its metadata preserved as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ieApiPayload"
    And I create an "apis" resource with payload "ieApiPayload" as "ieApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "ieApiName"
    # Export to an archive
    When I export the API "ieApiId" to an archive as "ieArchive"
    # Delete the original (not deployed) so the re-import does not collide on the case-insensitive-unique name
    When I delete the "apis" resource with id "ieApiId"
    Then The response status code should be 200
    # Re-import the archive -> the API is recreated with the same name (the import response is a plain message).
    When I import the exported archive "ieArchive" with additional properties "{}" as "ieImportResult"
    Then The response status code should be 200
    # Locate the recreated API by name (also registers it for teardown) and confirm the round-trip.
    When I find the Publisher API named "{{ieApiName}}" and store its id as "ieImportedApiId"
    Then The response status code should be 200
    And The response should contain "{{ieApiName}}"
    When I retrieve the "apis" resource with id "ieImportedApiId"
    Then The response status code should be 200
    # Metadata is preserved through the round-trip (testAPIState parity).
    And The response should contain "tag18-1"
    And The response should contain "tag18-2"
    And The response should contain "tag18-3"
    And The response should contain "Gold"
    And The response should contain "Bronze"
    And The response should contain "Unlimited"
    And The response should contain "http"
    And The response should contain "https"
    And The response should contain "\"visibility\":\"PUBLIC\""
    And The response should contain "\"verb\":\"GET\""
    And The response should contain "\"verb\":\"DELETE\""
    And The response should contain "/customers/{id}"

    # Post-import UPDATE re-assert (ports APIImportExportTestCase#testAPIUpdate + testAPIStateAfterUpdate):
    # after the round-trip, UPDATE the re-imported API's description, re-revision + deploy it, then confirm the
    # updated description persists AND the metadata parity above is still intact (tags/tiers/transports/
    # visibility/resource set survive the update). This proves the imported artifact is a fully editable,
    # re-deployable API — not a frozen import.
    When I put the response payload in context as "ieImportedApiPayload"
    And I update the "apis" resource "ieImportedApiId" and "ieImportedApiPayload" with configuration type "description" and value:
    """
    "Updated description after import round-trip"
    """
    Then The response status code should be 200
    When I deploy the API with id "ieImportedApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "ieImportedApiId"
    Then The response status code should be 200
    And The response should contain "Updated description after import round-trip"
    # Metadata parity still holds after the update.
    And The response should contain "tag18-1"
    And The response should contain "Gold"
    And The response should contain "\"visibility\":\"PUBLIC\""
    And The response should contain "\"verb\":\"GET\""
    And The response should contain "/customers/{id}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Secret stripping on export (HIGH value): an API with basic-auth endpoint security is exported and the exported
  # api.json must NOT carry the production/sandbox endpoint-security PASSWORDS in plain text — both are blanked.
  # Ports APIImportExportTestCase#testAPIExport. Export is a GET, so a least-privilege publisher user suffices.
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:APIImportExportTestCase
  Scenario Outline: Exporting an API strips endpoint-security passwords as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api_endpoint_security.json" in context as "epSecPayload"
    And I create an "apis" resource with payload "epSecPayload" as "epSecApiId"
    Then The response status code should be 201
    When I export the API "epSecApiId" to an archive as "epSecArchive"
    Then The exported API archive "epSecArchive" should have empty endpoint-security passwords

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # preserveProvider=true: a round-trip (export -> delete -> re-import with ?preserveProvider=true by the SAME
  # provider) keeps the API's original provider. Ports APIImportExportTestCase#testPreserveProviderTrue...Import.
  # Runs as admin (import needs apim:api_import_export). ×2 tenant proves the provider (incl. the tenant-qualified
  # admin@tenant1.com) is preserved verbatim.
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:APIImportExportTestCase
  Scenario Outline: preserveProvider=true keeps the original provider on re-import as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ppTrueApiPayload"
    And I create an "apis" resource with payload "ppTrueApiPayload" as "ppTrueApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "ppTrueApiName"
    When I export the API "ppTrueApiId" to an archive as "ppTrueArchive"
    When I delete the "apis" resource with id "ppTrueApiId"
    Then The response status code should be 200
    When I import the exported archive "ppTrueArchive" with additional properties "{}" and preserveProvider "true" as "ppTrueImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{ppTrueApiName}}" and store its id as "ppTrueImportedApiId"
    Then The response status code should be 200
    And The provider of API "ppTrueImportedApiId" should match actor "<actor>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # preserveProvider=false: when a DIFFERENT publisher re-imports the archive with ?preserveProvider=false, the
  # imported API is re-owned by the importer. Ports APIImportExportTestCase#testPreserveProviderFalse...Import.
  # A second admin-capable user is provisioned inline (via the existing TenantUserProvisioner) to act as the
  # distinct importer — the framework ships only one admin per tenant, so the "different importer" is created here.
  @cap:publisher @feat:definitions @rule:import-export @type:regression @dep:admin @legacy:APIImportExportTestCase
  Scenario Outline: preserveProvider=false re-owns the API to the importing publisher as <actor>
    Given The system is ready
    And I have valid access tokens as "<owner>"
    And I provision user "<importerKey>" with roles "admin" in tenant "<tenant>"
    And I have valid access tokens as "<importer>"
    # Author + export the API as the original owner.
    And I act as "<owner>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ppFalseApiPayload"
    And I create an "apis" resource with payload "ppFalseApiPayload" as "ppFalseApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "ppFalseApiName"
    When I export the API "ppFalseApiId" to an archive as "ppFalseArchive"
    When I delete the "apis" resource with id "ppFalseApiId"
    Then The response status code should be 200
    # Re-import as the DIFFERENT importer with preserveProvider=false -> the API is re-owned by the importer.
    When I act as "<importer>"
    And I import the exported archive "ppFalseArchive" with additional properties "{}" and preserveProvider "false" as "ppFalseImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{ppFalseApiName}}" and store its id as "ppFalseImportedApiId"
    Then The response status code should be 200
    And The provider of API "ppFalseImportedApiId" should match actor "<importer>"

    Examples:
      | owner             | importerKey       | tenant       | importer                    |
      | admin             | ppImporter        | carbon.super | ppImporter                  |
      | admin@tenant1.com | ppImporter        | tenant1.com  | ppImporter@tenant1.com      |

  # Thumbnail I/E: a thumbnail uploaded onto an API survives an export -> delete -> import round-trip
  # (hasThumbnail stays true on the re-imported API). Ports createAPIWithThumb + testAPIImportWithThumb.
  # Runs as admin (import needs apim:api_import_export).
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:APIImportExportTestCase
  Scenario Outline: An API's thumbnail survives an export/import round-trip as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "thumbApiPayload"
    And I create an "apis" resource with payload "thumbApiPayload" as "thumbApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "thumbApiName"
    # The thumbnail PUT (updateAPIThumbnail) returns 201 Created (verified live on 4.7.0), not 200.
    When I upload thumbnail "artifacts/images/thumbnail.png" for API "thumbApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "thumbApiId"
    Then The response should contain "\"hasThumbnail\":true"
    When I export the API "thumbApiId" to an archive as "thumbArchive"
    When I delete the "apis" resource with id "thumbApiId"
    Then The response status code should be 200
    When I import the exported archive "thumbArchive" with additional properties "{}" as "thumbImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{thumbApiName}}" and store its id as "thumbImportedApiId"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "thumbImportedApiId"
    Then The response should contain "\"hasThumbnail\":true"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Restricted visibility + endpoint preserved through import (ports APIImportExportTestCase#testNewAPIState):
  # an API created with RESTRICTED visibility (visibleRoles=[admin]) and a specific production endpoint URL
  # survives an export -> delete -> import round-trip with BOTH preserved — the re-imported API is still
  # RESTRICTED to the admin role and still routes to the original endpoint URL. This is distinct from the
  # preserveProvider scenarios above (which assert only the provider) — here the visibility/role restriction
  # and the endpoint config are the metadata under test. Runs as admin (import needs apim:api_import_export).
 @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:APIImportExportTestCase
  Scenario Outline: A restricted-visibility API preserves its visibility and endpoint through an export/import round-trip as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_restricted_visibility_api.json" in context as "rvApiPayload"
    And I create an "apis" resource with payload "rvApiPayload" as "rvApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "rvApiName"
    When I export the API "rvApiId" to an archive as "rvArchive"
    When I delete the "apis" resource with id "rvApiId"
    Then The response status code should be 200
    When I import the exported archive "rvArchive" with additional properties "{}" as "rvImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{rvApiName}}" and store its id as "rvImportedApiId"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "rvImportedApiId"
    Then The response status code should be 200
    # Visibility restriction preserved: RESTRICTED to the admin role.
    And The response should contain "\"visibility\":\"RESTRICTED\""
    And The response should contain "admin"
    # Endpoint config preserved: the original production endpoint URL survives the round-trip.
    And The response should contain "nodebackend:3001/jaxrs_basic/services/customers/customerservice"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Restricted (role-based access-control) API export authorization: an API whose access is restricted to a role
  # can be exported by a user WITH that role and by the admin, but a publisher-scoped user WITHOUT the role gets
  # 401. Ports APIImportExportTestCase#testRestrictedAPIExportFrom{UserWithAccessRole,UserWithoutAccessRole,
  # AdminUser}. A custom role + two publisher users (one carrying the role, one not) are provisioned inline via
  # the existing TenantUserProvisioner.
  @cap:publisher @feat:definitions @rule:import-export @type:negative @dep:admin @legacy:APIImportExportTestCase
  # The access role is lowercase: APIM stores/validates accessControlRoles case-folded (the legacy asserts the
  # stored role is lowercase), so a mixed-case role fails the accessControlRoles validation with a 400.
  Scenario Outline: Restricted-API export is allowed only for users with the access role as <actor>
    Given The system is ready
    And I have valid access tokens as "<owner>"
    And I provision role "<role>" in tenant "<tenant>"
    And I provision user "<withKey>" with roles "Internal/creator,Internal/publisher,<role>" in tenant "<tenant>"
    And I provision user "<withoutKey>" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<with>"
    And The system is ready and I have valid publisher access tokens as "<without>"
    # Author the API as the admin owner, then restrict it to <role> in a SINGLE update that sets BOTH
    # accessControl=RESTRICTED and accessControlRoles (restricting with no roles is a 400 "disallowed values",
    # so the two fields must be applied together). accessControl is an uppercase enum in the v4 REST API
    # (NONE/RESTRICTED) — the legacy's lowercase "restricted" was the old API and is rejected here.
    And I act as "<owner>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "raApiPayload"
    And I create an "apis" resource with payload "raApiPayload" as "raApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "raApiId"
    And I put the response payload in context as "raApiFullPayload"
    And I set the field "accessControl" to "RESTRICTED" in the payload "raApiFullPayload"
    And I update the "apis" resource "raApiId" and "raApiFullPayload" with configuration type "accessControlRoles" and value:
    """
    ["<role>"]
    """
    Then The response status code should be 200
    # Export succeeds for the admin owner.
    When I act as "<owner>"
    And I export the API "raApiId" to an archive as "raAdminArchive"
    # Export succeeds for a user WITH the access role.
    When I act as "<with>"
    And I export the API "raApiId" to an archive as "raWithArchive"
    # Export is rejected for a publisher user WITHOUT the access role. 4.7.0 returns 403 Forbidden (the user is
    # authenticated but lacks the role-restricted access), verified live — the legacy asserted 401, which predates
    # this stricter, more correct status.
    When I act as "<without>"
    And I attempt to export the API "raApiId" to an archive expecting status 403

    Examples:
      | owner             | role          | tenant       | withKey     | withoutKey     | with                     | without                     |
      | admin             | apiexportrole | carbon.super | raWith      | raWithout      | raWith                   | raWithout                   |
      | admin@tenant1.com | apiexportrole | tenant1.com  | raWith      | raWithout      | raWith@tenant1.com       | raWithout@tenant1.com       |

  # Cross-tenant import: an API archive exported by ONE tenant's admin can be imported by ANOTHER tenant's admin,
  # landing in the importing tenant owned by the importing admin (preserveProvider=false). Exercises API
  # portability ACROSS tenants — beyond the same-tenant / different-user preserveProvider matrix above. Runs both
  # directions (super->tenant1 and tenant1->super). Import needs apim:api_import_export (admin actors carry it).
  @cap:publisher @feat:definitions @rule:import-export @type:regression @dep:admin @legacy:APIImportExportTestCase
  Scenario Outline: An API archive exported in one tenant is importable into another tenant as <targetActor>
    Given The system is ready
    And I have valid access tokens as "<sourceActor>"
    And I have valid access tokens as "<targetActor>"

    # Author + export the API as the SOURCE tenant admin.
    And I act as "<sourceActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ctApiId" and deployed it
    When I retrieve the "apis" resource with id "ctApiId"
    And I extract response field "name" and store it as "ctApiName"
    When I export the API "ctApiId" to an archive as "ctArchive"

    # Import the archive as the TARGET tenant admin -> the API lands in the target tenant, owned by the target admin.
    When I act as "<targetActor>"
    And I import the exported archive "ctArchive" with additional properties "{}" and preserveProvider "false" as "ctImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{ctApiName}}" and store its id as "ctImportedApiId"
    Then The response status code should be 200
    And The provider of API "ctImportedApiId" should match actor "<targetActor>"

    Examples:
      | sourceActor       | targetActor       |
      | admin             | admin@tenant1.com |
      | admin@tenant1.com | admin             |
