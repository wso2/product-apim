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
