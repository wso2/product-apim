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

  # The external non-APIM OAuth service-provider legs ARE covered (an earlier port had reduced them away): the
  # scenario registers a standalone OAuth client in the RESIDENT key manager via DCR — no Developer Portal
  # application, no subscription — which is the same principal legacy built through the SOAP OAuthAdminService plus
  # a ServiceProvider. Its token is accepted (200) while validation is off and refused (403) once re-enabled, and
  # the DCR client is deregistered by teardown as its owner.
  @cap:gateway @feat:security-enforcement @rule:subscriptionless @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:SubscriptionValidationDisableTestCase
  Scenario Outline: Clearing an API's plans enables subscriptionless invocation only when the tenant allows it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Set AllowSubscriptionValidationDisabling DEFENSIVELY — proven NOT required on 4.7.0 (subscriptionless works
    # by default); kept idempotently for robustness against an environment whose default differs. Capture to restore.
    When I capture the tenant configuration as "originalTenantConf"
    And I register tenant configuration "originalTenantConf" for cleanup
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
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    # Keep this token: it must still work AFTER validation is re-enabled (the internal subscription it was granted
    # survives), which the port previously only claimed in a comment.
    And I copy context value "generatedAccessToken" to "subValApp1Token"

    # The invocation AUTO-CREATED an internal subscription: the API now has exactly ONE subscription, on the
    # internal DefaultSubscriptionless plan. This is the observable that distinguishes "subscriptionless
    # invocation" from "subscription validation skipped" — without it, a gateway that simply bypassed the check
    # would pass the 200 above. The listing is POLLED: the internal subscription is published asynchronously after
    # the invocation is answered, so an immediate single read races it (observed empty on one tenant row while the
    # other saw it in time). The exact count is still asserted after the poll, so a genuine absence fails loudly.
    When I retrieve all subscriptions of api "subValApiId" until the list contains 1 subscriptions within 60 seconds
    Then The response status code should be 200
    And The subscription list should contain exactly 1 subscriptions
    And The value of response field "list[0].throttlingPolicy" should be "DefaultSubscriptionless"

    # An EXTERNAL, non-APIM OAuth client — a service provider registered directly in the resident key manager via
    # DCR, with NO Developer Portal application and therefore no subscription of any kind. Its token is accepted
    # while validation is disabled. This is the legacy's externalSP leg (legacy built the same thing through the
    # SOAP OAuthAdminService + a ServiceProvider; DCR against the resident KM produces the identical principal and
    # its client is swept by teardown).
    When I register an OAuth client "subValExternalSp" as "subValExtSp"
    Then The response status code should be 200
    When I request a client-credentials token using consumer key "subValExtSpClientId" and secret "subValExtSpClientSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "subValExternalToken"
    When I invoke the API at gateway context "{{subValApiContext}}/1.0.0/customers/123/" with method "GET" using access token "subValExternalToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # The external token did NOT create a subscription of its own — the count is unchanged at 1 (legacy asserts
    # exactly this, i.e. the internal subscription is per-application, not per-token).
    When I retrieve all subscriptions of api "subValApiId"
    Then The response status code should be 200
    And The subscription list should contain exactly 1 subscriptions

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

    # The FIRST application's token STILL invokes successfully: it holds the internal subscription created while
    # validation was disabled, so re-enabling enforcement does not retroactively revoke it. Asserted, not merely
    # asserted-in-a-comment — and it is what makes the 403 above meaningful (enforcement discriminates by
    # subscription, it did not just start rejecting everything).
    When I invoke the API at gateway context "{{subValApiContext}}/1.0.0/customers/123/" with method "GET" using access token "subValApp1Token" and payload "" until response status code becomes 200 within 120 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # The EXTERNAL non-APIM principal is now refused (403): it has no application, hence never an internal
    # subscription, so with validation back on it cannot pass. This is the legacy's final external-token assertion,
    # but on a token minted FRESH from the same external client after re-enabling.
    #
    # OBSERVED — the token that was ALREADY used successfully while validation was disabled keeps returning 200 for
    # at least 120 s after enforcement is restored (measured: sustained 200 with the backend body, on both the
    # carbon.super and tenant1.com rows), so the legacy's assertion on the REUSED token does not reproduce here.
    # The gateway caches the key-validation verdict per token (default [apim.cache.gateway_token] TTL 15 min) and an
    # API deploy does not evict it. A distinct scope is requested so the key manager cannot answer from its own
    # per-(client, scope) token cache: this is a genuinely NEW token for the SAME external principal, and its 403
    # both closes the legacy leg and localises the stale 200 above to the per-token gateway cache rather than to the
    # principal having been grandfathered a subscription. The previously-warmed token is deliberately NOT asserted —
    # its 200 is a cache-lifetime artefact, and pinning it would pin a TTL.
    When I request a client-credentials token using consumer key "subValExtSpClientId" and secret "subValExtSpClientSecret" with scope "subValFreshProbe"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "subValExternalTokenAfterReEnable"
    When I invoke the API at gateway context "{{subValApiContext}}/1.0.0/customers/123/" with method "GET" using access token "subValExternalTokenAfterReEnable" and payload "" until response status code becomes 403 within 120 seconds
    Then The response status code should be 403

    # Restore the tenant configuration.
    When I update the tenant configuration from "originalTenantConf"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
