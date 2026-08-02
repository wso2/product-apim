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
  language is additionally run in tenant1.com to prove a TENANT-OWNED API's SDK generates for a tenant
  consumer — the tenant comes from the acting actor's devportal token and the tenant API id, NOT from the URL
  (devportal REST has no t/<tenant> route; see the scenario comment).
  Teardown is the runner's AfterClass sweep (the setup API is deleted once after all scenarios).

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
  # proves a TENANT-OWNED API's SDK generates for a tenant consumer — the tenant is carried by the acting actor's
  # devportal token (and the tenant API id), NOT by the URL. There is no "t/<tenant>" devportal REST route to
  # exercise: devportal tenancy is token/X-WSO2-Tenant-header based (only the OAuth2 userinfo/introspect endpoints
  # are tenant-path-qualified), and Utils.getApiSdkURL is deliberately single-arg for that reason. An earlier
  # revision of this comment claimed this row proved a "tenant-domain-qualified store SDK path (t/tenant1.com)";
  # it did not, and no such path exists — do not "restore" that claim or add a tenant prefix to the SDK URL.
  @cap:devportal @feat:sdk-generation @type:regression @dep:publisher @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario: Generate a client SDK for a published API in tenant1.com
    Given The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    When I generate a client SDK in language "java" for API "publishedApiId@tenant1.com"
    Then The response status code should be 200
