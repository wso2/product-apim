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

  # Small self-contained publisher read endpoints (no shared config API needed).
  # (The linter-custom-rules round trip lives in publisher/api_lifecycle — it must UPDATE tenant-conf, and that
  # file already owns this block's only tenant-conf mutation, so co-locating serialises the two.)
  @cap:publisher @feat:api-config @type:smoke @legacy:APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase
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

  # The default SUBSCRIPTION tiers, asserted by NAME and quota rather than by list index. Ports APIM634, which
  # asserted count == 5 plus each tier's name/displayName/description positionally (list.get(0..4)). Here the
  # exact name SET is asserted alongside the count, so a failure says WHICH tier changed — a bare count is brittle
  # against the product adding a default tier and, on its own, tells you nothing about which one moved. Every
  # description is pinned exactly, so a silently re-quota'd default tier fails the test.
  @cap:publisher @feat:api-config @rule:throttling-tiers @type:regression @legacy:APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase
  Scenario Outline: The default subscription throttling tiers carry their exact names and quotas as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the publisher "subscription" throttling policies
    Then The response status code should be 200
    And The value of response field "count" should be "5"
    And The response field "list[*].name" should be exactly the list "Bronze,DefaultSubscriptionless,Gold,Silver,Unlimited"
    And The response field "list[?(@.name=='Bronze')].displayName" should be exactly the list "Bronze"
    And The response field "list[?(@.name=='Bronze')].description" should be exactly the list "Allows 1000 requests per minute"
    And The response field "list[?(@.name=='Gold')].displayName" should be exactly the list "Gold"
    And The response field "list[?(@.name=='Gold')].description" should be exactly the list "Allows 5000 requests per minute"
    And The response field "list[?(@.name=='Silver')].displayName" should be exactly the list "Silver"
    And The response field "list[?(@.name=='Silver')].description" should be exactly the list "Allows 2000 requests per minute"
    And The response field "list[?(@.name=='Unlimited')].displayName" should be exactly the list "Unlimited"
    And The response field "list[?(@.name=='Unlimited')].description" should be exactly the list "Allows unlimited requests"
    And The response field "list[?(@.name=='DefaultSubscriptionless')].displayName" should be exactly the list "DefaultSubscriptionless"
    And The response field "list[?(@.name=='DefaultSubscriptionless')].description" should be exactly the list "Allows 10000 requests per minute when subscription validation is disabled"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The STREAMING subscription policies — the nine event-count-quota tiers. Ports
  # APIMGetAllSubscriptionThrottlingPolicies.testGetAllSubscriptionThrottlingPoliciesByQuotaType. The endpoint
  # (/throttling-policies/streaming/subscription) IS the eventCount filter; it is a different resource from the
  # /throttling-policies/{policyLevel} read above, which has no quota-type dimension at all. See the step's
  # javadoc for why legacy's "quotaType=eventCount" argument never reached the wire.
  @cap:publisher @feat:api-config @rule:throttling-tiers @type:regression @legacy:APIMGetAllSubscriptionThrottlingPolicies
  Scenario Outline: The streaming subscription throttling policies carry their exact names and quotas as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the publisher streaming subscription throttling policies
    Then The response status code should be 200
    And The value of response field "count" should be "9"
    # NOTE the field name: this endpoint returns SubscriptionPolicyDTOs keyed on "policyName", NOT the
    # ThrottlingPolicyDTO "name" of the scenario above. Confirmed live — filtering on "name" here matches nothing.
    And The response field "list[*].policyName" should be exactly the list "AsyncBronze,AsyncGold,AsyncSilver,AsyncUnlimited,AsyncDefaultSubscriptionless,AsyncWHBronze,AsyncWHGold,AsyncWHSilver,AsyncWHUnlimited"
    And The response field "list[*].defaultLimit.type" should be exactly the list "EVENTCOUNTLIMIT"
    And The response field "list[?(@.policyName=='AsyncBronze')].displayName" should be exactly the list "AsyncBronze"
    And The response field "list[?(@.policyName=='AsyncBronze')].description" should be exactly the list "Allows 5000 events per day"
    And The response field "list[?(@.policyName=='AsyncBronze')].defaultLimit.eventCount.eventCount" should be exactly the list "5000"
    And The response field "list[?(@.policyName=='AsyncGold')].displayName" should be exactly the list "AsyncGold"
    And The response field "list[?(@.policyName=='AsyncGold')].description" should be exactly the list "Allows 50000 events per day"
    And The response field "list[?(@.policyName=='AsyncGold')].defaultLimit.eventCount.eventCount" should be exactly the list "50000"
    And The response field "list[?(@.policyName=='AsyncSilver')].displayName" should be exactly the list "AsyncSilver"
    And The response field "list[?(@.policyName=='AsyncSilver')].description" should be exactly the list "Allows 25000 events per day"
    And The response field "list[?(@.policyName=='AsyncSilver')].defaultLimit.eventCount.eventCount" should be exactly the list "25000"
    And The response field "list[?(@.policyName=='AsyncUnlimited')].displayName" should be exactly the list "AsyncUnlimited"
    And The response field "list[?(@.policyName=='AsyncUnlimited')].description" should be exactly the list "Allows unlimited events"
    And The response field "list[?(@.policyName=='AsyncDefaultSubscriptionless')].displayName" should be exactly the list "AsyncDefaultSubscriptionless"
    And The response field "list[?(@.policyName=='AsyncDefaultSubscriptionless')].description" should be exactly the list "Allows 10000 events per day when subscription validation is disabled"
    And The response field "list[?(@.policyName=='AsyncDefaultSubscriptionless')].defaultLimit.eventCount.eventCount" should be exactly the list "10000"
    # The four webhook (WH) tiers additionally cap active subscriptions. subscriberCount is pinned alongside the
    # description because the two DISAGREE for AsyncWHBronze — see the note under the scenario.
    And The response field "list[?(@.policyName=='AsyncWHBronze')].displayName" should be exactly the list "AsyncWHBronze"
    And The response field "list[?(@.policyName=='AsyncWHBronze')].description" should be exactly the list "Allows 1000 events per month and 500 active subscriptions"
    And The response field "list[?(@.policyName=='AsyncWHBronze')].subscriberCount" should be exactly the list "100"
    And The response field "list[?(@.policyName=='AsyncWHGold')].displayName" should be exactly the list "AsyncWHGold"
    And The response field "list[?(@.policyName=='AsyncWHGold')].description" should be exactly the list "Allows 10000 events per month and 1000 active subscriptions"
    And The response field "list[?(@.policyName=='AsyncWHGold')].subscriberCount" should be exactly the list "1000"
    And The response field "list[?(@.policyName=='AsyncWHSilver')].displayName" should be exactly the list "AsyncWHSilver"
    And The response field "list[?(@.policyName=='AsyncWHSilver')].description" should be exactly the list "Allows 5000 events per month and 500 active subscriptions"
    And The response field "list[?(@.policyName=='AsyncWHSilver')].subscriberCount" should be exactly the list "500"
    And The response field "list[?(@.policyName=='AsyncWHUnlimited')].displayName" should be exactly the list "AsyncWHUnlimited"
    And The response field "list[?(@.policyName=='AsyncWHUnlimited')].description" should be exactly the list "Allows unlimited events and unlimited active subscriptions"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An API's own gatewayType field (wso2/synapse vs wso2/apk) — set at create, surviving a revision deploy, and
  # reflected on re-fetch. Ports APICreationTestCase.testCreateAndDeployApiWithGatewayType. NOTE this is the
  # API-level field: admin/gateway_environments.feature:42 sets a gateway type on an ENVIRONMENT, a different
  # resource. The revision deploy is part of the legacy assertion (it asserted a non-null revision id), so it is
  # kept: it proves the field does not block deployment.
  @cap:publisher @feat:api-config @rule:gateway-type @type:regression @legacy:APICreationTestCase
  Scenario Outline: An API created with gatewayType <gatewayType> reports it after deployment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "gwTypeApiPayload"
    And I set the field "gatewayType" to "<gatewayType>" in the payload "gwTypeApiPayload"
    And I create an "apis" resource with payload "gwTypeApiPayload" as "gwTypeApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "gwTypeRevPayload"
    """
    {"description":"revision for gatewayType check"}
    """
    And I make a request to create a revision for "apis" resource "gwTypeApiId" with payload "gwTypeRevPayload"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "apis" resource "gwTypeApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gwTypeApiId"
    Then The response status code should be 200
    And The value of response field "gatewayType" should be "<gatewayType>"

    Examples:
      | gatewayType   | actor                     |
      | wso2/apk      | publisherUser             |
      | wso2/apk      | publisherUser@tenant1.com |
      | wso2/synapse  | publisherUser             |
      | wso2/synapse  | publisherUser@tenant1.com |

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

  # The publisher endpoint-validation API probes a backend endpoint and reports its reachability. The target is
  # the APIM's own Carbon Version service, reachable from the server itself (no separate backend needed). Verified
  # live on 4.7.0: a reachable endpoint validates with statusCode 202 (Accepted) — the auth-protected sample
  # webapp instead reports its 401, so 202 is the healthy/reachable signal. Ports APIMANAGER2611.
  @cap:publisher @feat:api-config @type:regression @legacy:APIMANAGER2611EndpointValidationTestCase
  Scenario Outline: A reachable backend endpoint validates successfully as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "valApiPayload"
    And I create an "apis" resource with payload "valApiPayload" as "valApiId"
    When I validate the endpoint "https://localhost:9443/services/Version" for API "valApiId"
    Then The response status code should be 200
    And The response should contain "\"statusCode\":202"
    And The response should contain "Accepted"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Edit an API's metadata (tags + description) and confirm the new values persist on re-fetch. Ports
  # EditAPIAndCheckUpdatedInformationTestCase. Self-contained (creates its own API); torn down by the runner sweep.
  @cap:publisher @feat:api-config @rule:metadata @type:regression @legacy:EditAPIAndCheckUpdatedInformationTestCase
  Scenario Outline: Update a REST API's tags and description persist as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "editTag"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "editApiPayload"
    And I create an "apis" resource with payload "editApiPayload" as "editApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "editApiId"
    And I put the response payload in context as "editApiFull"
    # Append a new tag and change the description in one update.
    And I set the field "description" to "This is test API - New Description" in the payload "editApiFull"
    And I update the "apis" resource "editApiId" and "editApiFull" with configuration type "tags" and value:
    """
    ["tag18-1","tag18-2","tag18-3","{{editTag}}"]
    """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "editApiId"
    Then The response status code should be 200
    And The response should contain "This is test API - New Description"
    And The response should contain "{{editTag}}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An API update that nulls optional fields (securityScheme, then endpointConfig) is ACCEPTED (200), not a 400 or
  # a server error — a regression guard against a past NullPointerException. Ports UpdateAPINullPointerTestCase
  # (whose method names say "BadRequest" but assert 200 — the true subject is null-field ACCEPTANCE).
  @cap:publisher @feat:api-config @rule:null-fields @type:regression @legacy:UpdateAPINullPointerTestCase
  Scenario Outline: An API update that nulls optional fields is accepted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "nullApiPayload"
    And I create an "apis" resource with payload "nullApiPayload" as "nullApiId"
    Then The response status code should be 201
    # Null securityScheme -> update accepted.
    When I retrieve the "apis" resource with id "nullApiId"
    And I put the response payload in context as "nullApiFull1"
    And I set the field "securityScheme" to null in the payload "nullApiFull1"
    And I update "apis" resource of id "nullApiId" with payload "nullApiFull1"
    Then The response status code should be 200
    # Observable state (pinned live): a null securityScheme is NOT applied — the server retains/re-defaults the
    # scheme set, so the retrieved API still carries the default schemes.
    When I retrieve the "apis" resource with id "nullApiId"
    And I extract response field "securityScheme" and store it as "nullApiPostSS"
    Then the actual value of "nullApiPostSS" should match the expected value:
    """
    ["oauth_basic_auth_api_key_mandatory","oauth2"]
    """
    # Null endpointConfig -> update accepted.
    When I retrieve the "apis" resource with id "nullApiId"
    And I put the response payload in context as "nullApiFull2"
    And I set the field "endpointConfig" to null in the payload "nullApiFull2"
    And I update "apis" resource of id "nullApiId" with payload "nullApiFull2"
    Then The response status code should be 200
    # Observable state (pinned live): unlike securityScheme, a null endpointConfig IS applied — the retrieved API
    # carries no endpoint configuration any more (the create payload's production/sandbox endpoints are gone).
    When I retrieve the "apis" resource with id "nullApiId"
    Then The response status code should be 200
    And The response should not contain "production_endpoints"
    And The response should not contain "sandbox_endpoints"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # A load-balanced API's endpoint configuration is retrievable in full: the Publisher API reflects the
  # load_balance endpoint type, the RoundRobin algorithm and all four production + four sandbox endpoints, and
  # once published+deployed the DevPortal exposes the gateway endpoint URLs. Ports APIM720GetAllEndPointsTestCase.
  @cap:publisher @feat:api-config @rule:endpoint-listing @type:regression @dep:devportal @legacy:APIM720GetAllEndPointsTestCase
  Scenario Outline: A load-balanced API exposes its endpoint configuration and gateway URLs as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_endpoint_listing_api.json" as "lbApiId" and deployed it
    When I publish the "apis" resource with id "lbApiId"
    Then The lifecycle status of API "lbApiId" should be "Published"
    # Publisher reflects the load-balanced endpoint config: type, algorithm and all four production/sandbox endpoints.
    When I retrieve the "apis" resource with id "lbApiId"
    Then The response status code should be 200
    And The response should contain "load_balance"
    And The response should contain "org.apache.synapse.endpoints.algorithms.RoundRobin"
    And The response should contain "prod0"
    And The response should contain "prod3"
    And The response should contain "sand3"
    # The DevPortal exposes the API's gateway endpoint URLs once it is deployed.
    When I extract response field "context" and store it as "lbApiContext"
    Then I retrieve the devportal API "lbApiId" until it contains "{{lbApiContext}}/1.0.0" within 60 seconds
    And The response should contain "endpointURLs"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Publisher-plane contract for BACKEND endpoint security (the management-plane half of the endpoint-security
  # legacy suite — the gateway-injection half lives in gateway/security_enforcement). Ports the publisher-observable
  # assertions of AddEndPointSecurityPerTypeTestCase (create-path) + ChangeEndPointSecurityPerTypeTestCase
  # (update-path): on create/update the Publisher API stores per-type (production/sandbox) endpoint_security, echoes
  # back the non-secret fields (type, username / clientId, tokenUrl, enabled) and REDACTS the secret — the stored
  # password / clientSecret is returned as "" and never in plaintext. Pure publisher-plane: no deploy, no gateway
  # invocation. Runs in both tenants (×2).

  # BASIC, both production and sandbox with DISTINCT credentials: on create the response preserves both usernames
  # and the BASIC type and redacts both passwords (prodPass / sandPass never echoed).
  @cap:publisher @feat:api-config @rule:endpoint-security @type:regression @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Creating an API with per-type BASIC endpoint security redacts the stored passwords as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_endpoint_sec_both.json" in context as "epcbPayload"
    And I create an "apis" resource with payload "epcbPayload" as "epcbApiId"
    Then The response status code should be 201
    # Non-secret fields round-trip on the create response.
    And The response should contain "prodUser"
    And The response should contain "sandUser"
    And The response should contain "BASIC"
    # Secrets are redacted — never returned in plaintext.
    And The response should not contain "prodPass"
    And The response should not contain "sandPass"
    # Re-fetch confirms the persisted API also redacts the secrets.
    When I retrieve the "apis" resource with id "epcbApiId"
    Then The response status code should be 200
    And The response should contain "prodUser"
    And The response should contain "sandUser"
    And The response should not contain "prodPass"
    And The response should not contain "sandPass"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # BASIC update path: change the production credential via GET-mutate-PUT of endpointConfig; the update response
  # reflects the NEW username, keeps redacting the password, and the old credential is gone. Publisher-plane only.
  @cap:publisher @feat:api-config @rule:endpoint-security @type:regression @legacy:ChangeEndPointSecurityPerTypeTestCase
  Scenario Outline: Updating an API's BASIC endpoint-security credential reflects the change and keeps the secret redacted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_endpoint_sec_change.json" in context as "epubPayload"
    And I create an "apis" resource with payload "epubPayload" as "epubApiId"
    Then The response status code should be 201
    And The response should contain "prodInit"
    And The response should not contain "prodInitPass"
    # Update the production endpoint_security to a NEW credential (GET-mutate-PUT).
    When I retrieve the "apis" resource with id "epubApiId"
    And I put the response payload in context as "epubFull"
    When I put the following JSON payload in context as "epubNewEndpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"},"endpoint_security":{"production":{"enabled":true,"type":"BASIC","username":"prodChanged","password":"prodChangedPass"},"sandbox":{"enabled":true,"type":"BASIC","username":"sandInit","password":"sandInitPass"}}}
    """
    When I update the "apis" resource "epubApiId" and "epubFull" with configuration type "endpointConfig" and value:
    """
    epubNewEndpoint
    """
    Then The response status code should be 200
    # The update response reflects the NEW username, still redacts the password, and no longer carries the old one.
    And The response should contain "prodChanged"
    And The response should not contain "prodChangedPass"
    And The response should not contain "prodInit"
    # Re-fetch confirms persistence.
    When I retrieve the "apis" resource with id "epubApiId"
    Then The response status code should be 200
    And The response should contain "prodChanged"
    And The response should not contain "prodChangedPass"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # OAUTH (client_credentials), both production and sandbox: on create the response round-trips type=OAUTH, the
  # tokenUrl and the clientId, and REDACTS the clientSecret. Ports the publisher-observable assertions of
  # AddEndPointSecurityPerTypeTestCase#testAddEndpointSecurityForOauthForClientCredentialsGrantType (the clientId
  # is preserved, clientSecret comes back ""). Publisher-plane only — the gateway token-fetch/injection is proven
  # separately in gateway/security_enforcement.
  @cap:publisher @feat:api-config @rule:endpoint-security @type:regression @legacy:AddEndPointSecurityPerTypeTestCase
  Scenario Outline: Creating an API with per-type OAUTH endpoint security round-trips the config and redacts the client secret as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_endpoint_sec_oauth.json" in context as "epoauthPayload"
    And I create an "apis" resource with payload "epoauthPayload" as "epoauthApiId"
    Then The response status code should be 201
    And The response should contain "OAUTH"
    And The response should contain "epProdClientId0001"
    And The response should contain "epSandClientId0002"
    And The response should contain "https://localhost:9443/oauth2/token"
    # The client secrets are redacted — never returned in plaintext.
    And The response should not contain "epProdClientSecret0001"
    And The response should not contain "epSandClientSecret0002"
    When I retrieve the "apis" resource with id "epoauthApiId"
    Then The response status code should be 200
    And The response should contain "epProdClientId0001"
    And The response should not contain "epProdClientSecret0001"
    And The response should not contain "epSandClientSecret0002"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # DIGEST endpoint security: the Publisher DTO accepts a DIGEST type (basic or digest) in addition to BASIC/OAUTH.
  # On create the response round-trips type=DIGEST and the usernames and REDACTS the passwords. Publisher-plane
  # contract only (there is no digest backend in the harness, so gateway injection is out of scope here).
  @cap:publisher @feat:api-config @rule:endpoint-security @type:regression
  Scenario Outline: Creating an API with per-type DIGEST endpoint security round-trips the type and redacts the passwords as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_endpoint_sec_digest.json" in context as "epdigestPayload"
    And I create an "apis" resource with payload "epdigestPayload" as "epdigestApiId"
    Then The response status code should be 201
    And The response should contain "DIGEST"
    And The response should contain "digestProdUser"
    And The response should contain "digestSandUser"
    And The response should not contain "digestProdPass"
    And The response should not contain "digestSandPass"
    When I retrieve the "apis" resource with id "epdigestApiId"
    Then The response status code should be 200
    And The response should contain "DIGEST"
    And The response should not contain "digestProdPass"
    And The response should not contain "digestSandPass"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
