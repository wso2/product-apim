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

    # Definition FIDELITY after import (ports the OASTestCase import assertions, which compared the served
    # definitions against the imported original and against the API DTO field by field — a "contains paths" grep
    # cannot tell a faithful import from one that silently dropped or invented a resource):
    #   - the publisher copy declares exactly the API DTO's operations (count, path+verb, auth type, tier, scopes);
    #   - it carries the WSO2 management extensions consistent with the DTO;
    #   - the devportal copy declares the same resource surface but with every publisher-only extension stripped
    #     (the backend endpoint URLs travel in those extensions, so this is an information-disclosure boundary);
    #   - the imported original and the stored publisher copy declare the same resource surface;
    #   - the definition the product serves is itself parseable by the product's own validator.
    When I retrieve the swagger of "apis" resource "importedApiId"
    Then The response status code should be 200
    And I put the response payload in context as "importedPublisherDef"
    And The definition stored as "importedPublisherDef" should declare exactly the operations of API "importedApiId"
    And The definition stored as "importedPublisherDef" should carry the publisher extensions of API "importedApiId"

    When I retrieve the devportal swagger of API "importedApiId"
    Then The response status code should be 200
    And I put the response payload in context as "importedDevportalDef"
    And The definition stored as "importedDevportalDef" should not expose the publisher-only extensions carried by "importedPublisherDef"
    And The definitions stored as "importedPublisherDef" and "importedDevportalDef" should declare the same operations

    When I put JSON payload from file "<apiDefinition>" in context as "importedOriginalDef"
    Then The definitions stored as "importedOriginalDef" and "importedPublisherDef" should declare the same operations
    And The definition stored as "importedPublisherDef" should be reported valid by the definition validator

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

  # Definition UPDATE — replace an existing API's OpenAPI definition (PUT /swagger) and confirm it is reflected in
  # ALL THREE places legacy OASTestCase#testAPIDefinitionUpdate checked: the PUT's own response body, the publisher
  # GET and the devportal GET, each consistent with the definition that was submitted. Run for an OAS 3 and an
  # OAS 2 definition (legacy ran every OAS case in both versions; the v2 port had only OAS-3 fixtures).
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Update an API's OpenAPI definition as <actor> with <oasVersion>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "defUpdateApiPayload"
    And I create an "apis" resource with payload "defUpdateApiPayload" as "defUpdateApiId"
    And I put JSON payload from file "<definition>" in context as "defUpdateSubmittedDef"
    # The PUT answers with the updated definition — assert that body, not merely its status.
    When I update the swagger of "apis" resource "defUpdateApiId" from file "<definition>"
    Then The response status code should be 200
    And I put the response payload in context as "defUpdatePutBody"
    And The definitions stored as "defUpdateSubmittedDef" and "defUpdatePutBody" should declare the same operations

    When I retrieve the swagger of "apis" resource "defUpdateApiId"
    Then The response status code should be 200
    And I put the response payload in context as "defUpdatePublisherDef"
    And The definitions stored as "defUpdateSubmittedDef" and "defUpdatePublisherDef" should declare the same operations
    And The definition stored as "defUpdatePublisherDef" should declare exactly the operations of API "defUpdateApiId"
    And The definition stored as "defUpdatePublisherDef" should carry the publisher extensions of API "defUpdateApiId"
    And The definition stored as "defUpdatePublisherDef" should be reported valid by the definition validator

    # The devportal serves its own copy of the definition; legacy asserted that plane too. Publishing is enough to
    # expose it — the devportal falls back to an existing gateway environment when the API has no deployment.
    When I publish the "apis" resource with id "defUpdateApiId"
    Then The lifecycle status of API "defUpdateApiId" should be "Published"
    When I retrieve the devportal swagger of API "defUpdateApiId"
    Then The response status code should be 200
    And I put the response payload in context as "defUpdateDevportalDef"
    And The definitions stored as "defUpdateSubmittedDef" and "defUpdateDevportalDef" should declare the same operations
    And The definition stored as "defUpdateDevportalDef" should not expose the publisher-only extensions carried by "defUpdatePublisherDef"

    Examples:
      | actor                     | oasVersion | definition                                             |
      | publisherUser             | OAS 3      | artifacts/payloads/OAS/oas_v3_update_definition.json   |
      | publisherUser             | OAS 2      | artifacts/payloads/OAS/oas_v2_update_definition.json   |
      | publisherUser@tenant1.com | OAS 3      | artifacts/payloads/OAS/oas_v3_update_definition.json   |
      | publisherUser@tenant1.com | OAS 2      | artifacts/payloads/OAS/oas_v2_update_definition.json   |

  # Two legacy methods in one arc, in legacy's own order (create+publish, then DTO update):
  #   - testNewAPI: the definition the product GENERATES for a payload-created API is itself valid and matches the
  #     API DTO's operations in BOTH planes;
  #   - testAPIUpdate: changing the resource set through PUT /apis/{id} (a DTO update, NOT a swagger PUT) must
  #     REGENERATE the definition to match, again in both planes.
  # The v2 port had neither the DTO direction nor any operation-level fidelity or validity round trip.
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Updating an API's operations through its DTO regenerates its OpenAPI definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "dtoUpdApiPayload"
    And I create an "apis" resource with payload "dtoUpdApiPayload" as "dtoUpdApiId"
    Then The response status code should be 201
    And I put the response payload in context as "dtoUpdApiDto"

    # The generated definition of the freshly created API (testNewAPI).
    When I retrieve the swagger of "apis" resource "dtoUpdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "dtoUpdGeneratedDef"
    And The definition stored as "dtoUpdGeneratedDef" should declare exactly the operations of API "dtoUpdApiId"
    And The definition stored as "dtoUpdGeneratedDef" should carry the publisher extensions of API "dtoUpdApiId"
    And The definition stored as "dtoUpdGeneratedDef" should be reported valid by the definition validator

    When I publish the "apis" resource with id "dtoUpdApiId"
    Then The lifecycle status of API "dtoUpdApiId" should be "Published"
    When I retrieve the devportal swagger of API "dtoUpdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "dtoUpdGeneratedStoreDef"
    And The definitions stored as "dtoUpdGeneratedDef" and "dtoUpdGeneratedStoreDef" should declare the same operations
    And The definition stored as "dtoUpdGeneratedStoreDef" should not expose the publisher-only extensions carried by "dtoUpdGeneratedDef"

    # Replace the created API's /customers/{id} GET+DELETE with three /orders operations, through the DTO.
    When I replace the operations of the API payload "dtoUpdApiDto" with the operations from file "artifacts/payloads/OAS/dto_update_operations.json"
    And I update "apis" resource of id "dtoUpdApiId" with payload "dtoUpdApiDto"
    Then The response status code should be 200

    When I retrieve the swagger of "apis" resource "dtoUpdApiId"
    Then The response status code should be 200
    And The response should contain "/orders"
    And The response should not contain "/customers/{id}"
    And I put the response payload in context as "dtoUpdPublisherDef"
    And The definition stored as "dtoUpdPublisherDef" should declare exactly the operations of API "dtoUpdApiId"
    And The definition stored as "dtoUpdPublisherDef" should carry the publisher extensions of API "dtoUpdApiId"
    And The definition stored as "dtoUpdPublisherDef" should be reported valid by the definition validator

    # Still Published from above — the devportal must now serve the REGENERATED definition.
    When I retrieve the devportal swagger of API "dtoUpdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "dtoUpdDevportalDef"
    And The definitions stored as "dtoUpdPublisherDef" and "dtoUpdDevportalDef" should declare the same operations
    And The definition stored as "dtoUpdDevportalDef" should not expose the publisher-only extensions carried by "dtoUpdPublisherDef"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Resource modification via a swagger update REPLACES the API's resource set: the base test API (resources on
  # /customers/{id}) is updated with a definition whose resources are /pets, /pets/{petId} and /oldpets, and the
  # retrieved swagger reflects the NEW resources while the old /customers resource is gone. Ports
  # APIResourceModificationTestCase (which asserts the updated swagger differs from the original after a resource
  # edit) — pinned here as the concrete new-resource set rather than a bare inequality check.
  @cap:publisher @feat:definitions @rule:resource-modification @type:regression @legacy:APIResourceModificationTestCase
  Scenario Outline: Modifying an API's resources via a swagger update replaces its resource set as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "resModApiPayload"
    And I create an "apis" resource with payload "resModApiPayload" as "resModApiId"
    # Baseline: the created API's swagger carries the /customers/{id} resource.
    When I retrieve the swagger of "apis" resource "resModApiId"
    Then The response status code should be 200
    And The response should contain "/customers/{id}"
    # Modify the resources via a swagger update.
    When I update the swagger of "apis" resource "resModApiId" from file "artifacts/payloads/OAS/oas_v3_update_definition.json"
    Then The response status code should be 200
    When I retrieve the swagger of "apis" resource "resModApiId"
    Then The response status code should be 200
    # The new resources are present and the original /customers resource has been replaced.
    And The response should contain "/pets"
    And The response should contain "/oldpets"
    And The response should not contain "/customers/{id}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Advance endpoint configs survive a definition update (the OAS carries x-wso2 advance endpoint config). The
  # circuit-breaker VALUES and their per-environment split are what legacy
  # OASTestCase#testAddAdvanceConfigsToAPIDefinition pinned (sandbox 4/2048/100/25/2048, production
  # 3/1024/75/35/1024, taken from the two x-wso2-*-endpoints extensions of the submitted definition); a bare
  # "contains circuitBreakers" would pass even if the two environments' configs were swapped or defaulted.
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Advance endpoint configs are applied via a definition update as <actor> with <oasVersion>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "advCfgApiPayload"
    And I create an "apis" resource with payload "advCfgApiPayload" as "advCfgApiId"
    When I update the swagger of "apis" resource "advCfgApiId" from file "<definition>"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "advCfgApiId"
    Then The response status code should be 200
    And The value of response field "endpointConfig.production_endpoints.advanceEndpointConfig.circuitBreakers.maxRetries" should be "3"
    And The value of response field "endpointConfig.production_endpoints.advanceEndpointConfig.circuitBreakers.maxRequests" should be "75"
    And The value of response field "endpointConfig.production_endpoints.advanceEndpointConfig.circuitBreakers.maxPendingRequests" should be "35"
    And Each of the response fields "endpointConfig.production_endpoints.advanceEndpointConfig.circuitBreakers.maxConnectionPools,endpointConfig.production_endpoints.advanceEndpointConfig.circuitBreakers.maxConnections" should be "1024"
    And The value of response field "endpointConfig.sandbox_endpoints.advanceEndpointConfig.circuitBreakers.maxRetries" should be "4"
    And The value of response field "endpointConfig.sandbox_endpoints.advanceEndpointConfig.circuitBreakers.maxRequests" should be "100"
    And The value of response field "endpointConfig.sandbox_endpoints.advanceEndpointConfig.circuitBreakers.maxPendingRequests" should be "25"
    And Each of the response fields "endpointConfig.sandbox_endpoints.advanceEndpointConfig.circuitBreakers.maxConnectionPools,endpointConfig.sandbox_endpoints.advanceEndpointConfig.circuitBreakers.maxConnections" should be "2048"

    Examples:
      | actor                     | oasVersion | definition                                            |
      | publisherUser             | OAS 3      | artifacts/payloads/OAS/oas_v3_advance_configs.json    |
      | publisherUser             | OAS 2      | artifacts/payloads/OAS/oas_v2_advance_configs.json    |
      | publisherUser@tenant1.com | OAS 3      | artifacts/payloads/OAS/oas_v3_advance_configs.json    |
      | publisherUser@tenant1.com | OAS 2      | artifacts/payloads/OAS/oas_v2_advance_configs.json    |

  # Unsupported OpenAPI server blocks are stripped on import — legacy asserted BOTH planes (publisher AND store),
  # so the devportal copy is checked here too: it is the plane a consumer's try-out console reads, where a stray
  # unsupported server URL would actually be dialled.
  #
  # The OAS-2 variant of legacy testAPIDefinitionWithUnsupportedServerBlocksImport is NOT ported: its entire body
  # is wrapped in `if (oasVersion.equals(OAS_V3))` (OASTestCase.java:220), so the OAS_V2 instances of that method
  # execute no assertions at all. There is nothing to port, and no OAS-2 fixture is invented to fake it.
  @cap:publisher @feat:definitions @type:regression @legacy:OASTestCase
  Scenario Outline: Unsupported server blocks are stripped when importing a definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "artifacts/payloads/OAS/oas_v3_unsupported_servers.json" , additional properties from "artifacts/payloads/OAS/OAS3AdditionalProperties.json" and create api as "unsupSrvApiId"
    Then The response status code should be 201
    When I retrieve the swagger of "apis" resource "unsupSrvApiId"
    Then The response status code should be 200
    And The response should not contain "test-unsupported.com"

    When I publish the "apis" resource with id "unsupSrvApiId"
    Then The lifecycle status of API "unsupSrvApiId" should be "Published"
    When I retrieve the devportal swagger of API "unsupSrvApiId"
    Then The response status code should be 200
    And The response should not contain "test-unsupported.com"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An invalid OpenAPI definition (empty resource paths) is reported invalid by validation, rejected on import,
  # and rejected on update — ports the empty-resource-path validate / import / update trio.
  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: An invalid OpenAPI definition is reported invalid by validation as <actor> with <oasVersion>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the openapi definition from file "<definition>"
    Then The response status code should be 200
    And The value of response field "isValid" should be "false"

    Examples:
      | actor                     | oasVersion | definition                                  |
      | publisherUser             | OAS 3      | artifacts/payloads/OAS/oas_v3_invalid.json  |
      | publisherUser             | OAS 2      | artifacts/payloads/OAS/oas_v2_invalid.json  |
      | publisherUser@tenant1.com | OAS 3      | artifacts/payloads/OAS/oas_v3_invalid.json  |
      | publisherUser@tenant1.com | OAS 2      | artifacts/payloads/OAS/oas_v2_invalid.json  |

  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: Importing an invalid OpenAPI definition is rejected as <actor> with <oasVersion>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to import openapi definition from "<definition>" with additional properties from "<additionalProperty>"
    Then The response status code should be 400

    Examples:
      | actor                     | oasVersion | definition                                 | additionalProperty                                   |
      | publisherUser             | OAS 3      | artifacts/payloads/OAS/oas_v3_invalid.json | artifacts/payloads/OAS/OAS3AdditionalProperties.json |
      | publisherUser             | OAS 2      | artifacts/payloads/OAS/oas_v2_invalid.json | artifacts/payloads/OAS/OAS2AdditionalProperties.json |
      | publisherUser@tenant1.com | OAS 3      | artifacts/payloads/OAS/oas_v3_invalid.json | artifacts/payloads/OAS/OAS3AdditionalProperties.json |
      | publisherUser@tenant1.com | OAS 2      | artifacts/payloads/OAS/oas_v2_invalid.json | artifacts/payloads/OAS/OAS2AdditionalProperties.json |

  # A rejected definition update must also be a NO-OP: legacy testAPIDefinitionUpdateWithEmptyResourcePath
  # re-read the definition after the 400 and asserted it still matched the last good one. Without that read-back a
  # product that rejected the update but corrupted the stored definition on the way out would still pass.
  @cap:publisher @feat:definitions @type:negative @legacy:OASTestCase
  Scenario Outline: Updating with an invalid OpenAPI definition is rejected and leaves the stored definition intact as <actor> with <oasVersion>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "invUpdApiPayload"
    And I create an "apis" resource with payload "invUpdApiPayload" as "invUpdApiId"
    When I retrieve the swagger of "apis" resource "invUpdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "invUpdDefBefore"
    When I update the swagger of "apis" resource "invUpdApiId" from file "<definition>"
    Then The response status code should be 400
    When I retrieve the swagger of "apis" resource "invUpdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "invUpdDefAfter"
    And The definitions stored as "invUpdDefBefore" and "invUpdDefAfter" should declare the same operations
    And The definition stored as "invUpdDefAfter" should declare exactly the operations of API "invUpdApiId"

    Examples:
      | actor                     | oasVersion | definition                                        |
      | publisherUser             | OAS 3      | artifacts/payloads/OAS/oas_v3_invalid_update.json |
      | publisherUser             | OAS 2      | artifacts/payloads/OAS/oas_v2_invalid_update.json |
      | publisherUser@tenant1.com | OAS 3      | artifacts/payloads/OAS/oas_v3_invalid_update.json |
      | publisherUser@tenant1.com | OAS 2      | artifacts/payloads/OAS/oas_v2_invalid_update.json |

  # Deletion is confirmed by a READ: legacy APIM18 testRemoveAnAPIThroughThePublisherRest asserted the DELETE's 200
  # AND that a following GET of the same id returns 404. Several v2 scenarios delete an API, but none confirmed the
  # effect — a delete that answers 200 while leaving the API retrievable would have gone unnoticed.
  @cap:publisher @feat:definitions @type:regression @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase
  Scenario Outline: A deleted API can no longer be retrieved as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "delApiPayload"
    And I create an "apis" resource with payload "delApiPayload" as "delApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "delApiId"
    Then The response status code should be 200
    When I delete the "apis" resource with id "delApiId"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "delApiId"
    Then The response status code should be 404

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

  # Importing an INCORRECT archive (ports APIM18...WithIncorrectSwagger). An archive whose master swagger is
  # misnamed is refused, and the CURRENT behaviour is pinned exactly: 500 with
  # "Error occurred while validating API Definition" — the same status and message legacy asserted, verified
  # live on 4.7.0.
  #
  # WHY 500 IS PINNED RATHER THAN LEFT UNASSERTED. An earlier revision of this file deliberately left it
  # uncovered, on the principle of not enshrining a server error as the contract, and parked the fixture until
  # the product returned a clean 4xx. That reasoning framed the choice as "pin 500 vs pin a 4xx" — but the real
  # choice was "pin 500 vs pin NOTHING", and pinning nothing leaves the negative path with no regression
  # protection at all: an import that SUCCEEDED on a malformed archive, silently creating a broken API, would go
  # undetected. That is the regression worth guarding against, so the observable is pinned as it stands.
  #
  # A 4xx is arguably the correct status for what is a caller-input error the product detects and describes
  # (compare the deny-policy negatives, which pin 500 for the same reason). If the status is ever corrected,
  # THIS TEST WILL FAIL — that is the intended signal, not a nuisance: it forces the change to be noticed and
  # the assertion updated deliberately.
  @cap:publisher @feat:definitions @type:negative @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase
  Scenario Outline: Importing an OpenAPI archive whose swagger is incorrect is refused as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import api from archive "artifacts/swagger/incorrect-swagger-archive.zip" with additional properties "artifacts/payloads/archive_additional_properties.json" as "badArchiveApiId"
    # The import must NOT succeed. Asserting the exact status (not merely "not 201") is what makes a future
    # change to a clean 4xx visible instead of silently passing.
    Then The response status code should be 500
    And The response should contain "validating API Definition"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |


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

  # Secret redaction on IMPORT (twin of the export case): importing an OpenAPI definition whose additional
  # properties carry basic-auth backend endpoint security must NOT return the stored password in plaintext on a
  # subsequent retrieve. Ports the OAS-import-redaction facet of
  # AddEndPointSecurityPerTypeTestCase#testAPIDefinitionImportWithEndpointSecurity.
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Importing an OpenAPI definition with endpoint security redacts the stored password as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "artifacts/payloads/OAS/OAS3ApiDefinition.json" , additional properties from "artifacts/payloads/OAS/OAS3EndpointSecurityProps.json" and create api as "epImportApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "epImportApiId"
    Then The response status code should be 200
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "BASIC"
    And The value of response field "endpointConfig.endpoint_security.production.username" should be "importUser"
    And The value of response field "endpointConfig.endpoint_security.production.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.production.password" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "BASIC"
    And The value of response field "endpointConfig.endpoint_security.sandbox.username" should be "importUser"
    And The value of response field "endpointConfig.endpoint_security.sandbox.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.sandbox.password" should be ""
    And The response should not contain "importSecret123"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # OAUTH endpoint security on IMPORT: the BASIC case above only proves a password is not echoed back, which a
  # server that DROPPED the whole endpoint_security block would also satisfy. This asserts the positive round-trip
  # for the OAUTH type — type, grantType, tokenUrl and clientId all survive the OAS import for BOTH key types —
  # while the clientSecrets stay redacted, then PUBLISHES the imported API to prove the imported endpoint-security
  # config is a publishable artifact. Ports
  # AddEndPointSecurityPerTypeTestCase#testAPIDefinitionImportWithEndpointSecurity, whose assertions are exactly
  # the per-key-type OAUTH type/tokenUrl/clientId equality plus clientSecret == "" and the publish that follows.
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Importing an OpenAPI definition with OAUTH endpoint security round-trips the config and redacts the client secrets as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "artifacts/payloads/OAS/OAS3ApiDefinition.json" , additional properties from "artifacts/payloads/OAS/OAS3EndpointSecurityOauthProps.json" and create api as "epImportOauthApiId"
    Then The response status code should be 201
    # Non-secret fields round-trip on the import response, per key type.
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "oasImportProdClientId0001"
    And The value of response field "endpointConfig.endpoint_security.production.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.sandbox.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "oasImportSandClientId0002"
    And The value of response field "endpointConfig.endpoint_security.sandbox.enabled" should be "true"
    # The client secrets are blanked — legacy asserts clientSecret == "" exactly, which also proves the field is
    # present rather than dropped (a "should not contain" alone would pass on a stripped field too).
    And The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    And The response should not contain "oasImportProdClientSecret0001"
    And The response should not contain "oasImportSandClientSecret0002"
    # Re-fetch confirms the persisted API keeps the round-tripped config and the redaction.
    When I retrieve the "apis" resource with id "epImportOauthApiId"
    Then The response status code should be 200
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "oasImportProdClientId0001"
    And The value of response field "endpointConfig.endpoint_security.production.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "CLIENT_CREDENTIALS"
    And The value of response field "endpointConfig.endpoint_security.sandbox.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "oasImportSandClientId0002"
    And The value of response field "endpointConfig.endpoint_security.sandbox.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    And The response should not contain "oasImportProdClientSecret0001"
    And The response should not contain "oasImportSandClientSecret0002"
    # The imported API with OAUTH endpoint security is publishable.
    When I publish the "apis" resource with id "epImportOauthApiId"
    Then The lifecycle status of API "epImportOauthApiId" should be "Published"

    # Both the tenant ADMIN and the non-admin publisher rows run: this is an import ROUND-TRIP assertion, not an
    # authz one, and the stored provider differs per actor, so admin is NOT implied by publisherUser passing.
    # (The BASIC-endpoint-security import scenario above is deliberately publisherUser-only — it asserts only that
    # a password is not echoed back, which is actor-independent.)
    Examples:
      | actor                     |
      | admin                     |
      | admin@tenant1.com         |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # OAUTH endpoint security on IMPORT, PASSWORD grant. A sibling of the client_credentials scenario above rather
  # than another row in its Examples, because its assertion set cannot be expressed on a client_credentials row:
  # it pins grantType PASSWORD, the per-key-type resource-owner USERNAME round-trip, and the redaction of the
  # resource-owner PASSWORD. That last one is assertable HERE — and only here — because these import properties
  # carry distinct literal credentials that collide with nothing else in the payload, whereas the gateway
  # password-grant scenario uses the acting actor's own carbon password (see the OAUTH password-grant scenario in
  # gateway/security_enforcement.feature). Together with that scenario this pins the PASSWORD-grant config on BOTH
  # the create path (here) and the update path (there).
  # Ports the PASSWORD-grant half of AddEndPointSecurityPerTypeTestCase#testAPIDefinitionImportWithEndpointSecurity.
  @cap:publisher @feat:definitions @rule:import-export @type:regression @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Importing an OpenAPI definition with PASSWORD-grant OAUTH endpoint security round-trips the config and redacts the secrets as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "artifacts/payloads/OAS/OAS3ApiDefinition.json" , additional properties from "artifacts/payloads/OAS/OAS3EndpointSecurityOauthPasswordProps.json" and create api as "epImportOauthPwApiId"
    Then The response status code should be 201
    # Non-secret fields round-trip on the import response, per key type — including the resource-owner username.
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "PASSWORD"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "oasImportProdClientId0001"
    And The value of response field "endpointConfig.endpoint_security.production.username" should be "oasImportProdRoUser0001"
    And The value of response field "endpointConfig.endpoint_security.production.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "PASSWORD"
    And The value of response field "endpointConfig.endpoint_security.sandbox.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "oasImportSandClientId0002"
    And The value of response field "endpointConfig.endpoint_security.sandbox.username" should be "oasImportSandRoUser0002"
    And The value of response field "endpointConfig.endpoint_security.sandbox.enabled" should be "true"
    # Both the client secrets AND the resource-owner passwords are blanked to "" (the probed 4.7.0 contract), and
    # neither plaintext value appears anywhere in the payload.
    And The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.production.password" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.password" should be ""
    And The response should not contain "oasImportProdClientSecret0001"
    And The response should not contain "oasImportSandClientSecret0002"
    And The response should not contain "oasImportProdRoPass0001"
    And The response should not contain "oasImportSandRoPass0002"
    # Re-fetch confirms the persisted API keeps the round-tripped config and the redaction.
    When I retrieve the "apis" resource with id "epImportOauthPwApiId"
    Then The response status code should be 200
    And The value of response field "endpointConfig.endpoint_security.production.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.production.grantType" should be "PASSWORD"
    And The value of response field "endpointConfig.endpoint_security.production.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.production.clientId" should be "oasImportProdClientId0001"
    And The value of response field "endpointConfig.endpoint_security.sandbox.grantType" should be "PASSWORD"
    And The value of response field "endpointConfig.endpoint_security.production.username" should be "oasImportProdRoUser0001"
    And The value of response field "endpointConfig.endpoint_security.production.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.production.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.production.password" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.type" should be "OAUTH"
    And The value of response field "endpointConfig.endpoint_security.sandbox.tokenUrl" should be "https://localhost:9443/oauth2/token"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientId" should be "oasImportSandClientId0002"
    And The value of response field "endpointConfig.endpoint_security.sandbox.username" should be "oasImportSandRoUser0002"
    And The value of response field "endpointConfig.endpoint_security.sandbox.enabled" should be "true"
    And The value of response field "endpointConfig.endpoint_security.sandbox.clientSecret" should be ""
    And The value of response field "endpointConfig.endpoint_security.sandbox.password" should be ""
    And The response should not contain "oasImportProdClientSecret0001"
    And The response should not contain "oasImportSandClientSecret0002"
    And The response should not contain "oasImportProdRoPass0001"
    And The response should not contain "oasImportSandRoPass0002"
    # The imported API with PASSWORD-grant endpoint security is publishable.
    When I publish the "apis" resource with id "epImportOauthPwApiId"
    Then The lifecycle status of API "epImportOauthPwApiId" should be "Published"

    Examples:
      | actor                     |
      | admin                     |
      | admin@tenant1.com         |
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
  Scenario Outline: preserveProvider=false re-owns the API to the importing publisher as <importer>
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
  Scenario Outline: Restricted-API export is allowed only for users with the access role as <owner>
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
