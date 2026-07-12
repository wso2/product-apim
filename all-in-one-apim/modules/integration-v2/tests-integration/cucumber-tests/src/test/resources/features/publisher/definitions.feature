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
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:APIImportExportTestCase
  Scenario Outline: An API can be exported to an archive and re-imported as <actor>
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

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
