@cleanup
Feature: Publisher API Lifecycle

  Publisher-plane lifecycle of a REST API: create + deploy, retrieve, update (description & tiers),
  the update-does-not-rename invariant, publish, and presence in the API list. Asserts only publisher
  outcomes — subscription and gateway invocation are covered by devportal/subscribe and
  gateway/rest-invocation. Teardown via the @cleanup hook.

  @cap:publisher @feat:api-lifecycle @type:smoke @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: Create, update, publish and list a REST API as <actor>
    # Runs as a least-privilege publisher user (creator+publisher, not admin), in both the super tenant
    # and tenant1.com, proving the publisher-plane lifecycle is tenant-agnostic.
    Given The system is ready and I have valid publisher access tokens as "<actor>"

    # Create + deploy a revision
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it

    # A freshly created (not-yet-published) API is in the CREATED lifecycle state
    And The lifecycle status of API "createdApiId" should be "Created"

    # Retrieve and confirm the created metadata
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The response should contain "1.0.0"
    And The response should contain "lastUpdatedTimestamp"
    And I put the response payload in context as "retrievedApiPayload"

    # Update description and tier collection
    When I put JSON payload from file "artifacts/payloads/update_apim_test_api.json" in context as "apiUpdatePayload"
    And I update "apis" resource of id "createdApiId" with payload "apiUpdatePayload"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The response should contain "Updated description for the created API"
    And The response should contain "Gold"
    And The response should contain "Bronze"
    And The response should contain "Silver"

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
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: A subscriber-role user cannot create an API as <actor>
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

  # Create-validation matrix — ports APIM514 (missing mandatory fields), APIMANAGER5834 (invalid context) and
  # APIM519 (no auth). Built from the valid base payload with one field blanked/invalidated per row, so no
  # per-case fixture is needed. Only the clean, deterministic rejections are ported: blank name/context/version
  # and an invalid context all return 400; missing auth returns 401. The legacy tier/action cases are omitted
  # (they assert a 500 server error, not a validation response) and endpoint/resources were already disabled in
  # legacy.
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

  # No-auth is auth-layer and tenant-agnostic (the tokenless request cannot resolve a tenant, so it always hits
  # the super-tenant publisher endpoint) — ×1.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase
  Scenario: Creating an API without authentication is rejected
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "noAuthApiPayload"
    And I attempt to create an "apis" resource with payload "noAuthApiPayload" without authentication
    Then The response status code should be 401

  # Duplicate-context — creating a second API whose context matches an existing API is rejected (409). Ports the
  # same-context case of APIM18. The first API's resolved context is captured and reused for the second create,
  # whose (independently unique) name isolates the failure to the context clash.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIM18CreateAnAPIThroughThePublisherRestAPITestCase
  Scenario Outline: Creating an API with an already-existing context is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "dupCtxA"
    And I create an "apis" resource with payload "dupCtxA" as "dupApiIdA"
    Then The response status code should be 201
    And I extract response field "context" and store it as "dupContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "dupCtxB"
    And I set the field "context" to "{{dupContext}}" in the payload "dupCtxB"
    And I attempt to create an "apis" resource with payload "dupCtxB"
    Then The response status code should be 409

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

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

  # An API whose name contains a hyphen / URL-encodable characters round-trips correctly through get-by-id and
  # publish (the identifier is not mangled). Ports GIT_1638_UrlEncodedApiName.
  @cap:publisher @feat:api-lifecycle @type:regression @legacy:GIT_1638_UrlEncodedApiNameTestCase
  Scenario Outline: An API with a hyphenated name can be retrieved and published as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_hyphen_name_api.json" as "hyphenApiId" and deployed it
    When I retrieve the "apis" resource with id "hyphenApiId"
    Then The response status code should be 200
    And The response should contain "hyphen-api-name"
    When I publish the "apis" resource with id "hyphenApiId"
    Then The response status code should be 200
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

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the context-mismatch case of APIMANAGER5834 — creating a second API with the SAME name but a different
  # context (here also a new version) is rejected with 400. The first API's resolved name is captured and reused.
  # Verified live on 4.7.0 (400). Legacy also asserted the body "API Context does not exist"; we assert the code
  # only — the message text is version-brittle.
  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIMANAGER5834APICreationWithInvalidInputsTestCase
  Scenario Outline: Creating a same-named API with a different context is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ctxV1"
    And I create an "apis" resource with payload "ctxV1" as "ctxV1Id"
    Then The response status code should be 201
    And I extract response field "name" and store it as "ctxName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "ctxV2"
    And I set the field "name" to "{{ctxName}}" in the payload "ctxV2"
    And I set the field "version" to "2.0.0" in the payload "ctxV2"
    And I attempt to create an "apis" resource with payload "ctxV2"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
