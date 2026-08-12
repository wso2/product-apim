@cleanup
Feature: Admin Unlimited Tier Disabled

  Ports UnlimitedTierDisabledTestCase. Runs in the IntegrationV2-UnlimitedTierDisabled block, whose overlay sets
  the single key that drives the whole capability — "[apim.throttling] enable_unlimited_tier = false", which the
  product reads back through ThrottleProperties.isEnableUnlimitedTier(). With Unlimited disabled the product must
  (a) stop OFFERING it: naming Unlimited as an API subscription policy, as a resource-level x-throttling-tier or
  as an application tier is refused with 400 and error code 900305 (ExceptionCodes.TIER_NAME_INVALID), and
  (b) stop DEFAULTING to it: every tier-less resource, and the defaults the publisher settings advertise, fall
  through to the first key of a TreeMap of the tenant's policies at that level — "10KPerMin" at API/resource
  level, since the shipped advanced policies are 10KPerMin / 20KPerMin / 50KPerMin.
  The legacy class asserted only "not Unlimited" for the substituted tier; these scenarios pin the exact
  substituted value instead, so a change of substitution rule cannot pass unnoticed.
  Every scenario runs x2 tenant (the flag is server-global but each tenant resolves its own policy set).
  Sibling feature configurable_default_policy.feature shares this block; see testng-v2.xml for why that is safe.

  # Rows 143/149 — the GraphQL leg. A GraphQL API created without an explicit operations list gets the product's
  # default operations, whose tier comes from APIUtil.getDefaultThrottlingPolicy → the resource-level fall-through.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:UnlimitedTierDisabledTestCase
  Scenario Outline: A GraphQL API's operations fall back to the next available tier when Unlimited is disabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_default_tier_graphql_api.json" in context as "utdGqlPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "utdGqlPayload" as "utdGqlApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "utdGqlApiId"
    Then The response status code should be 200
    And Every operation in the response should have throttling policy "10KPerMin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 144/150 — the REST leg: substitution on import, visible on both the API's operations and the stored
  # definition, plus the refusal of Unlimited as a subscription policy on a subsequent API update.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:UnlimitedTierDisabledTestCase
  Scenario Outline: A tier-less REST resource falls back to the next available tier and Unlimited is refused as a subscription policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/default_tier_api_oas.json" with additional properties "artifacts/payloads/OAS/default_tier_api_props.json" as "utdRestApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "utdRestApiId"
    Then The response status code should be 200
    And Every operation in the response should have throttling policy "10KPerMin"
    And I put the response payload in context as "utdRestApiPayload"
    # The substituted tier is persisted into the stored OpenAPI definition as x-throttling-tier.
    When I retrieve the swagger of "apis" resource "utdRestApiId"
    Then The response status code should be 200
    And The value of response field "$.paths['/resource'].get['x-throttling-tier']" should be "10KPerMin"
    And The value of response field "$.paths['/resource'].post['x-throttling-tier']" should be "10KPerMin"
    # Unlimited is no longer a defined tier, so offering it as a business plan is rejected outright.
    When I update the "apis" resource "utdRestApiId" and "utdRestApiPayload" with configuration type "policies" and value:
    """
    ["Silver","Unlimited"]
    """
    Then The response status code should be 400
    And The value of error response field "code" should be "900305"
    And The value of error response field "message" should be "The tier name is invalid."

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 145/151 — substitution on a DEFINITION UPDATE, not just on import: the newly added tier-less resource
  # picks up the fall-through tier while the explicitly-tiered resources are left exactly as declared.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:UnlimitedTierDisabledTestCase
  Scenario Outline: A resource added by a definition update picks up the next available tier as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/default_tier_api_oas.json" with additional properties "artifacts/payloads/OAS/default_tier_api_props.json" as "utdUpdApiId"
    Then The response status code should be 201
    When I update the swagger of "apis" resource "utdUpdApiId" from file "artifacts/payloads/OAS/default_tier_api_oas_updated.json"
    Then The response status code should be 200
    When I retrieve the swagger of "apis" resource "utdUpdApiId"
    Then The response status code should be 200
    And The value of response field "$.paths['/added'].get['x-throttling-tier']" should be "10KPerMin"
    # The pre-existing resources declared 50KPerMin explicitly — substitution must not overwrite a declared tier.
    And The value of response field "$.paths['/resource'].get['x-throttling-tier']" should be "50KPerMin"
    And The value of response field "$.paths['/resource'].post['x-throttling-tier']" should be "50KPerMin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 146/152 — an explicit Unlimited RESOURCE tier in the imported definition is refused
  # (APIProviderImpl.checkResourceThrottlingTiersInURITemplates → ExceptionCodes.TIER_NAME_INVALID).
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @legacy:UnlimitedTierDisabledTestCase
  Scenario Outline: Importing a definition whose resource declares the Unlimited tier is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to import openapi definition from "artifacts/payloads/OAS/unlimited_resource_tier_api_oas.json" with additional properties from "artifacts/payloads/OAS/default_tier_api_props.json"
    Then The response status code should be 400
    And The value of error response field "code" should be "900305"
    And The value of error response field "message" should be "The tier name is invalid."

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 147/153 — an Unlimited APPLICATION tier is refused (APIConsumerImpl → TIER_NAME_INVALID).
  @cap:admin @feat:throttling-policies @type:negative @dep:devportal @legacy:UnlimitedTierDisabledTestCase
  Scenario Outline: Creating an application on the Unlimited tier is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put the following JSON payload in context as "utdUnlimitedApp"
    """
    {"name":"${UNIQUE:UtdUnlimitedApp}","throttlingPolicy":"Unlimited","description":"Unlimited-tier-disabled negative","tokenType":"JWT"}
    """
    And I attempt to create an application with payload "utdUnlimitedApp"
    Then The response status code should be 400
    And The value of error response field "code" should be "900305"
    And The value of error response field "message" should be "The tier name is invalid."

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 148/154 — the publisher settings document must not advertise Unlimited as either default; it advertises
  # the fall-through instead (SettingsMappingUtil → getDefaultAPILevelPolicy / getDefaultSubscriptionPolicy).
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:UnlimitedTierDisabledTestCase
  Scenario Outline: The publisher settings advertise the fall-through defaults, not Unlimited, as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the publisher settings
    Then The response status code should be 200
    And The value of response field "defaultAdvancePolicy" should be "10KPerMin"
    And The value of response field "defaultSubscriptionPolicy" should be "AIBronze"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
