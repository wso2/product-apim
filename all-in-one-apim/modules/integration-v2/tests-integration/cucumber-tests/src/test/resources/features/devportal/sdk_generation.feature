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
