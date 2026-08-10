Feature: DevPortal Client SDK Generation

  DevPortal-plane client-SDK generation: for a published API visible in the Developer Portal, the store
  generates a downloadable client SDK for each supported programming language via
  GET /apis/{apiId}/sdks/{language}, returning 200 with the SDK zip. Ports the SDK-generation coverage of
  the legacy CORSAccessControlAllowCredentialsHeaderTestCase#testAllSupportedSDKGeneration, which iterated
  the full supported-language list (android, java, csharp, dart, groovy, javascript, jmeter, perl, php,
  python, ruby, swift5, clojure) and asserted 200 for each. Thematically a devportal concern (SDK gen),
  orthogonal to the legacy CORS host — placed here as its own @cap:devportal feature. The published API for
  each tenant is provided by _setup_published_apis (listed first in the runner, created as that tenant's
  admin) and shared via tenant-qualified keys. SDK generation is performed as the subscriber consumer. The
  full language list is exercised in both tenants. The tenant comes from the acting actor's devportal token
  and the tenant API id, NOT from the URL
  (devportal REST has no t/<tenant> route; see the scenario comment).
  Teardown is the runner's AfterClass sweep (the setup API is deleted once after all scenarios).

  @cap:devportal @feat:sdk-generation @type:regression @dep:publisher @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario Outline: Generate a client SDK in <language> for a published API as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I generate a client SDK in language "<language>" for API "<apiId>"
    # 200 = a downloadable SDK zip is returned (confirmed against 4.7.0 for every supported language).
    Then The response status code should be 200

    Examples:
      | language   | actor                      | apiId                    |
      | android    | subscriberUser             | publishedApiId           |
      | java       | subscriberUser             | publishedApiId           |
      | csharp     | subscriberUser             | publishedApiId           |
      | dart       | subscriberUser             | publishedApiId           |
      | groovy     | subscriberUser             | publishedApiId           |
      | javascript | subscriberUser             | publishedApiId           |
      | jmeter     | subscriberUser             | publishedApiId           |
      | perl       | subscriberUser             | publishedApiId           |
      | php        | subscriberUser             | publishedApiId           |
      | python     | subscriberUser             | publishedApiId           |
      | ruby       | subscriberUser             | publishedApiId           |
      | swift5     | subscriberUser             | publishedApiId           |
      | clojure    | subscriberUser             | publishedApiId           |
      | android    | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | java       | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | csharp     | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | dart       | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | groovy     | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | javascript | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | jmeter     | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | perl       | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | php        | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | python     | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | ruby       | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | swift5     | subscriberUser@tenant1.com | publishedApiId@tenant1.com |
      | clojure    | subscriberUser@tenant1.com | publishedApiId@tenant1.com |

  # Ports the cross-tenant half of SDKGenerationTestCase. Legacy expressed the denial only as "the helper returned
  # false" (its generateSDK swallowed the ApiException and reported a boolean), so no status was ever pinned; here
  # the exact 404 is asserted — the API simply does not exist in the requesting actor's organization.
  # Both directions are exercised, each with its OWN-tenant generation as the positive control in the same
  # scenario, so a blanket SDK outage cannot masquerade as a cross-tenant denial. Both APIs are PUBLIC (as in
  # legacy), which is the point: tenant isolation holds even for a publicly-visible API.
  @cap:devportal @feat:sdk-generation @type:negative @dep:publisher @legacy:SDKGenerationTestCase
  Scenario: SDK generation is refused across tenants in both directions
    # tenant1.com consumer: own tenant's API succeeds, the super tenant's API is 404.
    Given The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    When I generate a client SDK in language "java" for API "publishedApiId@tenant1.com"
    Then The response status code should be 200
    When I generate a client SDK in language "java" for API "publishedApiId"
    Then The response status code should be 404
    # Super-tenant consumer: own tenant's API succeeds, tenant1.com's API is 404.
    Given The system is ready and I have valid devportal access token as "subscriberUser"
    When I generate a client SDK in language "java" for API "publishedApiId"
    Then The response status code should be 200
    When I generate a client SDK in language "java" for API "publishedApiId@tenant1.com"
    Then The response status code should be 404

  # Ports SDKGenerationTestCase#testSDKGenerationForPrivateAPIs. A PRIVATE-visibility API is visible to any
  # AUTHENTICATED user of its own tenant but to nobody outside it, and SDK generation follows that visibility: a
  # same-tenant consumer (who is not the provider and has no subscription) gets the SDK zip, while a consumer in
  # another tenant gets 404. Created inline — the shared _setup_published_apis fixture is PUBLIC and must stay so
  # for the language sweep above; torn down by the runner's AfterClass sweep.
  @cap:devportal @feat:sdk-generation @type:regression @dep:publisher @legacy:SDKGenerationTestCase
  Scenario: A PRIVATE-visibility API's SDK is generated for a same-tenant consumer but refused across tenants
    Given The system is ready
    And I have valid access tokens as "admin"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "sdkPrivPayload"
    And I have created an api from context payload "sdkPrivPayload" with devportal visibility "PRIVATE" for roles "" as "sdkPrivApiId" and deployed it
    When I publish the "apis" resource with id "sdkPrivApiId"
    Then The lifecycle status of API "sdkPrivApiId" should be "Published"

    # Same tenant, authenticated consumer: the devportal serves the PRIVATE API (readiness gate) and its SDK.
    Given The system is ready and I have valid devportal access token as "subscriberUser"
    When I retrieve the devportal API "sdkPrivApiId" until it contains "PUBLISHED" within 60 seconds
    Then The response status code should be 200
    When I generate a client SDK in language "java" for API "sdkPrivApiId"
    Then The response status code should be 200

    # Another tenant's consumer: the API is not in their organization at all.
    Given The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    When I generate a client SDK in language "java" for API "sdkPrivApiId"
    Then The response status code should be 404
