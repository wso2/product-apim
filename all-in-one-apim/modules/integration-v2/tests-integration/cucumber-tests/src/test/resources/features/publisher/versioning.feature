@cleanup
Feature: Publisher API Versioning

  Publisher-plane versioning: creating a new version of an API, the default-version flag, and taking
  the new version through its lifecycle to Published. Asserts only publisher outcomes — that the new
  version is invocable at the gateway is covered separately by gateway/rest-invocation. Positive flow
  runs as a least-privilege publisher in both the super tenant and tenant1.com.

  # The default-version smoke below also provides parity for NewCopyWithDefaultVersion (copy a new version AS the
  # default and confirm isDefaultVersion=true on it) and CopyNewVersionTestCase (legacy "copy API" == v2
  # create-version; the copied version's name is retained and its version equals the requested new version — both
  # asserted here via the response body containing "2.0.0" and the retrieved payload). CopyNewVersionTestCase was
  # commented-out in the legacy testng.xml (its APICreationRequestBean/old-REST "copyAPI" path is fully subsumed by
  # the v2 create-version primitive), so it is parity-tagged, not separately ported.
  @cap:publisher @feat:versioning @type:smoke @legacy:APIVersioningTestCase @legacy:NewCopyWithDefaultVersion @legacy:CopyNewVersionTestCase
  Scenario Outline: Create, version and publish an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it

    When I create a new version "2.0.0" of "apis" resource "createdApiId" with default version "true" as "newVersionId"
    Then The response status code should be 201
    And The response should contain "2.0.0"
    And The lifecycle status of API "newVersionId" should be "Created"

    When I retrieve the "apis" resource with id "newVersionId"
    Then The response status code should be 200
    And The response should contain "2.0.0"
    And I put the response payload in context as "retrievedApiPayload"
    # The new version was created as the default. APIM keeps a single default version, so assert the flag is
    # set on the new version AND that the original version was flipped out of default (the reflect step
    # re-fetches the id from the preceding retrieve and retries, tolerating the propagation delay).
    And The "apis" resource should reflect the updated "isDefaultVersion" as:
      """
      true
      """
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The "apis" resource should reflect the updated "isDefaultVersion" as:
      """
      false
      """

    When I deploy the API with id "newVersionId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "retrievedApiPayload"

    When I publish the "apis" resource with id "newVersionId"
    Then The lifecycle status of API "newVersionId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:versioning @type:negative @legacy:APIVersioningTestCase
  Scenario Outline: A subscriber-role user cannot create a new API version as <actor>
    # Create the base API as a publisher, then re-authenticate as a subscriber whose token lacks the
    # api_create scope and confirm the version-create is rejected (401), in both tenants.
    Given The system is ready and I have valid publisher access tokens as "<publisher>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    Given The system is ready and I have valid publisher access tokens as "<subscriber>"
    When I attempt to create a new version "2.0.0" of "apis" resource "createdApiId" with default version "true"
    Then The response status code should be 401
    # Switch back so @cleanup deletes the publisher-owned base API with the publisher's token.
    And I act as "<publisher>"

    Examples:
      | publisher                  | subscriber                  |
      | publisherUser              | subscriberUser              |
      | publisherUser@tenant1.com  | subscriberUser@tenant1.com  |

  # Ports SameVersionAPITestCase — creating a new version whose version string equals an ALREADY-EXISTING version
  # of the same API is rejected. The base API is version 1.0.0; requesting a copy back to "1.0.0" collides with
  # the source version. Legacy pins 409 with body "The API version already exists"; we assert the exact 409 AND the
  # stable numeric error code 900252 (API_VERSION_ALREADY_EXISTS) carried in the body — the human-readable message
  # text is version-brittle, so it is not asserted.
  @cap:publisher @feat:versioning @type:negative @legacy:SameVersionAPITestCase
  Scenario Outline: Creating a new version with an already-existing version string is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "sameVerApiId" and deployed it
    When I attempt to create a new version "1.0.0" of "apis" resource "sameVerApiId" with default version "false"
    Then The response status code should be 409
    And The response should contain "900252"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Ports NewVersionUpdateTestCase. Two facets: (1) a copied (new) version can be independently updated — change the
  # copied version's production endpoint URL and confirm the new URL is reflected in its endpointConfig (the copy
  # has its own endpoint config, decoupled from the source version); (2) both published versions of an API are
  # surfaced in the devportal — after copying v1 -> v2 and publishing both, a devportal search by the API name lists
  # BOTH entries (v1 AND v2). NOTE: the legacy test asserted a count of 1 ("only the latest version"), but that
  # relies on DisplayMultipleVersions=false, which is NOT the default config of this v2 lane — the default lists
  # every published version. We therefore assert the ACTUAL default-config behaviour (both versions listed), pinned
  # live: count:2 with both 1.0.0 and 2.0.0. The devportal search is a cross-plane read (@dep:devportal); the subject
  # is the publisher versioning behaviour. ×2 tenant.
  @cap:publisher @feat:versioning @type:regression @dep:devportal @legacy:NewVersionUpdateTestCase
  Scenario Outline: A copied API version is independently updatable and both versions are listed in the devportal as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "verUpdApiId" and deployed it
    When I retrieve the "apis" resource with id "verUpdApiId"
    And I extract response field "name" and store it as "verUpdName"
    When I publish the "apis" resource with id "verUpdApiId"
    Then The lifecycle status of API "verUpdApiId" should be "Published"

    # Copy v1 -> v2 as the new default version.
    When I create a new version "2.0.0" of "apis" resource "verUpdApiId" with default version "true" as "verUpdNewApiId"
    Then The response status code should be 201

    # Update the copied version's production endpoint to a distinct backend URL.
    When I retrieve the "apis" resource with id "verUpdNewApiId"
    And I put the response payload in context as "verUpdPayload"
    When I put the following JSON payload in context as "verUpdEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3015/updated/"},"sandbox_endpoints":{"url":"http://nodebackend:3015/updated/"}}
    """
    When I update the "apis" resource "verUpdNewApiId" and "verUpdPayload" with configuration type "endpointConfig" and value:
    """
    verUpdEndpoint
    """
    Then The response status code should be 200

    # The new endpoint URL is reflected on the copied version.
    When I retrieve the "apis" resource with id "verUpdNewApiId"
    Then The response status code should be 200
    And The response should contain "http://nodebackend:3015/updated/"

    # Deploy and publish the copied version so both versions are published.
    When I deploy the API with id "verUpdNewApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "verUpdNewApiId"
    Then The lifecycle status of API "verUpdNewApiId" should be "Published"

    # The devportal lists BOTH published versions of the API. Poll the search until it surfaces the NEW version
    # (2.0.0) — the distinguishing end-state: polling on a condition the PRE-existing v1 entry already satisfies
    # would return immediately on stale state and race the assertion. Once 2.0.0 appears, both versions are listed
    # ("count":2, containing 1.0.0 and 2.0.0).
    When I search DevPortal APIs with query "{{verUpdName}}" until it contains "2.0.0" within 60 seconds
    Then The response should contain "1.0.0"
    And The response should contain "2.0.0"
    And The response should contain "\"count\":2"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
