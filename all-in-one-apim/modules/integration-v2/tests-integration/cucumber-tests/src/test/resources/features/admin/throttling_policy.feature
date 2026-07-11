@cleanup
Feature: Admin Throttling Policy CRUD

  Admin-plane CRUD of throttling policies via the admin REST API, across all policy types — application,
  subscription, advanced (API-level), and custom (Siddhi). Covers create (request-count + bandwidth limit
  types; advanced conditional groups), retrieve, update, delete, and the not-found (404) edge, plus listing.
  Ports the CRUD of the backend Application/Subscription/Advanced/Custom ThrottlingPolicyTestCase (+ the CRUD
  half of the restart-family policy tests). Enforcement (429) is covered by gateway/throttling_enforcement.
  Application/subscription/advanced run ×2 tenant (tenant admins manage their own tiers); custom rules are an
  admin-global feature (a tenant admin gets 403 creating one — see custom-throttling-policy-restart-port), so
  the custom scenario is super-tenant only. Each scenario uses uniquely-named policies (parallel-safe) and
  cleans them up. Duplicate-name (409) and delete-of-an-in-use advanced policy (403) are covered below;
  export/import is in throttle_policy_export_import.feature. Deferred to a later increment: advanced
  op↔API-level enforcement (gateway) + cross-admin permission, subscription permission-visibility.

  @cap:admin @feat:throttling-policies @type:regression @legacy:ApplicationThrottlingPolicyTestCase @legacy:ApplicationThrottlingPolicyServerRestartTestCase
  Scenario Outline: Application throttling policy CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "${UNIQUE:appCrud}" allowing 20 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    When I retrieve the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{appThrottlePolicyName}}"
    And The response should contain "REQUESTCOUNTLIMIT"
    When I update the "application" throttling policy "appThrottlePolicyId" setting its description to "updated application policy"
    Then The response status code should be 200
    And The response should contain "updated application policy"
    # Bandwidth limit-type variant.
    When I create an application throttling policy "${UNIQUE:appCrudBw}" allowing 5 KB per minute
    Then The response status code should be 201
    And The response should contain "BANDWIDTHLIMIT"
    # Delete, then confirm it is gone via a GET → 404 (parity with the legacy CRUD assertion).
    When I delete the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 200
    When I retrieve the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: Subscription throttling policy CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:subCrud}" allowing 20 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    When I retrieve the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{subThrottlePolicyName}}"
    When I update the "subscription" throttling policy "subThrottlePolicyId" setting its description to "updated subscription policy"
    Then The response status code should be 200
    And The response should contain "updated subscription policy"
    When I create a subscription throttling policy "${UNIQUE:subCrudBw}" allowing 5 KB per minute
    Then The response status code should be 201
    And The response should contain "BANDWIDTHLIMIT"
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: Advanced (API-level) throttling policy CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:advCrud}" allowing 20 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    When I retrieve the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{advThrottlePolicyName}}"
    When I update the "advanced" throttling policy "advThrottlePolicyId" setting its description to "updated advanced policy"
    Then The response status code should be 200
    And The response should contain "updated advanced policy"
    # Bandwidth + conditional-group limit-type variants.
    When I create an advanced throttling policy "${UNIQUE:advCrudBw}" allowing 5 KB per minute
    Then The response status code should be 201
    And The response should contain "BANDWIDTHLIMIT"
    When I create an advanced throttling policy "${UNIQUE:advCrudCond}" allowing 20 requests per minute with a header conditional group
    Then The response status code should be 201
    And The response should contain "HEADERCONDITION"
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Custom (Siddhi) rules are admin-global — a tenant admin gets 403 creating one — so this runs super only.
  @cap:admin @feat:throttling-policies @type:regression @legacy:CustomThrottlingPolicyTestCase @legacy:CustomThrottlingPolicyServerRestartTestCase
  Scenario: Custom (Siddhi) throttling rule CRUD
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "${UNIQUE:customCrud}" throttling API context "crudDummyContext" after 10 requests per minute
    Then The response status code should be 201
    And The response should contain "siddhiQuery"
    When I retrieve the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{customThrottlePolicyName}}"
    When I update the "custom" throttling policy "customThrottlePolicyId" setting its description to "updated custom rule"
    Then The response status code should be 200
    And The response should contain "updated custom rule"
    When I delete the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 200
    # Confirm it is gone via a GET → 404 (parity with the legacy CRUD assertion).
    When I retrieve the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 404

  @cap:admin @feat:throttling-policies @type:regression @legacy:GetThrottlingPoliciesTestCase
  Scenario: List throttling policies and confirm the built-in defaults are present
    Given The system is ready
    And I have valid access tokens as "admin"
    When I retrieve all "subscription" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

  # --- Duplicate-name → 409 (increment-2 Group A). Create, then re-create with the captured name. ---
  @cap:admin @feat:throttling-policies @type:negative @legacy:ApplicationThrottlingPolicyTestCase
  Scenario Outline: Creating an application throttling policy with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "dupApp${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I create an application throttling policy "{{appThrottlePolicyName}}" allowing 1000 requests per minute
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:negative @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: Creating a subscription throttling policy with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "dupSub${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I create a subscription throttling policy "{{subThrottlePolicyName}}" allowing 1000 requests per minute
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:negative @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: Creating an advanced throttling policy with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "dupAdv${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I create an advanced throttling policy "{{advThrottlePolicyName}}" allowing 1000 requests per minute
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Custom (Siddhi) is super-tenant only (tenant create is 403).
  @cap:admin @feat:throttling-policies @type:negative @legacy:CustomThrottlingPolicyTestCase
  Scenario: Creating a custom throttling policy with an existing name is rejected
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "dupCustom${UNIQUE:P}" throttling API context "/dc${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 201
    When I create a custom throttling policy "{{customThrottlePolicyName}}" throttling API context "/dc2${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 409

  # --- Delete-of-an-in-use advanced policy → 403 (increment-2 Group C #5). ---
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy assigned to an API cannot be deleted as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "inUse${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "inUseApiId" and deployed it
    When I retrieve the "apis" resource with id "inUseApiId"
    And I put the response payload in context as "inUseApiPayload"
    And I update the "apis" resource "inUseApiId" and "inUseApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 403
    # Un-assign by deleting the API so the (registered) policy can then be cleaned up.
    When I delete the "apis" resource with id "inUseApiId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Cross-admin advanced-policy delete (increment-2 Group B) — a second admin can delete a policy created by
  # another admin (200): admin management is not owner-scoped. Provisions a 2nd admin user per tenant. x2 tenant.
  # Ports AdvancedThrottlingPolicyTestCase #11.
  @cap:admin @feat:throttling-policies @type:regression @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy created by one admin can be deleted by another admin in <tenant>
    Given The system is ready
    And I provision user "policyAdmin2" with roles "admin" in tenant "<tenant>"
    And I have valid access tokens as "<admin1>"
    When I create an advanced throttling policy "crossAdmin${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    And I have valid access tokens as "<admin2>"
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200

    Examples:
      | tenant       | admin1            | admin2                   |
      | carbon.super | admin             | policyAdmin2             |
      | tenant1.com  | admin@tenant1.com | policyAdmin2@tenant1.com |

  # Role-restricted subscription tier (increment-2 Group B) — a tier ALLOW-restricted to a role cannot be used to
  # subscribe by a user outside that role (403 "Tier … is not allowed"). subscriberUser (Internal/subscriber)
  # lacks Internal/creator. Spans admin (policy) + publisher (API) + devportal (subscribe). x2 tenant. Ports
  # SubscriptionThrottlingPolicyTestCase#testCheckPolicyPermission.
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @dep:devportal @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: A subscription tier restricted to a role cannot be used by a user outside that role as <adminActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    When I create a subscription throttling policy "restrictedTier${UNIQUE:P}" allowing 1000 requests per minute restricted to role "Internal/creator"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "b2ApiId" and deployed it
    When I retrieve the "apis" resource with id "b2ApiId"
    And I put the response payload in context as "b2ApiPayload"
    And I update the "apis" resource "b2ApiId" and "b2ApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "b2ApiId"
    Then The lifecycle status of API "b2ApiId" should be "Published"
    And The system is ready and I have valid devportal access token as "<subscriberActor>"
    And I create an application "${UNIQUE:B2App}" with visibility "PRIVATE" as "b2AppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "b2Sub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I attempt to subscribe to API "b2ApiId" using application "b2AppId" with payload "b2Sub"
    Then The response status code should be 403

    Examples:
      | adminActor        | subscriberActor            |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
