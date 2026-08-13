@cleanup
Feature: Cross-tenant subscription and discovery

  Ports the discovery + subscribe + tenant-isolation facets of the legacy CrossTenantSubscriptionTestCase (and the
  workflow-tenant facet of CrossTenantSubscriptionUpdateTestCase). An API published in a PROVIDER tenant with
  subscriptionAvailability=ALL_TENANTS is discoverable and subscribable from a CONSUMER tenant when
  [apim.devportal] enable_cross_tenant_subscriptions=true (supplied by this block's tomlExtraOverlayPath). Every
  cross-tenant devportal operation carries the tenant it addresses in the X-WSO2-Tenant header (the legacy
  RestAPIStoreImpl overloads' tenantDomain argument, declared as the requestedTenant parameter on the DevPortal
  API): API listing and API GET, key-manager listing, an application's oauth-keys and subscriptions, and the
  subscribe/update POSTs. The application-policy listing is the ONE exception — it accepts the header and
  ignores it; see that scenario.

  Each scenario runs in BOTH directions where direction is observable, via an Examples row per (provider,
  consumer) pair — carbon.super-provider/tenant1.com-consumer and the mirror — because a tenant-isolation rule
  can hold one way and leak the other (the super tenant is not symmetric with a sub-tenant in the product's
  tenant-flow handling).

  The ISOLATION half of each listing is asserted as a precise NEGATIVE (the OTHER tenant's uniquely-named
  resource must be ABSENT), paired with the positive on the same call. That pair is what the legacy count==1
  assertions were really expressing, and unlike a total count it is stable: other scenarios in this block create
  APIs and policies in the same two tenants, so any absolute count would be a moving target. Every negative is
  paired with the same assertion made against the caller's OWN tenant, so an absent-because-nothing-was-visible
  outcome cannot pass vacuously.

  The runtime half — minting a token for a cross-tenant subscription and INVOKING the provider tenant's API at
  the gateway — is the gateway plane's subject and lives in gateway/cross_tenant_invocation (it must use the
  PROVIDER tenant's key manager). Teardown via the per-scenario cleanup hook.

  @cap:devportal @feat:subscribe @type:smoke @dep:admin @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario Outline: A consumer-tenant application subscribes to an ALL_TENANTS API published in tenant <providerTenant>
    Given The system is ready
    # Provider tenant: publish an ALL_TENANTS API as the tenant admin.
    And I have valid access tokens as "<providerAdmin>"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "providerApiId" and deployed it
    When I publish the "apis" resource with id "providerApiId"
    Then The lifecycle status of API "providerApiId" should be "Published"

    # Consumer tenant: the API is discoverable across the boundary.
    When The system is ready and I have valid devportal access token as "<consumer>"
    And I list DevPortal APIs in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "{{providerApiId}}"
    When I retrieve DevPortal API "providerApiId" in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "{{providerApiId}}"

    # Consumer tenant: create an application and subscribe cross-tenant on the public Bronze tier.
    When I create an application "${UNIQUE:ctConsumerApp}" with visibility "PRIVATE" as "consumerAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "providerApiId" using application "consumerAppId" with payload "ctSub" in provider tenant "<providerTenant>"
    Then The response status code should be 201

    Examples:
      | providerTenant | providerAdmin     | consumer                   |
      | carbon.super   | admin             | subscriberUser@tenant1.com |
      | tenant1.com    | admin@tenant1.com | subscriberUser             |

  # The isolation half of cross-tenant discovery, which the containment assertion above cannot express: the
  # listing addressed at the provider tenant must carry the provider's API and NOT the consumer's own — even
  # when the consumer's own API is itself PUBLIC and ALL_TENANTS, i.e. the likeliest thing to leak. The
  # symmetric pair (each API present in its own tenant's listing, absent from the other's) is what makes the
  # negative falsifiable rather than trivially true.
  @cap:devportal @feat:discovery @rule:cross-tenant @type:regression @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario Outline: The cross-tenant API listing carries only tenant <providerTenant>'s APIs
    Given The system is ready
    And I have valid access tokens as "<providerAdmin>"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "leakProviderApiId" and deployed it
    When I publish the "apis" resource with id "leakProviderApiId"
    Then The lifecycle status of API "leakProviderApiId" should be "Published"

    # The consumer tenant's OWN ALL_TENANTS API — the leak probe.
    When I have valid access tokens as "<consumerAdmin>"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "leakConsumerApiId" and deployed it
    When I publish the "apis" resource with id "leakConsumerApiId"
    Then The lifecycle status of API "leakConsumerApiId" should be "Published"

    When The system is ready and I have valid devportal access token as "<consumer>"
    And I list DevPortal APIs in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "{{leakProviderApiId}}"
    And The response should not contain "{{leakConsumerApiId}}"

    # Addressed at the consumer's own tenant the membership is exactly reversed — so the absence asserted above
    # is genuine filtering, not the consumer API simply never becoming devportal-visible.
    When I list DevPortal APIs in tenant "<consumerTenant>"
    Then The response status code should be 200
    And The response should contain "{{leakConsumerApiId}}"
    And The response should not contain "{{leakProviderApiId}}"

    Examples:
      | providerTenant | consumerTenant | providerAdmin     | consumerAdmin     | consumer                   |
      | carbon.super   | tenant1.com    | admin             | admin@tenant1.com | subscriberUser@tenant1.com |
      | tenant1.com    | carbon.super   | admin@tenant1.com | admin             | subscriberUser             |

  @cap:devportal @feat:subscribe @type:negative @dep:admin @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario Outline: A cross-tenant subscribe on a role-restricted tier of tenant <providerTenant> the consumer lacks is refused
    Given The system is ready
    # Provider tenant: create a role-restricted subscription tier and publish an ALL_TENANTS API offering it.
    And I have valid access tokens as "<providerAdmin>"
    And I create a subscription throttling policy "${UNIQUE:CtRestricted}" allowing 10 requests per minute restricted to role "ctprovideronly"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "restrictedApiId" and deployed it
    When I retrieve the "apis" resource with id "restrictedApiId"
    And I put the response payload in context as "restrictedApiPayload"
    And I update the "apis" resource "restrictedApiId" and "restrictedApiPayload" with configuration type "policies" and value:
    """
    ["Bronze","Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "restrictedApiId"
    Then The lifecycle status of API "restrictedApiId" should be "Published"

    # Consumer tenant: wait for the API to cross the boundary (visibility propagates asynchronously), then read
    # the API. The restricted tier is FILTERED OUT of the tiers the consumer is offered, because it lacks the
    # provider-only role — the discovery-side half of the same rule the subscribe below is refused by.
    When The system is ready and I have valid devportal access token as "<consumer>"
    And I list DevPortal APIs in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "{{restrictedApiId}}"
    When I retrieve DevPortal API "restrictedApiId" in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "Bronze"
    And The response should not contain "{{subThrottlePolicyName}}"

    # ... and subscribing on it anyway is refused (403), not silently downgraded.
    When I create an application "${UNIQUE:ctRestrictedApp}" with visibility "PRIVATE" as "restrictedConsumerAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctRestrictedSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "restrictedApiId" using application "restrictedConsumerAppId" with payload "ctRestrictedSub" in provider tenant "<providerTenant>"
    Then The response status code should be 403

    Examples:
      | providerTenant | providerAdmin     | consumer                   |
      | carbon.super   | admin             | subscriberUser@tenant1.com |
      | tenant1.com    | admin@tenant1.com | subscriberUser             |

  @cap:devportal @feat:subscribe @type:negative @dep:admin @legacy:CrossTenantSubscriptionTestCase
  Scenario Outline: Creating an application with tenant <providerTenant>'s application policy is refused
    Given The system is ready
    # Provider tenant: create a tenant-local application throttling policy.
    And I have valid access tokens as "<providerAdmin>"
    And I create an application throttling policy "${UNIQUE:CtProviderAppPolicy}" allowing 5 requests per minute
    Then The response status code should be 201

    # Consumer tenant: an application referencing the provider tenant's application policy is refused (400).
    # A fixed name is safe here — the create is rejected at policy validation, so nothing persists to collide.
    When The system is ready and I have valid devportal access token as "<consumer>"
    And I put the following JSON payload in context as "ctWrongPolicyApp"
    """
    {"name": "ctWrongPolicyApp", "throttlingPolicy": "{{appThrottlePolicyName}}", "description": "cross-tenant wrong-policy app"}
    """
    And I attempt to create an application with payload "ctWrongPolicyApp"
    Then The response status code should be 400

    Examples:
      | providerTenant | providerAdmin     | consumer                   |
      | carbon.super   | admin             | subscriberUser@tenant1.com |
      | tenant1.com    | admin@tenant1.com | subscriberUser             |

  # The rule the 400 above is the consequence of, and the counter-example to every other listing in this file:
  # the devportal application-policy listing is NOT tenant-addressable. It always answers with the CALLER's own
  # tenant's plans, so another tenant's application policy is never offered — which is exactly why an
  # application referencing one is rejected. MEASURED (run2, both directions): addressed at the provider tenant
  # the listing returned the CONSUMER's policy and not the provider's. The mechanism is in the resource method
  # itself — ThrottlingPoliciesApiServiceImpl.throttlingPoliciesPolicyLevelGet accepts the xWSO2Tenant
  # parameter and never reads it, resolving `organization` from the message context instead (contrast
  # ApisApiServiceImpl.apisGet, which passes it through). The legacy assertions say the same thing and are the
  # reason this is encoded as behaviour rather than filed as a defect: CrossTenantSubscriptionTestCase
  # #testApplicationPolicyAvailabilityInTenant2 calls getApplicationPolicies(tenant1Name) as a TENANT2 user and
  # asserts Tenant2AppPolicy (its own) present, Tenant1AppPolicy (the addressed tenant's) absent.
  #
  # Both legs below therefore assert the SAME membership: the header changes nothing. The negative is what
  # carries the weight — it fires if the addressed tenant's policy ever leaks in.
  @cap:devportal @feat:applications @rule:cross-tenant @type:regression @dep:admin @legacy:CrossTenantSubscriptionTestCase
  Scenario Outline: The devportal application-policy listing stays on the caller's own tenant when tenant <providerTenant> is addressed
    Given The system is ready
    And I have valid access tokens as "<providerAdmin>"
    And I create an application throttling policy "${UNIQUE:CtPolicyProvider}" allowing 5 requests per minute
    Then The response status code should be 201
    And I copy context value "appThrottlePolicyName" to "ctProviderAppPolicyName"

    When I have valid access tokens as "<consumerAdmin>"
    And I create an application throttling policy "${UNIQUE:CtPolicyConsumer}" allowing 5 requests per minute
    Then The response status code should be 201
    And I copy context value "appThrottlePolicyName" to "ctConsumerAppPolicyName"

    When The system is ready and I have valid devportal access token as "<consumer>"
    And I retrieve DevPortal "application" throttling policies in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "{{ctConsumerAppPolicyName}}"
    And The response should not contain "{{ctProviderAppPolicyName}}"

    # Unchanged when addressed at its own tenant — proving the membership above is the caller's own tenant and
    # not an empty/degraded response that would satisfy the negative vacuously.
    When I retrieve DevPortal "application" throttling policies in tenant "<consumerTenant>"
    Then The response status code should be 200
    And The response should contain "{{ctConsumerAppPolicyName}}"
    And The response should not contain "{{ctProviderAppPolicyName}}"

    Examples:
      | providerTenant | consumerTenant | providerAdmin     | consumerAdmin     | consumer                   |
      | carbon.super   | tenant1.com    | admin             | admin@tenant1.com | subscriberUser@tenant1.com |
      | tenant1.com    | carbon.super   | admin@tenant1.com | admin             | subscriberUser             |

  # Key managers are tenant-local too, and the devportal listing is how a cross-tenant consumer discovers the
  # PROVIDER tenant's key manager (the one it must generate keys against to invoke — see
  # gateway/cross_tenant_invocation). A custom key manager is registered in EACH tenant so the listing has
  # something tenant-specific to filter on: the Resident Key Manager exists in every tenant under the same name
  # and therefore cannot distinguish "the provider's list" from "my own".
  @cap:admin @feat:key-manager-config @rule:cross-tenant @type:regression @dep:devportal @legacy:CrossTenantSubscriptionTestCase
  Scenario Outline: The cross-tenant key-manager listing carries only tenant <providerTenant>'s key managers
    Given The system is ready
    And I have valid access tokens as "<providerAdmin>"
    And I create a key manager from payload "artifacts/payloads/keymanagers/auth0.json" as "ctProviderKm"

    When I have valid access tokens as "<consumerAdmin>"
    And I create a key manager from payload "artifacts/payloads/keymanagers/okta.json" as "ctConsumerKm"

    When The system is ready and I have valid devportal access token as "<consumer>"
    And I list DevPortal key managers in tenant "<providerTenant>"
    Then The response status code should be 200
    And The response should contain "{{ctProviderKmName}}"
    And The response should not contain "{{ctConsumerKmName}}"

    # Reversed when addressed at the caller's own tenant.
    When I list DevPortal key managers in tenant "<consumerTenant>"
    Then The response status code should be 200
    And The response should contain "{{ctConsumerKmName}}"
    And The response should not contain "{{ctProviderKmName}}"

    Examples:
      | providerTenant | consumerTenant | providerAdmin     | consumerAdmin     | consumer                   |
      | carbon.super   | tenant1.com    | admin             | admin@tenant1.com | subscriberUser@tenant1.com |
      | tenant1.com    | carbon.super   | admin@tenant1.com | admin             | subscriberUser             |

  # One application can hold a key mapping per key manager, and key managers are tenant-local — so the
  # oauth-keys listing of a SINGLE application returns a different set depending on the tenant addressed. Both
  # halves are asserted on the same application in one scenario: the pair IS the behaviour, and either half
  # alone would pass just as well against a completely unfiltered list.
  @cap:key-manager @feat:oauth-keys @rule:cross-tenant @type:regression @dep:devportal @legacy:CrossTenantSubscriptionTestCase
  Scenario: An application's OAuth key mappings are filtered by the tenant addressed
    Given The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    When I create an application "${UNIQUE:ctKeysApp}" with visibility "PRIVATE" as "ctKeysAppId"
    Then The response status code should be 201

    # A key mapping against the consumer's OWN resident key manager.
    When I put the following JSON payload in context as "ctOwnKmKeys"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "ctKeysAppId" with payload "ctOwnKmKeys"
    Then The response status code should be 200
    And I extract response field "keyMappingId" and store it as "ctOwnKmKeyMappingId"

    # ... and one against the PROVIDER tenant's resident key manager, discovered across the boundary. It has to
    # be addressed by UUID: the NAME "Resident Key Manager" exists in every tenant, so a name lookup would
    # resolve to the consumer's own.
    When I list DevPortal key managers in tenant "carbon.super"
    Then The response status code should be 200
    And I capture the id of the list entry named "Resident Key Manager" as "ctProviderResidentKmId"
    When I put the following JSON payload in context as "ctProviderKmKeys"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{ctProviderResidentKmId}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "ctKeysAppId" with payload "ctProviderKmKeys"
    Then The response status code should be 200
    And I extract response field "keyMappingId" and store it as "ctProviderKmKeyMappingId"

    When I retrieve existing application keys for "ctKeysAppId" in tenant "tenant1.com"
    Then The response status code should be 200
    And The response should contain "{{ctOwnKmKeyMappingId}}"
    And The response should not contain "{{ctProviderKmKeyMappingId}}"

    When I retrieve existing application keys for "ctKeysAppId" in tenant "carbon.super"
    Then The response status code should be 200
    And The response should contain "{{ctProviderKmKeyMappingId}}"
    And The response should not contain "{{ctOwnKmKeyMappingId}}"

  # An application subscribed both inside its own tenant and across the boundary reports a DIFFERENT
  # subscription list and a DIFFERENT subscriptionCount per tenant addressed. The exact count is asserted (not
  # containment): the application is created by this scenario and holds exactly two subscriptions, one per
  # tenant, so 1-per-tenant is a stable exact number and an unfiltered count would read 2.
  @cap:devportal @feat:subscription-management @rule:cross-tenant @type:regression @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario: An application's subscription list and count are scoped to the tenant addressed
    Given The system is ready
    # Provider tenant (carbon.super): an ALL_TENANTS API.
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "ctSubsProviderApiId" and deployed it
    When I publish the "apis" resource with id "ctSubsProviderApiId"
    Then The lifecycle status of API "ctSubsProviderApiId" should be "Published"

    # Consumer tenant (tenant1.com): its own API, so the application ends up with one subscription per tenant.
    When I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "ctSubsConsumerApiId" and deployed it
    When I publish the "apis" resource with id "ctSubsConsumerApiId"
    Then The lifecycle status of API "ctSubsConsumerApiId" should be "Published"

    When The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I create an application "${UNIQUE:ctSubsApp}" with visibility "PRIVATE" as "ctSubsAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctSubsSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "ctSubsConsumerApiId" using application "ctSubsAppId" with payload "ctSubsSub" in provider tenant "tenant1.com"
    Then The response status code should be 201
    When I list DevPortal APIs in tenant "carbon.super"
    Then The response status code should be 200
    And The response should contain "{{ctSubsProviderApiId}}"
    When I subscribe to API "ctSubsProviderApiId" using application "ctSubsAppId" with payload "ctSubsSub" in provider tenant "carbon.super"
    Then The response status code should be 201

    When I retrieve all subscriptions of application "ctSubsAppId" in tenant "tenant1.com"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The response should contain "{{ctSubsConsumerApiId}}"
    And The response should not contain "{{ctSubsProviderApiId}}"
    When I retrieve the application with id "ctSubsAppId" in tenant "tenant1.com"
    Then The response status code should be 200
    And The value of response field "subscriptionCount" should be "1"

    When I retrieve all subscriptions of application "ctSubsAppId" in tenant "carbon.super"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The response should contain "{{ctSubsProviderApiId}}"
    And The response should not contain "{{ctSubsConsumerApiId}}"
    When I retrieve the application with id "ctSubsAppId" in tenant "carbon.super"
    Then The response status code should be 200
    And The value of response field "subscriptionCount" should be "1"

  # LAST in the file on purpose: it flips the SubscriptionUpdate workflow executor to the Approval variant, a
  # server-global registry write (restored by the runner's AfterClass sweep). Only a subscription UPDATE is
  # affected, and no other scenario in this block performs one.
  #
  # With cross-tenant subscriptions enabled the product resolves the update workflow's executor from, and stamps
  # its tenantDomain with, the API PROVIDER's tenant rather than the requesting consumer's
  # (APIConsumerImpl.updateSubscription: workflowDomain = requestedDomain).
  #
  # That recorded domain is asserted through the ONLY observable the product exposes for it: which tenant's admin
  # the task shows up for. GET admin/v4/workflows filters on it directly
  # (SQLConstants.GET_ALL_WORKFLOW_DETAILS_BY_WORKFLOW_TYPE = "... WHERE WF_TYPE = ? AND WF_STATUS = ? AND
  # TENANT_DOMAIN = ?", bound to the LOGGED-IN admin's tenant), so present-for-the-provider-admin plus
  # absent-for-the-consumer-admin pins TENANT_DOMAIN to carbon.super exactly. The field itself is NOT in the
  # response: the admin WorkflowInfo schema carries only workflowType/workflowStatus/created-updatedTime/
  # referenceId/properties/description — MEASURED, run1 read tenantDomain as null. Which also means the legacy
  # CrossTenantSubscriptionUpdateTestCase#verifyTenantDomainInWorkflowsTable could never have asserted anything:
  # its listItem.get("tenantDomain") would have thrown on the missing key, and it did not, because it ran with
  # the Simple executor — which persists no workflow entry at all, so its property-match loop had nothing to
  # iterate.
  @cap:admin @feat:workflows @rule:cross-tenant @type:regression @dep:devportal @dep:publisher @legacy:CrossTenantSubscriptionUpdateTestCase
  Scenario: A cross-tenant subscription update parks its workflow in the API provider's tenant
    Given The system is ready
    And I have valid access tokens as "admin"
    When I enable approval workflow executors from "artifacts/configFiles/crossTenantSub/workflow-extensions.xml"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "ctWfApiId" and deployed it
    When I publish the "apis" resource with id "ctWfApiId"
    Then The lifecycle status of API "ctWfApiId" should be "Published"
    When I retrieve the "apis" resource with id "ctWfApiId"
    Then The response status code should be 200
    And I extract response field "name" and store it as "ctWfApiName"

    # The consumer tenant subscribes across the boundary and then requests a plan change.
    When The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I list DevPortal APIs in tenant "carbon.super"
    Then The response status code should be 200
    And The response should contain "{{ctWfApiId}}"
    When I create an application "${UNIQUE:ctWfApp}" with visibility "PRIVATE" as "ctWfAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctWfSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "ctWfApiId" using application "ctWfAppId" with payload "ctWfSub" in provider tenant "carbon.super"
    Then The response status code should be 201
    And I extract response field "subscriptionId" and store it as "ctWfSubscriptionId"
    When I get the subscription with id "ctWfSubscriptionId" in tenant "carbon.super"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"
    When I request a subscription plan change of "ctWfSubscriptionId" from "Bronze" to "Unlimited" in provider tenant "carbon.super"
    Then The response status code should be 200

    # The parked task is recorded against the API PROVIDER's tenant, so the provider tenant's admin sees it ...
    When I act as "admin"
    And I capture the pending "AM_SUBSCRIPTION_UPDATE" workflow reference where "apiName" is "{{ctWfApiName}}" as "ctWfRef"
    Then The response status code should be 200

    # ... and the CONSUMER tenant's admin, whose listing is scoped to its own tenant, does not.
    When I have valid access tokens as "admin@tenant1.com"
    And I list pending "AM_SUBSCRIPTION_UPDATE" workflows
    Then The response status code should be 200
    And The response should not contain "{{ctWfApiName}}"
