@setup
Feature: Setup - cross-tenant subscription runtime fixture

  Provisions what the cross-tenant gateway-invocation scenarios need but do not test: an ALL_TENANTS API
  published in the PROVIDER tenant (carbon.super) against the node backend, a CONSUMER-tenant (tenant1.com)
  application subscribed to it across the tenant boundary, and the id of the PROVIDER tenant's Resident Key
  Manager as discovered from the consumer side. The cross-tenant devportal calls carry the provider tenant in
  the X-WSO2-Tenant header and need this block's tomlExtraOverlayPath
  ([apim.devportal] enable_cross_tenant_subscriptions). Cross-tenant API visibility propagates
  asynchronously, so the listing step polls before the subscribe. Asserts only fixture-create success; created
  ids are registered for the runner's AfterClass sweep as the creating actor.

  Scenario: Provision the provider-tenant API, the cross-tenant subscription and the provider key-manager id
    Given The system is ready

    # --- Provider tenant (carbon.super): publish an ALL_TENANTS API as the tenant admin ---
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_all_tenants_api.json" as "providerApiId" and deployed it
    When I publish the "apis" resource with id "providerApiId"
    Then The lifecycle status of API "providerApiId" should be "Published"
    When I retrieve the "apis" resource with id "providerApiId"
    And I extract response field "context" and store it as "providerApiContext"

    # --- Consumer tenant (tenant1.com): wait for the API to cross the boundary, then subscribe to it ---
    When The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I list DevPortal APIs in tenant "carbon.super"
    Then The response status code should be 200
    And The response should contain "{{providerApiId}}"
    When I create an application "${UNIQUE:ctInvokeApp}" with visibility "PRIVATE" as "consumerAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ctInvokeSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "providerApiId" using application "consumerAppId" with payload "ctInvokeSub" in provider tenant "carbon.super"
    Then The response status code should be 201

    # --- The provider tenant's Resident Key Manager, discovered across the boundary by the consumer ---
    When I list DevPortal key managers in tenant "carbon.super"
    Then The response status code should be 200
    And I capture the id of the list entry named "Resident Key Manager" as "providerResidentKmId"
