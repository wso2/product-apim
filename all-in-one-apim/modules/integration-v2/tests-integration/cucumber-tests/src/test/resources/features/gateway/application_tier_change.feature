@cleanup
Feature: Gateway Application Tier Change Enforcement

  Ports ChangeApplicationTierAndTestInvokingTestCase. Proves the DISTINCT assertion the throttling suite does not
  cover: changing an APPLICATION's throttling tier mid-life is honoured at the gateway. An application bound to a
  LOW application policy is throttled (429) once it exceeds that low limit; after the application is switched to a
  HIGHER application policy (and its throttle counter reset so the change is observed immediately rather than after
  the window), a burst that would have tripped the LOW limit now all succeeds (200) — and the app still trips 429
  at the higher limit; switching back to the LOW policy re-imposes the low limit (429). The bespoke low/high
  policies are created via the admin API (built-in tiers are thousands/min, unreachable in a test).

  # Runs x2-tenant (super + tenant1) in its OWN thread-count=1 block: the bespoke low/high policies, the
  # application and its token all scope to the acting actor's tenant (unique names via ${UNIQUE:}), so the two
  # rows are isolated. Application-throttle windows are time-sensitive and the tier-change + reset dance is the
  # part that made the legacy test flaky, so the block never shares a container with other time-sensitive throttle
  # scenarios (the two rows still run sequentially). The counter RESET (rather than a 60s window sleep) is what
  # makes the tier change deterministically observable in-window — the same reset the throttling suite relies on.
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:ChangeApplicationTierAndTestInvokingTestCase
  Scenario Outline: An application-tier change raises then lowers the enforced limit at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A LOW application policy (3/min) and a HIGH one (20/min), both reachable/distinguishable in a test.
    When I create an application throttling policy "${UNIQUE:appTierLow3}" allowing 3 requests per minute
    Then The response status code should be 201
    And I copy context value "appThrottlePolicyName" to "appTierLowPolicy"
    When I create an application throttling policy "${UNIQUE:appTierHigh20}" allowing 20 requests per minute
    Then The response status code should be 201
    And I copy context value "appThrottlePolicyName" to "appTierHighPolicy"

    # Publish and deploy an API to invoke.
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tierApiId" and deployed it
    When I publish the "apis" resource with id "tierApiId"
    Then The lifecycle status of API "tierApiId" should be "Published"
    When I retrieve the "apis" resource with id "tierApiId"
    And I extract response field "context" and store it as "tierApiContext"

    # An application on the LOW policy, subscribed and keyed for the password grant.
    When I put the following JSON payload in context as "tierAppPayload"
    """
    {"name":"${UNIQUE:TierApp}","throttlingPolicy":"{{appTierLowPolicy}}","description":"Application-tier change test app"}
    """
    And I create an application with payload "tierAppPayload"
    Then The response status code should be 201
    And I extract response field "name" and store it as "tierAppName"
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "tierApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "tierSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # LOW tier active: the app trips 429 quickly (3/min).
    When I invoke the API at gateway context "{{tierApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    # Switch the application to the HIGH (20/min) policy. The gateway's per-minute throttle window must roll over
    # before the new limit is observed (a counter reset only clears the persisted count, not the in-flight window),
    # so poll until the app can invoke again (200) — this waits out the old window AND confirms the change is live.
    # Then a burst of 8 calls all succeed (200) — impossible under the old 3/min limit — proving the tier change
    # raised the enforced limit.
    When I put the following JSON payload in context as "tierAppToHighPayload"
    """
    {"name":"{{tierAppName}}","throttlingPolicy":"{{appTierHighPolicy}}","description":"switched to high tier"}
    """
    And I update the application "createdAppId" with payload "tierAppToHighPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{tierApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{tierApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" 8 times expecting status 200
    # And it still trips 429 at the higher limit.
    When I invoke the API at gateway context "{{tierApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    # Switch the application BACK to the LOW (3/min) policy — the low limit is re-imposed. Wait out the current
    # window (poll until 200), then a fresh burn trips 429 again quickly, proving the change back took effect.
    When I put the following JSON payload in context as "tierAppToLowPayload"
    """
    {"name":"{{tierAppName}}","throttlingPolicy":"{{appTierLowPolicy}}","description":"switched back to low tier"}
    """
    And I update the application "createdAppId" with payload "tierAppToLowPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{tierApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{tierApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
