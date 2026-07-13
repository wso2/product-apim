Feature: Publisher API Runtime & Common Configuration

  Publisher-plane configuration across all four API types (REST, SOAP, GraphQL, WebSocket): each scenario
  PATCHes one configuration field and asserts the change persists on re-fetch, in BOTH the super tenant and
  tenant1.com. The base APIs are created per type per tenant by _setup_config_api (listed first in the runner)
  and shared via the runner's local scope under type-/tenant-qualified keys (configApiId / configSoapApiId /
  configGraphqlApiId / configWsApiId, each optionally suffixed @tenant1.com, plus the matching payloads);
  teardown is the runner's AfterClass sweep. Each scenario acts as the matching tenant's admin. Shared-scope
  assignment is covered by publisher/scopes.

  @cap:publisher @feat:api-config @type:regression @rule:rest @legacy:APIRuntimeConfigurationsTestCase @legacy:APIOtherCommonConfigurationsTestCase
  Scenario Outline: Update a REST API configuration field in <tenant>
    Given I act as "admin<tenantSuffix>"
    When I update the "apis" resource "configApiId<tenantSuffix>" and "configApiPayload<tenantSuffix>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "configApiId<tenantSuffix>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | tenant      | tenantSuffix | configType             | configValue                                                                                                                                                                                              |
      | super       |              | responseCachingEnabled | true                                                                                                                                                                                                     |
      | super       |              | cacheTimeout           | 400                                                                                                                                                                                                      |
      | super       |              | enableSchemaValidation | true                                                                                                                                                                                                     |
      | super       |              | transport              | ["https"]                                                                                                                                                                                                |
      | super       |              | corsConfiguration      | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true}         |
      | super       |              | additionalProperties   | [{"name":"newProperty","value":"newValue","display":true}]                                                                                                                                               |
      | super       |              | policies               | ["Bronze","Gold","Silver"]                                                                                                                                                                               |
      | tenant1.com | @tenant1.com | responseCachingEnabled | true                                                                                                                                                                                                     |
      | tenant1.com | @tenant1.com | cacheTimeout           | 400                                                                                                                                                                                                      |
      | tenant1.com | @tenant1.com | enableSchemaValidation | true                                                                                                                                                                                                     |
      | tenant1.com | @tenant1.com | transport              | ["https"]                                                                                                                                                                                                |
      | tenant1.com | @tenant1.com | corsConfiguration      | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true}         |
      | tenant1.com | @tenant1.com | additionalProperties   | [{"name":"newProperty","value":"newValue","display":true}]                                                                                                                                               |
      | tenant1.com | @tenant1.com | policies               | ["Bronze","Gold","Silver"]                                                                                                                                                                               |

  @cap:publisher @feat:api-config @type:regression @rule:soap @legacy:APIRuntimeConfigurationsTestCase @legacy:APIOtherCommonConfigurationsTestCase
  Scenario Outline: Update a SOAP API configuration field in <tenant>
    Given I act as "admin<tenantSuffix>"
    When I update the "apis" resource "configSoapApiId<tenantSuffix>" and "configSoapApiPayload<tenantSuffix>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "configSoapApiId<tenantSuffix>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | tenant      | tenantSuffix | configType             | configValue                                                                                                                                                                                              |
      | super       |              | responseCachingEnabled | true                                                                                                                                                                                                     |
      | super       |              | cacheTimeout           | 400                                                                                                                                                                                                      |
      | super       |              | enableSchemaValidation | true                                                                                                                                                                                                     |
      | super       |              | transport              | ["http","https"]                                                                                                                                                                                         |
      | super       |              | corsConfiguration      | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true}         |
      | super       |              | additionalProperties   | [{"name":"newProperty","value":"newValue","display":true}]                                                                                                                                               |
      | super       |              | policies               | ["Bronze","Gold","Silver"]                                                                                                                                                                               |
      | tenant1.com | @tenant1.com | responseCachingEnabled | true                                                                                                                                                                                                     |
      | tenant1.com | @tenant1.com | cacheTimeout           | 400                                                                                                                                                                                                      |
      | tenant1.com | @tenant1.com | enableSchemaValidation | true                                                                                                                                                                                                     |
      | tenant1.com | @tenant1.com | transport              | ["http","https"]                                                                                                                                                                                         |
      | tenant1.com | @tenant1.com | corsConfiguration      | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true}         |
      | tenant1.com | @tenant1.com | additionalProperties   | [{"name":"newProperty","value":"newValue","display":true}]                                                                                                                                               |
      | tenant1.com | @tenant1.com | policies               | ["Bronze","Gold","Silver"]                                                                                                                                                                               |

  @cap:publisher @feat:api-config @type:regression @rule:graphql @legacy:APIRuntimeConfigurationsTestCase @legacy:APIOtherCommonConfigurationsTestCase
  Scenario Outline: Update a GraphQL API configuration field in <tenant>
    Given I act as "admin<tenantSuffix>"
    When I update the "apis" resource "configGraphqlApiId<tenantSuffix>" and "configGraphqlApiPayload<tenantSuffix>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "configGraphqlApiId<tenantSuffix>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | tenant      | tenantSuffix | configType             | configValue                                                                                                                                                                                              |
      | super       |              | responseCachingEnabled | true                                                                                                                                                                                                     |
      | super       |              | cacheTimeout           | 400                                                                                                                                                                                                      |
      | super       |              | enableSchemaValidation | true                                                                                                                                                                                                     |
      | super       |              | transport              | ["http","https"]                                                                                                                                                                                         |
      | super       |              | corsConfiguration      | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true}         |
      | super       |              | additionalProperties   | [{"name":"newProperty","value":"newValue","display":true}]                                                                                                                                               |
      | super       |              | policies               | ["Bronze","Gold","Silver"]                                                                                                                                                                               |
      | tenant1.com | @tenant1.com | responseCachingEnabled | true                                                                                                                                                                                                     |
      | tenant1.com | @tenant1.com | cacheTimeout           | 400                                                                                                                                                                                                      |
      | tenant1.com | @tenant1.com | enableSchemaValidation | true                                                                                                                                                                                                     |
      | tenant1.com | @tenant1.com | transport              | ["http","https"]                                                                                                                                                                                         |
      | tenant1.com | @tenant1.com | corsConfiguration      | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true}         |
      | tenant1.com | @tenant1.com | additionalProperties   | [{"name":"newProperty","value":"newValue","display":true}]                                                                                                                                               |
      | tenant1.com | @tenant1.com | policies               | ["Bronze","Gold","Silver"]                                                                                                                                                                               |

  # WebSocket/Async API — the legacy matrix exercised throttling policy (runtime) plus custom properties and
  # subscription policies (other-common) for this type; caching/transport/CORS do not apply.
  @cap:publisher @feat:api-config @type:regression @rule:streaming @legacy:APIRuntimeConfigurationsTestCase @legacy:APIOtherCommonConfigurationsTestCase
  Scenario Outline: Update a WebSocket API configuration field in <tenant>
    Given I act as "admin<tenantSuffix>"
    When I update the "apis" resource "configWsApiId<tenantSuffix>" and "configWsApiPayload<tenantSuffix>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "configWsApiId<tenantSuffix>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | tenant      | tenantSuffix | configType           | configValue                                                |
      | super       |              | apiThrottlingPolicy  | Unlimited                                                  |
      | super       |              | additionalProperties | [{"name":"newProperty","value":"newValue","display":true}] |
      | super       |              | policies             | ["Bronze","Gold","Silver"]                                 |
      | tenant1.com | @tenant1.com | apiThrottlingPolicy  | Unlimited                                                  |
      | tenant1.com | @tenant1.com | additionalProperties | [{"name":"newProperty","value":"newValue","display":true}] |
      | tenant1.com | @tenant1.com | policies             | ["Bronze","Gold","Silver"]                                 |

  # Resources/operations across all four types (type-specific verbs), asserted by re-fetch containing the new
  # target. Verified via "contains" rather than exact reflect because the server augments each operation with
  # ids/policies on persist.
  @cap:publisher @feat:api-config @type:regression @rule:operations @legacy:APIOtherCommonConfigurationsTestCase
  Scenario Outline: Add a resource operation to a <label> API in <tenant>
    Given I act as "admin<tenantSuffix>"
    When I update the "apis" resource "<apiKey><tenantSuffix>" and "<payloadKey><tenantSuffix>" with configuration type "operations" and value:
      """
      <operations>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiKey><tenantSuffix>"
    Then The response should contain "newlyAddedResource"

    Examples:
      | label     | apiKey             | payloadKey              | tenant      | tenantSuffix | operations                                                |
      | REST      | configApiId        | configApiPayload        | super       |              | [{"verb":"POST","target":"/newlyAddedResource"}]          |
      | SOAP      | configSoapApiId    | configSoapApiPayload    | super       |              | [{"verb":"POST","target":"/newlyAddedResource"}]          |
      | GraphQL   | configGraphqlApiId | configGraphqlApiPayload | super       |              | [{"verb":"QUERY","target":"newlyAddedResource"}]          |
      | WebSocket | configWsApiId      | configWsApiPayload      | super       |              | [{"verb":"SUBSCRIBE","target":"/newlyAddedResource"}]     |
      | REST      | configApiId        | configApiPayload        | tenant1.com | @tenant1.com | [{"verb":"POST","target":"/newlyAddedResource"}]          |
      | SOAP      | configSoapApiId    | configSoapApiPayload    | tenant1.com | @tenant1.com | [{"verb":"POST","target":"/newlyAddedResource"}]          |
      | GraphQL   | configGraphqlApiId | configGraphqlApiPayload | tenant1.com | @tenant1.com | [{"verb":"QUERY","target":"newlyAddedResource"}]          |
      | WebSocket | configWsApiId      | configWsApiPayload      | tenant1.com | @tenant1.com | [{"verb":"SUBSCRIBE","target":"/newlyAddedResource"}]     |

  @cap:publisher @feat:api-config @type:negative @legacy:APIRuntimeConfigurationsTestCase
  Scenario Outline: A subscriber-role user cannot update API configuration in <tenant>
    Given The system is ready and I have valid publisher access tokens as "subscriberUser<tenantSuffix>"
    When I update "apis" resource of id "configApiId<tenantSuffix>" with payload "configApiPayload<tenantSuffix>"
    Then The response status code should be 401
    And I act as "admin<tenantSuffix>"

    Examples:
      | tenant      | tenantSuffix |
      | super       |              |
      | tenant1.com | @tenant1.com |

  # Small self-contained publisher read endpoints (no shared config API needed). Ports GetLinterCustomRules and
  # the publisher throttling-policies read (APIM634 tiers / APIMGetAllSubscriptionThrottlingPolicies).
  @cap:publisher @feat:api-config @type:smoke @legacy:GetLinterCustomRulesThroughThePublisherRestAPITestCase
  Scenario Outline: Retrieve the linter custom rules as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the linter custom rules
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:api-config @type:smoke @legacy:APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase @legacy:APIMGetAllSubscriptionThrottlingPolicies
  Scenario Outline: Retrieve available <level> throttling policies as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the publisher "<level>" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

    Examples:
      | level        | actor                     |
      | subscription | publisherUser             |
      | subscription | publisherUser@tenant1.com |
      | api          | publisherUser             |
      | api          | publisherUser@tenant1.com |

  # I1: a CORS-disabled API returns EMPTY arrays (not null) for the CORS allow-lists. Ports
  # CheckEmptyCORSConfigurationsTestCase — creating an API with an explicit CORS object whose lists are null
  # makes the product normalise them to [] (not null) in the API response. This is a CREATE-time normalisation:
  # the legacy test passes null lists to addAPI and asserts the GET returns []. (A whole-null corsConfiguration
  # instead yields the full CORS defaults like ["*"], and updating an existing API's CORS to null does not
  # renormalise — so this must be asserted at create, via a payload carrying the explicit null-list CORS object.)
  @cap:publisher @feat:api-config @rule:cors @type:regression @legacy:CheckEmptyCORSConfigurationsTestCase
  Scenario Outline: A CORS-disabled API with null CORS lists returns empty arrays rather than null as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_null_api.json" as "corsApiId" and deployed it
    When I retrieve the "apis" resource with id "corsApiId"
    Then The response status code should be 200
    And The response should contain "\"accessControlAllowOrigins\":[]"
    And The response should contain "\"accessControlAllowHeaders\":[]"
    And The response should contain "\"accessControlAllowMethods\":[]"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # I2: a thumbnail uploaded to an API survives a subsequent API update that doesn't touch it (the thumbnail is
  # a separate resource, not a field in the API JSON). Ports APIMANAGER5872.
  @cap:publisher @feat:api-config @rule:thumbnail @type:regression @legacy:APIMANAGER5872UpdateAPIWithoutThumbnailValueAndAPISummaryTestCase
  Scenario Outline: An API thumbnail is preserved across an API update that omits it as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "thumbApiId" and deployed it
    When I upload thumbnail "artifacts/images/thumbnail.png" for API "thumbApiId"
    Then The response status code should be 201
    When I retrieve the thumbnail for API "thumbApiId"
    Then The response status code should be 200
    # Update the API (description only) — the thumbnail must survive.
    When I retrieve the "apis" resource with id "thumbApiId"
    And I put the response payload in context as "thumbApiPayload"
    When I update the "apis" resource "thumbApiId" and "thumbApiPayload" with configuration type "description" and value:
      """
      Updated without touching the thumbnail
      """
    Then The response status code should be 200
    When I retrieve the thumbnail for API "thumbApiId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
