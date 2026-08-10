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
    # The create-version primitive must carry the CREATOR's identity onto the copy — the provider is the
    # authorization key for every later publisher operation on the new version, and versioning is the one publisher
    # primitive that mints a second artifact from a first. Asserted on BOTH the base API and the copy so the third
    # Examples row proves a store-qualified provider survives the copy verbatim, store domain and all.
    And The provider of API "createdApiId" should match actor "<actor>"
    And The provider of API "newVersionId" should match actor "<actor>"

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

    # The THIRD row runs the whole arc as a SECONDARY.COM user-store creator (CLAUDE.md §12), closing the legacy
    # SUPER_TENANT_USER_STORE_USER mode for the provider-identity facet. Super tenant only: the store domain, not
    # the tenant, is the variable, and the row above already varies the tenant.
    Examples:
      | actor                         |
      | publisherUser                 |
      | publisherUser@tenant1.com     |
      | SECONDARY.COM/publisherUser1  |

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

  # Ports NewVersionUpdateTestCase. Three facets: (1) a copied (new) version can be independently updated — change
  # the copied version's production endpoint URL and confirm the new URL is reflected in its endpointConfig (the copy
  # has its own endpoint config, decoupled from the source version); (2) a devportal SEARCH by the API name lists
  # BOTH published versions (count 2, 1.0.0 and 2.0.0); (3) the devportal's UNFILTERED listing lists the API ONCE,
  # at its LATEST version — which is what NewVersionUpdateTestCase#testCheckMultipleVersionedAPIsCount asserted.
  #
  # (2) and (3) are BOTH true at once, and the reason is not a config difference — this lane runs the product
  # DEFAULT `apim.devportal.display_multiple_versions = false` (nothing overrides it). Traced in carbon-apimgt
  # 9.33.162: RegistrySearchUtil.getDevPortalSearchQuery appends the Solr grouping clause
  # `&group=true&group.field=name&group.ngroups=true&group.sort=versionComparable desc` when the flag is false, but
  # ONLY when the effective query is EMPTY or is EXACTLY the devportal type-filter constant. The devportal REST layer
  # (ApisApiServiceImpl.apisGet) APPENDS that constant to any user-supplied query, so `?query=<name>` can never
  # satisfy the equality test and is never grouped — which is why a name search legitimately returns both versions
  # while the unfiltered listing returns only the latest. So the flag is NOT dead config; it governs a DIFFERENT
  # endpoint, and both behaviours are pinned here rather than one being relaxed to make room for the other.
  # (The in-memory APIConsumerImpl.filterMultipleVersionedAPIs, the pre-Solr implementation, is dead code — private
  # with no callers.) The listing assertion counts occurrences of THIS API's uniquely-generated name, not the
  # tenant-wide total the legacy test pinned, which can only hold in an empty tenant.
  #
  # The devportal reads are cross-plane (@dep:devportal); the subject is the publisher versioning behaviour.
  # ×2 tenant.
  @cap:publisher @feat:versioning @type:regression @dep:devportal @legacy:NewVersionUpdateTestCase
  Scenario Outline: A copied API version is independently updatable and the devportal groups its versions as <actor>
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

    # The UNFILTERED devportal listing — the one read path that applies the DisplayMultipleVersions grouping —
    # carries this API exactly ONCE, at its latest version (2.0.0). The count-2 search above is the control: it
    # proves both versions really are published and devportal-visible, so a single entry here is version grouping
    # and not a missing publish.
    Then the devportal API listing should list API "{{verUpdName}}" exactly 1 time with version "2.0.0" within 60 seconds

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
