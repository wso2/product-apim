@cleanup
Feature: Admin Configurable Default Throttling Policy

  Ports ConfigurableDefaultPolicyTestCase. The capability is driven from TENANT CONFIGURATION, not from
  deployment.toml: the tenant-config keys DefaultAPILevelTier / DefaultSubscriptionLevelTier /
  DefaultApplicationLevelTier are read by APIUtil.getDefault{APILevel,Subscription,Application}Policy and take
  precedence over BOTH "Unlimited" and the fall-through to the first key of the tenant's policy TreeMap at that
  level. A configured name is honoured only if a policy with that name exists at that level.
  Each scenario therefore captures the tenant configuration, points a default at a policy that is neither
  Unlimited nor the fall-through (so the assertion can only pass if the tenant-config override was applied),
  asserts the observable outcome, and RESTORES the tenant configuration — a mutated tenant config would leak into
  the sibling unlimited_tier_disabled.feature, which asserts the fall-through values.
  Built-in policies are used as the configured defaults wherever possible (20KPerMin at API level, Gold at
  subscription level, 20PerMin at application level) so nothing has to be created and torn down; the one
  scenario that needs a custom policy is the delete guard, which exists precisely because a policy configured as
  a default must not be deletable.
  Runs in the IntegrationV2-UnlimitedTierDisabled block: the legacy class ran in the same server as
  UnlimitedTierDisabledTestCase, and it needs Unlimited disabled both to make the override distinguishable from
  the Unlimited default and for its "an Unlimited subscription policy is refused" leg (asserted once, in
  unlimited_tier_disabled.feature, since it is the same product check).
  All scenarios run x2 tenant — the tenant configuration is per tenant.

  # Rows 135/139 — the GraphQL leg. A GraphQL schema UPDATE re-derives the operation list from the new schema;
  # operations with no carried-over tier are filled from APIUtil.getDefaultAPILevelPolicy, i.e. the configured
  # DefaultAPILevelTier. NOTE: the graphql CREATE path resolves its default through
  # APIUtil.getDefaultThrottlingPolicy instead, which does NOT consult the tenant configuration — that
  # inconsistency is reported, not asserted here.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:ConfigurableDefaultPolicyTestCase
  Scenario Outline: GraphQL operations derived from a schema update take the configured default API-level tier as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "cdpGqlOriginal"
    And I register tenant configuration "cdpGqlOriginal" for cleanup
    And I capture the tenant configuration as "cdpGqlModified"
    And I set the field "DefaultAPILevelTier" to "20KPerMin" in the payload "cdpGqlModified"
    And I update the tenant configuration from "cdpGqlModified"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_default_tier_graphql_api.json" in context as "cdpGqlPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "cdpGqlPayload" as "cdpGqlApiId"
    Then The response status code should be 201
    When I update the GraphQL schema of API "cdpGqlApiId" with schema file "artifacts/payloads/updated_graphql_schema.graphql"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "cdpGqlApiId"
    Then The response status code should be 200
    And Every operation in the response should have throttling policy "20KPerMin"
    # Restore, so the sibling fall-through assertions and the policy sweep are unaffected.
    When I update the tenant configuration from "cdpGqlOriginal"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 136/140 — the REST leg: the configured DefaultAPILevelTier is substituted into every tier-less resource
  # on import, in place of the 10KPerMin fall-through that unlimited_tier_disabled.feature pins.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:ConfigurableDefaultPolicyTestCase
  Scenario Outline: A tier-less REST resource takes the configured default API-level tier as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "cdpRestOriginal"
    And I register tenant configuration "cdpRestOriginal" for cleanup
    And I capture the tenant configuration as "cdpRestModified"
    And I set the field "DefaultAPILevelTier" to "20KPerMin" in the payload "cdpRestModified"
    And I update the tenant configuration from "cdpRestModified"
    Then The response status code should be 200
    When I import openapi definition from "artifacts/payloads/OAS/default_tier_api_oas.json" with additional properties "artifacts/payloads/OAS/default_tier_api_props.json" as "cdpRestApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "cdpRestApiId"
    Then The response status code should be 200
    And Every operation in the response should have throttling policy "20KPerMin"
    When I retrieve the swagger of "apis" resource "cdpRestApiId"
    Then The response status code should be 200
    And The value of response field "$.paths['/resource'].get['x-throttling-tier']" should be "20KPerMin"
    And The value of response field "$.paths['/resource'].post['x-throttling-tier']" should be "20KPerMin"
    When I update the tenant configuration from "cdpRestOriginal"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 137/141 — the publisher settings document reflects BOTH configured defaults.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:ConfigurableDefaultPolicyTestCase
  Scenario Outline: The publisher settings advertise the configured default policies as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "cdpSettingsOriginal"
    And I register tenant configuration "cdpSettingsOriginal" for cleanup
    And I capture the tenant configuration as "cdpSettingsModified"
    And I set the field "DefaultAPILevelTier" to "20KPerMin" in the payload "cdpSettingsModified"
    And I set the field "DefaultSubscriptionLevelTier" to "Gold" in the payload "cdpSettingsModified"
    And I update the tenant configuration from "cdpSettingsModified"
    Then The response status code should be 200
    When I retrieve the publisher settings
    Then The response status code should be 200
    And The value of response field "defaultAdvancePolicy" should be "20KPerMin"
    And The value of response field "defaultSubscriptionPolicy" should be "Gold"
    When I update the tenant configuration from "cdpSettingsOriginal"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Rows 138/142 — the DefaultApplication the product auto-creates for a brand-new subscriber takes the
  # configured DefaultApplicationLevelTier (AbstractAPIManager.addDefaultApplicationForSubscriber). The
  # subscriber must be provisioned AFTER the tenant config is set, and its first devportal REST call is what
  # triggers the auto-creation (SubscriberRegistrationInterceptor).
  @cap:admin @feat:throttling-policies @type:regression @dep:devportal @legacy:ConfigurableDefaultPolicyTestCase
  Scenario Outline: A new subscriber's DefaultApplication takes the configured default application tier in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    When I capture the tenant configuration as "cdpAppOriginal"
    And I register tenant configuration "cdpAppOriginal" for cleanup
    And I capture the tenant configuration as "cdpAppModified"
    And I set the field "DefaultApplicationLevelTier" to "20PerMin" in the payload "cdpAppModified"
    And I update the tenant configuration from "cdpAppModified"
    Then The response status code should be 200
    And I provision user "cdpDefaultAppUser" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "<subscriberActor>"
    When I fetch the application with "DefaultApplication" as "cdpDefaultAppId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "list[0].throttlingPolicy" should be "20PerMin"
    # The auto-created application is a product side effect of this scenario, so remove it here.
    When I delete the application with id "cdpDefaultAppId"
    Then The response status code should be 200
    Given I act as "<adminActor>"
    When I update the tenant configuration from "cdpAppOriginal"
    Then The response status code should be 200

    Examples:
      | tenant       | adminActor        | subscriberActor               |
      | carbon.super | admin             | cdpDefaultAppUser             |
      | tenant1.com  | admin@tenant1.com | cdpDefaultAppUser@tenant1.com |

  # Capability completeness (legacy asserted this three times, in its @BeforeClass): once a policy is named as a
  # tenant default it becomes UNDELETABLE — ThrottlingApiServiceImpl calls APIUtil.checkPolicyConfiguredAsDefault
  # and throws a plain APIManagementException, which surfaces as 500. Un-configuring it makes it deletable again.
  @cap:admin @feat:throttling-policies @type:negative @legacy:ConfigurableDefaultPolicyTestCase
  Scenario Outline: An advanced policy configured as the tenant default cannot be deleted as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "cdpGuard${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I capture the tenant configuration as "cdpGuardOriginal"
    And I register tenant configuration "cdpGuardOriginal" for cleanup
    And I capture the tenant configuration as "cdpGuardModified"
    And I set the field "DefaultAPILevelTier" to "{{advThrottlePolicyName}}" in the payload "cdpGuardModified"
    And I update the tenant configuration from "cdpGuardModified"
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 500
    # Un-configure the default; the very same delete now succeeds.
    When I update the tenant configuration from "cdpGuardOriginal"
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
