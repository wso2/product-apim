@cleanup
Feature: Gateway Subscriptionless Invocation (subscription-validation disabling)

  Ports SubscriptionValidationDisableTestCase. Clearing an API's business plans (policies=[]) makes the product
  auto-apply the internal DefaultSubscriptionless tier, after which the API can be invoked WITHOUT a subscription;
  re-adding a plan restores subscription enforcement (a token from a non-subscribed application is then refused).
  VERIFIED on 4.7.0 (2026-07-19, probe): this works BY DEFAULT — no tenant-conf gate is required. An earlier port
  believed AllowSubscriptionValidationDisabling had to be set true first, but a probe that cleared policies WITHOUT
  the flag still got DefaultSubscriptionless (matches the WSO2 docs "supported by default"). The scenario below
  still sets the flag DEFENSIVELY (harmless idempotent set, robust if some environment's default differs) and
  restores it. Runs x2-tenant (super + tenant1): tokens and API scope to the acting actor's tenant. Needs the
  block backend for the runtime invocations; per-scenario cleanup removes the API/application.

  # SCOPE: the external non-APIM OAuth service-provider half of the legacy (a second token minted from an IS-side
  # OAuth SP via SOAP identity provisioning) is a DOCUMENTED REDUCTION — the portable core is subscriptionless
  # enforcement itself (no-subscription invocation → 200 when disabled; → refused when re-enabled), which a normal
  # application token exercises fully.
  @cap:gateway @feat:security-enforcement @rule:subscriptionless @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:SubscriptionValidationDisableTestCase
  Scenario Outline: Clearing an API's plans enables subscriptionless invocation only when the tenant allows it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Set AllowSubscriptionValidationDisabling DEFENSIVELY — proven NOT required on 4.7.0 (subscriptionless works
    # by default); kept idempotently for robustness against an environment whose default differs. Capture to restore.
    When I capture the tenant configuration as "originalTenantConf"
    And I copy context value "originalTenantConf" to "subValTenantConf"
    And I set the boolean field "AllowSubscriptionValidationDisabling" to "true" in the payload "subValTenantConf"
    And I update the tenant configuration from "subValTenantConf"
    Then The response status code should be 200

    # Publish and deploy an API (on the Unlimited plan to begin with).
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "subValApiId" and deployed it
    When I publish the "apis" resource with id "subValApiId"
    Then The lifecycle status of API "subValApiId" should be "Published"
    When I retrieve the "apis" resource with id "subValApiId"
    And I extract response field "context" and store it as "subValApiContext"
    And I put the response payload in context as "subValApiPayload"

    # Clear the API's business plans — with the tenant flag on, the product auto-applies the internal
    # DefaultSubscriptionless tier.
    When I update the "apis" resource "subValApiId" and "subValApiPayload" with configuration type "policies" and value:
      """
      []
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "subValApiId"
    Then The response should contain "DefaultSubscriptionless"
    When I deploy the API with id "subValApiId"
    Then The response status code should be 201

    # An application that is keyed but NOT subscribed to this API can still invoke it (subscriptionless).
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "subValAppPayload"
    And I create an application with payload "subValAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{subValApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200

    # Re-add a business plan — subscription enforcement returns. The FIRST application's token keeps working (it was
    # granted the auto-created internal subscription while validation was disabled, matching the legacy), but a
    # freshly-keyed application that never got that internal subscription is now refused (403) — proving enforcement
    # is back on.
    When I retrieve the "apis" resource with id "subValApiId"
    And I put the response payload in context as "subValApiPayload"
    When I update the "apis" resource "subValApiId" and "subValApiPayload" with configuration type "policies" and value:
      """
      ["Unlimited"]
      """
    Then The response status code should be 200
    When I deploy the API with id "subValApiId"
    Then The response status code should be 201

    # A brand-new, non-subscribed application: its token has no subscription to this API, so with validation
    # re-enabled the gateway refuses it (403).
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "subValApp2Payload"
    And I create an application with payload "subValApp2Payload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{subValApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 120 seconds
    Then The response status code should be 403

    # Restore the tenant configuration.
    When I update the tenant configuration from "originalTenantConf"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
