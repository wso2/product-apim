@cleanup
Feature: Publisher API Lifecycle

  Publisher-plane lifecycle of a REST API: create + deploy, retrieve, update (description & tiers),
  the update-does-not-rename invariant, publish, and presence in the API list. Asserts only publisher
  outcomes — subscription and gateway invocation are covered by devportal/subscribe and
  gateway/rest-invocation. Teardown via the @cleanup hook.

  @cap:publisher @feat:api-lifecycle @type:smoke @legacy:APIMANAGERPublisherTestCase @legacy:APITestCase
  Scenario Outline: Create, update, publish and list a REST API as <actor>
    # Runs as a least-privilege publisher user (creator+publisher, not admin), in both the super tenant
    # and tenant1.com, proving the publisher-plane lifecycle is tenant-agnostic.
    Given The system is ready and I have valid publisher access tokens as "<actor>"

    # Create once, capture the product-returned identity, then verify the same metadata survives a readback.
    Given I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "apiLifecycleCreatePayload"
    And I extract field "name" from json payload "apiLifecycleCreatePayload" and store it as "apiLifecycleCreatedName"
    And I extract field "context" from json payload "apiLifecycleCreatePayload" and store it as "apiLifecycleCreatedContext"
    # API definitions use a leading slash. Publisher readback tenant-qualifies tenant API contexts with /t/<domain>.
    And I set the field "context" to "/{{apiLifecycleCreatedContext}}" in the payload "apiLifecycleCreatePayload"
    And I put literal value "<tenantContextPrefix>/{{apiLifecycleCreatedContext}}" in context as "apiLifecycleExpectedContext"
    When I create an "apis" resource with payload "apiLifecycleCreatePayload" as "createdApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{apiLifecycleCreatedName}}"
    And The value of response field "context" should be "{{apiLifecycleExpectedContext}}"
    And The value of response field "version" should be "1.0.0"
    And The response should contain "lastUpdatedTimestamp"
    And The response field "lastUpdatedTimestamp" should be non-blank
    And I put the response payload in context as "retrievedApiPayload"

    # A freshly created (not-yet-published) API is still CREATED after its revision is deployed.
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And The lifecycle status of API "createdApiId" should be "Created"

    # Update description and tier collection
    When I put JSON payload from file "artifacts/payloads/update_apim_test_api.json" in context as "apiUpdatePayload"
    And I update "apis" resource of id "createdApiId" with payload "apiUpdatePayload"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated description for the created API"
    And The response array field "policies" should have exactly 3 entries
    And The response field "policies[*]" should be exactly the list "Gold,Bronze,Silver"

    # Updating must not rename the API
    When I put JSON payload from file "artifacts/payloads/rename_apim_test_api.json" in context as "apiRenamePayload"
    And I update "apis" resource of id "createdApiId" with payload "apiRenamePayload"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response should not contain "APIMTestRenamed"

    # Publish and confirm it is listed
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "createdApiId" should be in the list of all APIS

    Examples:
      | actor                     | tenantContextPrefix |
      | publisherUser             |                     |
      | publisherUser@tenant1.com | /t/tenant1.com      |

  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIMANAGERPublisherTestCase @legacy:APICreationForTenantsTestCase
  Scenario Outline: A subscriber-role user cannot create a REST API as <actor>
    # A subscriber-only (self-signup-equivalent) user obtains a token, but it lacks the api_create scope, so
    # the Publisher API rejects the create as unauthenticated-for-this-resource (401). Proves publisher-plane
    # role enforcement in both tenants.
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APICreationForTenantsTestCase
  Scenario Outline: A publisher whose custom role lacks API-create permission cannot create an API, provisioned by <provisioner>
    Given The system is ready and I have valid publisher access tokens as "<provisioner>"
    When I provision a user with the API-creator role fixture and store its actor as "apiCreatorActor"
    And The system is ready and I have valid publisher access tokens as "{{apiCreatorActor}}"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "restrictedApiPayload"
    And I attempt to create an "apis" resource with payload "restrictedApiPayload"
    Then The response status code should be 401

    Examples:
      | provisioner        |
      | admin               |
      | admin@tenant1.com   |

  # Create-validation matrix — ports APIM514 (missing mandatory fields), APIMANAGER5834 (invalid context) and
  # APIM519 (no auth). Built from the valid base payload with one field blanked/invalidated per row, so no
  # per-case fixture is needed. This outline carries the clean, deterministic rejections: blank name/context/version
  # and an invalid context all return 400; missing auth returns 401. The remaining APIM514 cases — a missing
  # endpoint configuration, no resources, and a blank tier list — are NOT clean 400s, so each gets its own
  # scenario below that pins the product's actual answer (status AND body) rather than being omitted.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase @legacy:APIMANAGER5834APICreationWithInvalidInputsTestCase @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase
  Scenario Outline: Creating an API with <case> is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "invalidApiPayload"
    And I set the field "<field>" to "<value>" in the payload "invalidApiPayload"
    And I attempt to create an "apis" resource with payload "invalidApiPayload"
    Then The response status code should be 400

    Examples:
      | case                | field   | value    | actor                       |
      | a blank name        | name    |          | publisherUser               |
      | a blank name        | name    |          | publisherUser@tenant1.com   |
      | a blank context     | context |          | publisherUser               |
      | a blank context     | context |          | publisherUser@tenant1.com   |
      | a blank version     | version |          | publisherUser               |
      | a blank version     | version |          | publisherUser@tenant1.com   |
      | an invalid context  | context | /        | publisherUser               |
      | an invalid context  | context | /        | publisherUser@tenant1.com   |
      | a malformed context | context | bad`ctx  | publisherUser               |
      | a malformed context | context | bad`ctx  | publisherUser@tenant1.com   |

  # No-auth is exercised through both publisher URL shapes. The standard endpoint reaches the authentication gate
  # (401); the explicitly tenant-qualified path is not exposed by this management API and is therefore hidden (404).
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase
  Scenario Outline: Creating an API without authentication is rejected in <tenantDomain>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "noAuthApiPayload"
    And I attempt to create an "apis" resource with payload "noAuthApiPayload" without authentication in tenant "<tenantDomain>"
    Then The response status code should be <expectedStatus>

    Examples:
      | actor                     | tenantDomain | expectedStatus |
      | publisherUser             | carbon.super | 401            |
      | publisherUser@tenant1.com | tenant1.com  | 404            |

  # APIM514, remaining cases (1/3) — an API created with NO endpoint configuration at all. Legacy
  # (testCreateAnAPIThroughThePublisherRestWithoutEndpoint) had its @Test annotation and every assertion commented
  # out, so it never ran and asserted nothing; the SCENARIO is nevertheless meaningful on the v4 REST surface, so
  # it is implemented here and the product's actual answer is pinned, together with the resulting observable state.
  @cap:publisher @feat:api-lifecycle @rule:create-validation @type:regression @legacy:APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase
  Scenario Outline: Creating an API with no endpoint configuration as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "noEpApiPayload"
    And I remove the field "endpointConfig" from the payload "noEpApiPayload"
    And I attempt to create an "apis" resource with payload "noEpApiPayload" as "noEpApiId"
    Then The response status code should be 201
    # Observable state: the API exists and carries no endpoint configuration — neither endpoint is invented for it.
    When I retrieve the "apis" resource with id "noEpApiId"
    Then The response status code should be 200
    And The response should not contain "production_endpoints"
    And The response should not contain "sandbox_endpoints"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # APIM514, remaining cases (2/3) — an API created with an EMPTY resource/operation list. Legacy
  # (testCreateAnAPIThroughThePublisherRestWithoutResources) was likewise dead code (no @Test, all assertions
  # commented out) and drove the removed design-API surface with an empty swagger; on v4 the equivalent is an
  # empty `operations` array. Pins the actual answer plus the resulting operation list.
  @cap:publisher @feat:api-lifecycle @rule:create-validation @type:regression @legacy:APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase
  Scenario Outline: Creating an API with no resources as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "noResApiPayload"
    And I set the field "operations" to an empty array in the payload "noResApiPayload"
    And I attempt to create an "apis" resource with payload "noResApiPayload" as "noResApiId"
    Then The response status code should be 201
    # Observable state: what resource set an empty operations array actually yields.
    When I retrieve the "apis" resource with id "noResApiId"
    Then The response status code should be 200
    # An empty operations array yields the default resource set: /* under 5 verbs (measured, not assumed). The
    # count is the load-bearing half — the value set alone passes if only one verb survives.
    And The response array field "operations[*].target" should have exactly 5 entries
    And The response field "operations[*].target" should be exactly the list "/*"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # APIM514, remaining cases (3/3) — a BLANK subscription-tier list. Legacy
  # (testCreateAnAPIThroughThePublisherRestWithoutTierAvailability) blanked tiersCollection+tier and then asserted
  # "either a non-empty id OR a 500", i.e. nothing, and the earlier v2 note omitted the case on the grounds that
  # "legacy asserts a 500 rather than a validation response".
  #
  # VERIFIED LIVE ON 4.7.0: that premise is false today. The create is accepted and the PUBLISH is rejected with a
  # clean 400 carrying a specific error code — 903224, "Failed to publish service to API store. No Tiers selected",
  # naming the API's UUID. So no 500 needed enshrining here: the exact status, code and message are pinned instead,
  # which fails in both directions — if the validation disappears the publish becomes a 200, and if the code or
  # wording changes the assertion catches that too.
  @cap:publisher @feat:api-lifecycle @rule:create-validation @type:negative @legacy:APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase
  Scenario Outline: Publishing an API with a blank subscription tier list is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "noTierApiPayload"
    And I set the field "policies" to an empty array in the payload "noTierApiPayload"
    And I attempt to create an "apis" resource with payload "noTierApiPayload" as "noTierApiId"
    # The CREATE is accepted — the tier list is only required to make the API subscribable.
    Then The response status code should be 201
    And The response field "policies[*]" should be exactly the list ""
    # The PUBLISH is what fails, and it fails as a proper validation response naming the missing tiers.
    When I attempt to change the lifecycle of API "noTierApiId" with action "Publish"
    # Error envelope, not the field-value step: that one refuses a non-2xx body by design.
    Then The response status code should be 400
    And The error response should have code "903224" and message "Failed to publish service to API store. No Tiers selected"
    And The response should contain "No Tiers selected for API with UUID"
    # A rejected transition must not half-apply — the API stays in its pre-publish state.
    And The lifecycle status of API "noTierApiId" should be "Created"

  Examples:
    | actor                     |
    | publisherUser             |
    | publisherUser@tenant1.com |

  # Legacy APIM514 explicitly sent an empty action field, but its assertion accepted either a created API id or an
  # HTTP 500. V2's API create contract rejects the unrecognized field; pin the exact client-error envelope and
  # description so this input cannot silently become accepted or fail for an unrelated reason.
  @cap:publisher @feat:api-lifecycle @rule:create-validation @type:regression @legacy:APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase
  Scenario Outline: Creating an API with an empty action is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "emptyActionPayload"
    And I set the field "action" to "" in the payload "emptyActionPayload"
    And I attempt to create an "apis" resource with payload "emptyActionPayload"
    Then The response status code should be 400
    And The error response should have code "400" message "Bad Request" and description containing "Unrecognized property 'action'"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Table-driven API creation collision matrix. Each row creates one uniquely named/contexted API, then tries the
  # listed identity combination against it. Keeping setup shared makes the differences under test explicit while
  # retaining the established context-mismatch response detail. The exact direct-create duplicate is pinned to
  # HTTP 409; its body is intentionally not guessed from the separate copy-version endpoint contract.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase @legacy:APIMANAGER5834APICreationWithInvalidInputsTestCase
  Scenario Outline: Creating an API with a colliding identity combination is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "collisionBase"
    And I extract field "name" from json payload "collisionBase" and store it as "collisionBaseName"
    And I extract field "context" from json payload "collisionBase" and store it as "collisionBaseContext"
    And I create an "apis" resource with payload "collisionBase" as "collisionBaseId"
    Then The response status code should be 201

    # The candidate starts with independently generated name/context values. The Examples table selects which
    # identity fields to reuse, so each row isolates one collision dimension without repeating the create flow.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "collisionCandidate"
    And I extract field "name" from json payload "collisionCandidate" and store it as "collisionCandidateName"
    And I extract field "context" from json payload "collisionCandidate" and store it as "collisionCandidateContext"
    And I set the field "name" to "<candidateName>" in the payload "collisionCandidate"
    And I set the field "context" to "<candidateContext>" in the payload "collisionCandidate"
    And I set the field "version" to "<candidateVersion>" in the payload "collisionCandidate"
    And I attempt to create an "apis" resource with payload "collisionCandidate"
    Then The response status code should be <expectedStatus>
    And The collision response should contain "<expectedErrorFragment>" when specified

    Examples:
      | actor                     | candidateName                | candidateContext                | candidateVersion | expectedStatus | expectedErrorFragment       |
      | publisherUser             | {{collisionCandidateName}}   | {{collisionBaseContext}}        | 1.0.0            | 409            | -                           |
      | publisherUser             | {{collisionBaseName}}        | {{collisionBaseContext}}        | 1.0.0            | 409            | -                           |
      | publisherUser             | {{collisionBaseName}}        | {{collisionCandidateContext}}   | 2.0.0            | 400            | API Context does not exist |
      | publisherUser@tenant1.com | {{collisionCandidateName}}   | {{collisionBaseContext}}        | 1.0.0            | 409            | -                           |
      | publisherUser@tenant1.com | {{collisionBaseName}}        | {{collisionBaseContext}}        | 1.0.0            | 409            | -                           |
      | publisherUser@tenant1.com | {{collisionBaseName}}        | {{collisionCandidateContext}}   | 2.0.0            | 400            | API Context does not exist |

  # APITestCase's legacy v0.16 requests expected a second same-name API version with a different context to succeed.
  # The current v4 runtime rejects that combination with 400/900977 because versions of the same API must retain
  # the existing context. Pin that current behavior and ensure the rejected create does not alter the first version.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APITestCase
  Scenario Outline: A same-name API version with a different context is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "sameNameFirstPayload"
    And I extract field "name" from json payload "sameNameFirstPayload" and store it as "sameNameExpected"
    And I extract field "context" from json payload "sameNameFirstPayload" and store it as "sameNameFirstContext"
    And I set the field "context" to "/{{sameNameFirstContext}}" in the payload "sameNameFirstPayload"
    And I put literal value "<tenantContextPrefix>/{{sameNameFirstContext}}" in context as "sameNameExpectedContext"
    And I set the field "version" to "2.0.0" in the payload "sameNameFirstPayload"
    And I create an "apis" resource with payload "sameNameFirstPayload" as "sameNameFirstId"
    Then The response status code should be 201

    # A fresh payload supplies a distinct generated context; only the name is reused from the first API.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "sameNameSecondPayload"
    And I set the field "name" to "{{sameNameExpected}}" in the payload "sameNameSecondPayload"
    And I set the field "version" to "3.0.0" in the payload "sameNameSecondPayload"
    And I attempt to create an "apis" resource with payload "sameNameSecondPayload"
    Then The response status code should be 400
    And The error response should have code "900977" message "Block Condition Error" and description containing "API Context does not exist"

    # The failed version create must leave the existing API unchanged.
    When I retrieve the "apis" resource with id "sameNameFirstId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{sameNameExpected}}"
    And The value of response field "context" should be "{{sameNameExpectedContext}}"
    And The value of response field "version" should be "2.0.0"
    And The response field "id" should be non-blank

    Examples:
      | actor                     | tenantContextPrefix |
      | publisherUser             |                     |
      | publisherUser@tenant1.com | /t/tenant1.com      |

  # Ports DAOTestCase#testDAOTestCase — an API deleted via DELETE /apis/{id} can be re-created by a PLAIN POST
  # with the SAME name and context. v2 today only re-creates via IMPORT (definitions.feature:import-export), which
  # resolves a name/context collision through APIImportExportManager — a different code path — so the plain
  # POST /apis uniqueness path after a delete was unguarded. Catches DAO residue that would leave the name/context
  # reserved and 409 the re-create. Least-privilege publisher in both tenants (×2).
  @cap:publisher @feat:api-lifecycle @type:regression @legacy:DAOTestCase
  Scenario Outline: A deleted API can be re-created by the same name and context as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    # Resolve the ${UNIQUE:...} name+context ONCE (put-time) so the re-create targets the SAME identity, not a
    # fresh unique value.
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "recreatePayload"
    When I create an "apis" resource with payload "recreatePayload" as "firstApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "recreateName"
    And I extract response field "context" and store it as "recreateContext"
    And I extract response field "provider" and store it as "recreateProvider"
    When I delete the "apis" resource with id "firstApiId"
    Then The response status code should be 200
    # Re-create with the SAME payload (identical name+context) through a plain POST /apis — must be accepted.
    When I attempt to create an "apis" resource with payload "recreatePayload" as "recreatedApiId"
    Then The response status code should be 201
    # The re-created API echoes back the original identity (name/context/version/provider).
    When I retrieve the "apis" resource with id "recreatedApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{recreateName}}"
    And The value of response field "context" should be "{{recreateContext}}"
    And The value of response field "version" should be "1.0.0"
    And The value of response field "provider" should be "{{recreateProvider}}"
    When I publish the "apis" resource with id "recreatedApiId"
    Then The lifecycle status of API "recreatedApiId" should be "Published"
    When I retrieve the "apis" resource with id "recreatedApiId"
    Then The response status code should be 200
    And The response array field "tags" should have exactly 3 entries
    And The response field "tags[*]" should be exactly the list "tag18-1,tag18-2,tag18-3"
    And The value of response field "description" should be "This is Test API Created by API Manager Integration Test"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Ports APIPublishingAndVisibilityInStoreTestCase — a created-but-unpublished API is present in the publisher
  # but NOT visible in the devportal (store), and only becomes visible after publish. The devportal GET returns
  # 403 for an unpublished API (even with a valid devportal token), and 200 once published. The devportal check
  # is a cross-plane prerequisite (@dep:devportal); the subject is the publisher-driven publish transition
  # gating store visibility. Runs as admin (needs both publisher publish scope and the devportal read). ×2 tenant.
  @cap:publisher @feat:api-lifecycle @type:regression @dep:devportal @legacy:APIPublishingAndVisibilityInStoreTestCase
  Scenario Outline: A created API is hidden from the store until it is published as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "storeVisApiId" and deployed it
    And The lifecycle status of API "storeVisApiId" should be "Created"

    # Present in the publisher.
    When I retrieve the "apis" resource with id "storeVisApiId"
    Then The response status code should be 200

    # Not yet visible in the devportal — an unpublished API returns 403.
    When I retrieve the devportal API "storeVisApiId" until the response status code becomes 403 within 30 seconds
    Then The response status code should be 403

    # Publish, then it becomes visible in the devportal (200).
    When I publish the "apis" resource with id "storeVisApiId"
    Then The lifecycle status of API "storeVisApiId" should be "Published"
    When I retrieve the devportal API "storeVisApiId" until the response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "{{storeVisApiId}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the publisher/devportal side of AccessibilityOfRetireAPITestCase — an API taken Published -> Deprecated
  # -> Retired reaches each state, and once RETIRED it is removed from the devportal (a retired API returns 403
  # from the store, like an unpublished one). The gateway 404-after-retire arc is covered by
  # gateway/lifecycle_stage_invocation. Runs as admin (publisher lifecycle + devportal read). ×2 tenant.
  @cap:publisher @feat:api-lifecycle @type:regression @dep:devportal @legacy:AccessibilityOfRetireAPITestCase
  Scenario Outline: A retired API transitions Published -> Deprecated -> Retired and is removed from the store as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "retireApiId" and deployed it
    When I publish the "apis" resource with id "retireApiId"
    Then The lifecycle status of API "retireApiId" should be "Published"
    # Visible in the devportal while published.
    When I retrieve the devportal API "retireApiId" until the response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    When I change the lifecycle of API "retireApiId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Deprecated"
    Then The lifecycle status of API "retireApiId" should be "Deprecated"

    When I change the lifecycle of API "retireApiId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Retired"
    Then The lifecycle status of API "retireApiId" should be "Retired"
    # Removed from the devportal once retired -> 403.
    When I retrieve the devportal API "retireApiId" until the response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # PROTOTYPED is a lifecycle state — an API can be transitioned CREATED -> PROTOTYPED via the "Deploy as a
  # Prototype" action. Ports the publisher-plane half of PrototypedAPITestcase / APIM574. (The runtime side —
  # invoke a deployed prototyped API with a subscription token -> 200, demote -> 401, inline mock, devportal
  # visibility — is covered by publisher/prototype_api.feature. The earlier "keyless -> 401" note was a
  # misread: legacy invokes WITH a token; keyless was never the contract.)
  @cap:publisher @feat:api-lifecycle @type:regression @legacy:PrototypedAPITestcase @legacy:APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase
  Scenario Outline: An API can be transitioned to the PROTOTYPED state as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "protoPayload"
    And I create an "apis" resource with payload "protoPayload" as "protoApiId"
    When I change the lifecycle of API "protoApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    And The lifecycle status of API "protoApiId" should be "Prototyped"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An API whose name contains a hyphen round-trips correctly through the UUID-addressed v4 GET and publish.
  # v4 identifies APIs by UUID rather than the legacy provider-name-version composite, so pin the exact name in
  # the returned DTO instead of exercising the removed composite-id path. Ports the current-contract behavior of
  # GIT_1638_UrlEncodedApiName.
  @cap:publisher @feat:api-lifecycle @type:regression @legacy:GIT_1638_UrlEncodedApiNameTestCase
  Scenario Outline: An API with a hyphenated name can be retrieved and published as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_hyphen_name_api.json" in context as "hyphenApiPayload"
    And I extract field "name" from json payload "hyphenApiPayload" and store it as "hyphenApiExpectedName"
    When I create an "apis" resource with payload "hyphenApiPayload" as "hyphenApiId"
    Then The response status code should be 201
    When I deploy the API with id "hyphenApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "hyphenApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{hyphenApiExpectedName}}"
    When I publish the "apis" resource with id "hyphenApiId"
    And The lifecycle status of API "hyphenApiId" should be "Published"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Ports DeleteTierAlreadyAttachedToAPITestCase — deleting a subscription tier that is attached to a published
  # API must not break the API: attach a custom tier, publish (devportal shows it), delete the tier, and a
  # subsequent API update still succeeds with the deleted tier dropped from the API's business plans. Needs both
  # admin (policy CRUD) and publisher (API CRUD) scopes, so it runs as the admin actor. ×2 tenant.
  @cap:publisher @feat:api-lifecycle @type:regression @dep:admin @legacy:DeleteTierAlreadyAttachedToAPITestCase
  Scenario Outline: An API can be updated after a subscription tier attached to it is deleted as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "delTier${UNIQUE:Tier}" allowing 1000 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tierApiId" and deployed it
    When I retrieve the "apis" resource with id "tierApiId"
    And I put the response payload in context as "tierApiPayload"
    And I update the "apis" resource "tierApiId" and "tierApiPayload" with configuration type "policies" and value:
    """
    ["Bronze","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "tierApiId"
    Then The lifecycle status of API "tierApiId" should be "Published"
    Then I retrieve the devportal API "tierApiId" until it contains "{{subThrottlePolicyName}}" within 60 seconds
    And The devportal API tiers should be exactly "Bronze,{{subThrottlePolicyName}}"
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "tierApiId"
    And I put the response payload in context as "tierApiPayload2"
    And I update the "apis" resource "tierApiId" and "tierApiPayload2" with configuration type "description" and value:
    """
    "Updated after attached tier deletion"
    """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "tierApiId"
    Then The response should not contain "{{subThrottlePolicyName}}"
    When I retrieve the devportal API "tierApiId" until its tiers are exactly "Bronze" within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "{{tierApiId}}"
    And The devportal API tiers should be exactly "Bronze"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the independent second-version create of AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase — a
  # plain POST /apis with the SAME name and the SAME context as an existing API and a new version is accepted and
  # yields a second version of the same API. The payload's own context is reused (not the publisher read-back,
  # which carries the /t/<tenant> prefix on the tenant row).
  @cap:publisher @feat:api-lifecycle @rule:versioning @type:regression @legacy:AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
  Scenario Outline: A same-named API with the same context and a new version can be created as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "sameCtxPayload"
    And I create an "apis" resource with payload "sameCtxPayload" as "sameCtxV1Id"
    Then The response status code should be 201
    And I extract response field "name" and store it as "sameCtxName"
    And I extract response field "context" and store it as "sameCtxContext"
    When I set the field "version" to "2.0.0" in the payload "sameCtxPayload"
    And I attempt to create an "apis" resource with payload "sameCtxPayload" as "sameCtxV2Id"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "sameCtxV2Id"
    Then The response status code should be 200
    And The value of response field "name" should be "{{sameCtxName}}"
    And The value of response field "context" should be "{{sameCtxContext}}"
    And The value of response field "version" should be "2.0.0"
    When I retrieve the "apis" resource with id "sameCtxV1Id"
    Then The response status code should be 200
    And The value of response field "version" should be "1.0.0"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Ports the mixed-sibling publish of AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase — v3 is created
  # from the DEPRECATED v1 while v2 is still PUBLISHED, then published with "Deprecate old versions after publishing
  # the API". The checklist deprecates only the PUBLISHED sibling (v2); the already-DEPRECATED v1 stays Deprecated,
  # and the publish itself succeeds.
  @cap:publisher @feat:api-lifecycle @rule:versioning @type:regression @legacy:AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
  Scenario Outline: Publishing with deprecate-old-versions over a deprecated and a published sibling as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "mixV1Id" and deployed it
    When I publish the "apis" resource with id "mixV1Id"
    Then The lifecycle status of API "mixV1Id" should be "Published"

    When I create a new version "2.0.0" of "apis" resource "mixV1Id" with default version "false" as "mixV2Id"
    Then The response status code should be 201
    When I deploy the API with id "mixV2Id"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mixV2Id"
    Then The lifecycle status of API "mixV2Id" should be "Published"
    And The lifecycle status of API "mixV1Id" should be "Published"

    When I change the lifecycle of API "mixV1Id" with action "Deprecate"
    Then The response status code should be 200
    And The lifecycle status of API "mixV1Id" should be "Deprecated"

    # v3 is copied from the deprecated v1 and starts CREATED.
    When I create a new version "3.0.0" of "apis" resource "mixV1Id" with default version "false" as "mixV3Id"
    Then The response status code should be 201
    And The lifecycle status of API "mixV3Id" should be "Created"
    When I deploy the API with id "mixV3Id"
    Then The response status code should be 201

    When I change the lifecycle of API "mixV3Id" with action "Publish" and checklist "Deprecate old versions after publishing the API:true"
    Then The response status code should be 200
    And The lifecycle status of API "mixV3Id" should be "Published"
    And The lifecycle status of API "mixV2Id" should be "Deprecated"
    And The lifecycle status of API "mixV1Id" should be "Deprecated"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # API-name uniqueness is case-insensitive: a second API whose name differs from an existing one only by letter
  # case (with its own independent unique context) is rejected. Distinct from the same-name/different-context case
  # above — here the names are only case-folded-equal, not byte-identical. Ports APIMANAGER3226. Verified live on
  # 4.7.0: 409 "The API name already exists" (code 900250) — a distinct path from the same-name/version 400 above.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIMANAGER3226APINameWithDifferentCaseTestCase
  Scenario Outline: Creating an API whose name differs only by case is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "dupName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "caseV1"
    And I set the field "name" to "{{dupName}}" in the payload "caseV1"
    And I create an "apis" resource with payload "caseV1" as "caseV1Id"
    Then The response status code should be 201
    When I store the uppercase of "dupName" as "dupNameUpper"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "caseV2"
    And I set the field "name" to "{{dupNameUpper}}" in the payload "caseV2"
    And I attempt to create an "apis" resource with payload "caseV2"
    Then The response status code should be 409

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Lifecycle transitions beyond publish: an API moves Created -> Published -> Blocked -> Deprecated, each state
  # reflected in its lifecycle status, and — at each state — the set of AVAILABLE transitions and the recorded LC
  # HISTORY/audit-trail are asserted. Ports RegistryLifeCycleInclusionTest (state transitions + available
  # transitions per state + lifecycle history). The available-transitions sets are the exact events the 4.7.0
  # default (registry) lifecycle offers per state — pinned and verified against a live container.
  @cap:publisher @feat:api-lifecycle @type:regression @legacy:RegistryLifeCycleInclusionTest
  Scenario Outline: An API transitions through Published, Blocked and Deprecated states as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "lcApiId" and deployed it
    And The lifecycle status of API "lcApiId" should be "Created"

    When I publish the "apis" resource with id "lcApiId"
    Then The lifecycle status of API "lcApiId" should be "Published"
    # A Published API can be prototyped, blocked, demoted back to Created, or deprecated.
    And The available lifecycle transitions of API "lcApiId" should be exactly "Deploy as a Prototype,Block,Demote to Created,Deprecate"
    And The lifecycle history of API "lcApiId" should record a transition from "Created" to "Published"

    When I change the lifecycle of API "lcApiId" with action "Block"
    Then The lifecycle status of API "lcApiId" should be "Blocked"
    # A Blocked API can be re-published or deprecated.
    And The available lifecycle transitions of API "lcApiId" should be exactly "Re-Publish,Deprecate"
    And The lifecycle history of API "lcApiId" should record a transition from "Published" to "Blocked"

    When I change the lifecycle of API "lcApiId" with action "Deprecate"
    Then The lifecycle status of API "lcApiId" should be "Deprecated"
    # A Deprecated API can only be retired.
    And The available lifecycle transitions of API "lcApiId" should be exactly "Retire"
    And The lifecycle history of API "lcApiId" should record a transition from "Blocked" to "Deprecated"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Copy-version lifecycle (ports RegistryLifeCycleInclusionTest#testChecklistItemsVisibility and
  # #testLCStateChangeVisibility): a copy starts in CREATED with Publish + Deploy as a Prototype, then the copied
  # resource itself must support Published -> Blocked -> Deprecated and expose the matching transitions/history.
  # The separate lifecycle scenario above checks those transitions on an original API, not a copied version.
  # The CREATED and Published transition sets are pinned exactly (verified live on 4.7.0).
  @cap:publisher @feat:api-lifecycle @type:regression @legacy:RegistryLifeCycleInclusionTest
  Scenario Outline: A copied API version follows its own lifecycle transitions as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "copyBaseApiId" and deployed it
    When I publish the "apis" resource with id "copyBaseApiId"
    Then The lifecycle status of API "copyBaseApiId" should be "Published"

    # Copy the published API to a new version (2.0.0), not as default.
    When I create a new version "2.0.0" of "apis" resource "copyBaseApiId" with default version "false" as "copyNewVersionId"
    Then The response status code should be 201

    # The copied version is in CREATED (a fresh version does not inherit the source's Published state).
    And The lifecycle status of API "copyNewVersionId" should be "Created"
    # Its available transitions are exactly Publish + Deploy as a Prototype (the CREATED checklist items).
    And The available lifecycle transitions of API "copyNewVersionId" should be exactly "Publish,Deploy as a Prototype"

    When I publish the "apis" resource with id "copyNewVersionId"
    Then The response status code should be 200
    And The lifecycle status of API "copyNewVersionId" should be "Published"
    And The available lifecycle transitions of API "copyNewVersionId" should be exactly "Deploy as a Prototype,Block,Demote to Created,Deprecate"
    # The legacy method intended to check Created -> Published history here, but queried the source API ID.
    # Check the copied API's own history rather than repeating that wrong-resource assertion.
    And The lifecycle history of API "copyNewVersionId" should record a transition from "Created" to "Published"

    When I change the lifecycle of API "copyNewVersionId" with action "Block"
    Then The response status code should be 200
    And The lifecycle status of API "copyNewVersionId" should be "Blocked"
    And The available lifecycle transitions of API "copyNewVersionId" should be exactly "Re-Publish,Deprecate"
    And The lifecycle history of API "copyNewVersionId" should record a transition from "Published" to "Blocked"

    When I change the lifecycle of API "copyNewVersionId" with action "Deprecate"
    Then The response status code should be 200
    And The lifecycle status of API "copyNewVersionId" should be "Deprecated"
    And The available lifecycle transitions of API "copyNewVersionId" should be exactly "Retire"
    And The lifecycle history of API "copyNewVersionId" should record a transition from "Blocked" to "Deprecated"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Demoting a published API back to Created must retain existing subscriptions. Runs as admin (needs both the
  # publisher lifecycle scope and the consumer subscribe scope). Ports APIMANAGER5337SubscriptionRetainTestCase.
  @cap:publisher @feat:api-lifecycle @type:regression @dep:devportal @legacy:APIMANAGER5337SubscriptionRetainTestCase
  Scenario Outline: Demoting a published API to Created retains its subscriptions as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "subRetainApiId" and deployed it
    When I publish the "apis" resource with id "subRetainApiId"
    Then The lifecycle status of API "subRetainApiId" should be "Published"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "subRetainApp"
    And I create an application with payload "subRetainApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "subRetainSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "subRetainApiId" using application "createdAppId" with payload "subRetainSub" as "subRetainSubId"

    When I change the lifecycle of API "subRetainApiId" with action "Demote to Created"
    Then The lifecycle status of API "subRetainApiId" should be "Created"
    When I retrieve the subscription for Api "subRetainApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The subscription with id "subRetainSubId" should be in the list of all subscriptions

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Custom API lifecycle: inject a custom LifeCycle (adds a Promoted state) into the tenant configuration, then an
  # API transitions Published -> Promoted -> Published via the custom Promote / Re-Publish events. The original
  # tenant config is restored. Admin actor (tenant-config is admin-only). Ports CustomLifeCycleTestCase.
  @cap:publisher @feat:api-lifecycle @rule:custom-lifecycle @type:regression @dep:admin @legacy:CustomLifeCycleTestCase
  Scenario Outline: A custom API lifecycle adds a Promoted state as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "origTenantConf"
    And I register tenant configuration "origTenantConf" for cleanup
    When I capture the tenant configuration as "customTenantConf"
    And I set the JSON field "LifeCycle" from file "artifacts/lifecycle/custom_api_lifecycle.json" in the payload "customTenantConf"
    And I update the tenant configuration from "customTenantConf"
    Then The response status code should be 200
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "clcApiId" and deployed it
    When I publish the "apis" resource with id "clcApiId"
    Then The lifecycle status of API "clcApiId" should be "Published"
    When I change the lifecycle of API "clcApiId" with action "Promote"
    Then The response status code should be 200
    Then The lifecycle status of API "clcApiId" should be "Promoted"
    When I change the lifecycle of API "clcApiId" with action "Re-Publish"
    Then The response status code should be 200
    Then The lifecycle status of API "clcApiId" should be "Published"
    When I update the tenant configuration from "origTenantConf"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Linter custom rules ROUND TRIP. The publisher /linter-custom-rules read returns the tenant-config's
  # LinterCustomRules block VERBATIM as the whole response body when no apiId/apiType is supplied
  # (LinterCustomRulesApiServiceImpl#getLinterCustomRules). Seed the block, read it back, compare STRICTLY against
  # the very fixture that seeded it, then restore the original tenant config. Ports
  # GetLinterCustomRulesThroughThePublisherRestAPITestCase — whose v2 predecessor in publisher/api_config asserted
  # only a 200 and so never exercised the behaviour under test at all.
  #
  # Deliberately co-located here even though it is @feat:api-config: tenant-config is a container-wide mutable
  # resource, and the custom-lifecycle scenario above is this block's only other tenant-config mutation. A
  # runner's scenarios run sequentially, so keeping both in this file removes the lost-update race that two
  # concurrently-running capture/modify/restore cycles would otherwise have on the shared container.
  @cap:publisher @feat:api-config @rule:linter-rules @type:regression @dep:admin @legacy:GetLinterCustomRulesThroughThePublisherRestAPITestCase
  Scenario Outline: The linter custom rules returned are the ones seeded into tenant-config as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "lcrOriginalConf"
    # Registered as well as restored below: tenant-config is container-wide, so a failure between the seed and the
    # explicit restore would otherwise leave it modified for every later scenario on this container.
    And I register tenant configuration "lcrOriginalConf" for cleanup
    And I capture the tenant configuration as "lcrSeededConf"
    And I set the JSON field "LinterCustomRules" from file "artifacts/payloads/linter_custom_rules.json" in the payload "lcrSeededConf"
    And I update the tenant configuration from "lcrSeededConf"
    Then The response status code should be 200
    When I retrieve the linter custom rules
    Then The response status code should be 200
    And The response body should equal the JSON file "artifacts/payloads/linter_custom_rules.json"
    When I update the tenant configuration from "lcrOriginalConf"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
