@cleanup
Feature: Cross-tenant subscription and discovery

  Ports the discovery + subscribe facets of the legacy CrossTenantSubscriptionTestCase. An API published in
  a PROVIDER tenant with subscriptionAvailability=ALL_TENANTS is discoverable and subscribable from a
  CONSUMER tenant when [apim.devportal] enable_cross_tenant_subscriptions=true (supplied by this block's
  tomlExtraOverlayPath). The provider tenant is here carbon.super and the consumer tenant is tenant1.com;
  the cross-tenant devportal operations carry the provider tenant in the X-WSO2-Tenant header (the legacy
  RestAPIStoreImpl overloads' tenantDomain argument). Covers: cross-tenant API listing and direct API GET
  (discovery), a successful cross-tenant subscribe on a public tier, and the two negatives (restricted-tier
  refusal, wrong-tenant application policy). The runtime half — minting a token for the cross-tenant
  subscription and INVOKING the provider tenant's API at the gateway — is the gateway plane's subject and lives
  in gateway/cross_tenant_invocation (it must use the PROVIDER tenant's key manager; the consumer tenant's own
  key manager yields 401 / 900901 there). Teardown via the per-scenario cleanup hook.

  @cap:devportal @feat:subscribe @type:smoke @dep:admin @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario: A consumer-tenant application subscribes to an ALL_TENANTS API published in another tenant
    Given The system is ready
    # Provider tenant (carbon.super): publish an ALL_TENANTS API as the tenant admin.
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "providerApiId" and deployed it
    When I publish the "apis" resource with id "providerApiId"
    Then The lifecycle status of API "providerApiId" should be "Published"

    # Consumer tenant (tenant1.com): the API is discoverable across the boundary.
    When The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I list DevPortal APIs in tenant "carbon.super"
    Then The response status code should be 200
    And The response should contain "{{providerApiId}}"
    When I retrieve DevPortal API "providerApiId" in tenant "carbon.super"
    Then The response status code should be 200
    And The response should contain "{{providerApiId}}"

    # Consumer tenant: create an application and subscribe cross-tenant on the public Bronze tier.
    When I create an application "${UNIQUE:ctConsumerApp}" with visibility "PRIVATE" as "consumerAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "providerApiId" using application "consumerAppId" with payload "ctSub" in provider tenant "carbon.super"
    Then The response status code should be 201

  @cap:devportal @feat:subscribe @type:negative @dep:admin @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario: A cross-tenant subscribe on a role-restricted tier the consumer lacks is refused
    Given The system is ready
    # Provider tenant: create a role-restricted subscription tier and publish an ALL_TENANTS API offering it.
    And I have valid access tokens as "admin"
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

    # Consumer tenant: the consumer lacks the provider-only role, so the restricted tier is refused (403).
    When The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I create an application "${UNIQUE:ctRestrictedApp}" with visibility "PRIVATE" as "restrictedConsumerAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctRestrictedSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "restrictedApiId" using application "restrictedConsumerAppId" with payload "ctRestrictedSub" in provider tenant "carbon.super"
    Then The response status code should be 403

  @cap:devportal @feat:subscribe @type:negative @dep:admin @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario: Creating an application with another tenant's application policy is refused
    Given The system is ready
    # Provider tenant: create a tenant-local application throttling policy.
    And I have valid access tokens as "admin"
    And I create an application throttling policy "${UNIQUE:CtProviderAppPolicy}" allowing 5 requests per minute

    # Consumer tenant: an application referencing the provider tenant's application policy is refused (400).
    # A fixed name is safe here — the create is rejected at policy validation, so nothing persists to collide.
    When The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I put the following JSON payload in context as "ctWrongPolicyApp"
    """
    {"name": "ctWrongPolicyApp", "throttlingPolicy": "{{appThrottlePolicyName}}", "description": "cross-tenant wrong-policy app"}
    """
    And I attempt to create an application with payload "ctWrongPolicyApp"
    Then The response status code should be 400
