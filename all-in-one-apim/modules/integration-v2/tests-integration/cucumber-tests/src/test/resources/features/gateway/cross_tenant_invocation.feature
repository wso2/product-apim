Feature: Cross-tenant gateway invocation

  Runtime half of the cross-tenant subscription arc: a CONSUMER-tenant (tenant1.com) application that is
  subscribed across the tenant boundary to an ALL_TENANTS API published in the PROVIDER tenant (carbon.super)
  invokes that API at the gateway. Both scenarios share the fixture built by
  _setup_cross_tenant_subscription (one API, one application, one cross-tenant subscription), and differ in
  exactly ONE variable: the KEY MANAGER the application's keys were generated against.

  The gateway resolves a JWT's validator from the key managers of the tenant that owns the invoked API
  (KeyManagerHolder.getKeyManagerByIssuer(<invoked API's tenant>, <token issuer>)), so a token minted from a
  key manager registered only in the CONSUMER tenant can never authenticate at the PROVIDER tenant's gateway.
  The product's answer is to generate the keys against the PROVIDER tenant's key manager, which the consumer
  discovers through the cross-tenant devportal key-manager listing — and which is why the devportal keygen
  falls back from a per-organization key-manager NAME lookup to a tenant-agnostic UUID lookup. Ports
  CrossTenantSubscriptionTestCase#invokeFromTokenInOtherTenant (whose same-tenant twin is already covered by
  gateway/rest_invocation). Teardown is per-runner (the AfterClass sweep), not per scenario, because the
  fixture must survive both scenarios.

  @cap:gateway @feat:rest-invocation @rule:cross-tenant @type:smoke @dep:devportal @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario: A consumer-tenant application invokes the provider tenant's API with keys from the provider's key manager
    Given I act as "subscriberUser@tenant1.com"
    When I put the following JSON payload in context as "providerKmKeys"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{providerResidentKmId}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "consumerAppId" with payload "providerKmKeys"
    Then The response status code should be 200
    When I request a client-credentials token using consumer key "consumerKey" and secret "consumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "providerKmAccessToken"
    When I invoke the API at gateway context "{{providerApiContext}}/1.0.0/customers/123/" with method "GET" using access token "providerKmAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

  # The security boundary the positive scenario depends on: the same application, the same cross-tenant
  # subscription, but keys from the CONSUMER tenant's own resident key manager. The provider tenant's gateway
  # has no validator for that issuer, so the token is not merely unsubscribed (403) - it fails authentication
  # outright with 401 / 900901.
  @cap:gateway @feat:security-enforcement @rule:cross-tenant @type:negative @dep:devportal @dep:publisher @legacy:CrossTenantSubscriptionTestCase
  Scenario: A token from the consumer tenant's own key manager is rejected at the provider tenant's gateway
    Given I act as "subscriberUser@tenant1.com"
    When I put the following JSON payload in context as "consumerKmKeys"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "consumerAppId" with payload "consumerKmKeys"
    Then The response status code should be 200
    When I request a client-credentials token using consumer key "consumerKey" and secret "consumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "consumerKmAccessToken"
    When I invoke the API at gateway context "{{providerApiContext}}/1.0.0/customers/123/" with method "GET" using access token "consumerKmAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
