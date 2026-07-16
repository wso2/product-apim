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
  full language list is exercised in the super tenant (SDK generation is tenant-agnostic); a representative
  language is additionally run in tenant1.com to prove the tenant-domain-qualified store path also serves
  the SDK. Teardown is the runner's AfterClass sweep (the setup API is deleted once after all scenarios).

  @cap:devportal @feat:sdk-generation @type:regression @dep:publisher @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario Outline: Generate a client SDK in <language> for a published API in the super tenant
    Given The system is ready and I have valid devportal access token as "subscriberUser"
    When I generate a client SDK in language "<language>" for API "publishedApiId"
    # 200 = a downloadable SDK zip is returned (confirmed against 4.7.0 for every supported language).
    Then The response status code should be 200

    Examples:
      | language   |
      | android    |
      | java       |
      | csharp     |
      | dart       |
      | groovy     |
      | javascript |
      | jmeter     |
      | perl       |
      | php        |
      | python     |
      | ruby       |
      | swift5     |
      | clojure    |

  # SDK generation is tenant-agnostic, so the full 13-language sweep above runs super-only; this single row
  # proves the tenant-domain-qualified store SDK path (t/tenant1.com) also serves a 200 SDK zip.
  @cap:devportal @feat:sdk-generation @type:regression @dep:publisher @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario: Generate a client SDK for a published API in tenant1.com
    Given The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    When I generate a client SDK in language "java" for API "publishedApiId@tenant1.com"
    Then The response status code should be 200
