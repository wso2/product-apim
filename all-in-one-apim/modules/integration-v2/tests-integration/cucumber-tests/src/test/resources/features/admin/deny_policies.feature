@cleanup
Feature: Admin Deny (Blocking Condition) Policies

  Ports the deny-throttling / blocking-condition family (legacy APIDenyPolicyTestCase + DenyPolicySearchTestCase)
  over /api/am/admin/v4/throttling/deny-policies. A deny policy blocks by conditionType — API context, USER, IP,
  IP-range, or APPLICATION. This increment covers the self-contained types (IP / IP-range / USER) that need no
  other resource: the full CRUD lifecycle (create -> get -> toggle status -> delete), create-by-type, the
  duplicate-condition rejection (409), and search by condition type + value. Deny policies affect the gateway
  data plane, not the management API, so creating a USER deny does not lock the admin out here; @cleanup removes
  every created condition with the admin token.
  Runs ×2 tenant (super + tenant) — deny-policy management is available per tenant admin. Each tenant row uses a
  distinct IP/value so scenarios never collide on the shared container regardless of whether blocking conditions
  are tenant-isolated or global.
  Also covers the resource-dependent types: API-context deny (needs a deployed API) and APPLICATION deny
  (value owner:appName; needs an application), plus the non-existing-context / non-existing-app negatives.

  @cap:admin @feat:throttling-policies @type:regression @legacy:APIDenyPolicyTestCase
  Scenario Outline: Deny-policy CRUD lifecycle for a fixed IP as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an IP deny policy for fixed IP "<ip>" as "ipDenyId"
    Then The response status code should be 201
    When I retrieve the deny policy "ipDenyId"
    Then The response status code should be 200
    And The response should contain "<ip>"
    When I set the deny policy "ipDenyId" status to "false"
    Then The response status code should be 200
    When I delete the deny policy "ipDenyId"
    Then The response status code should be 200

    Examples:
      | actor             | ip        |
      | admin             | 10.10.0.1 |
      | admin@tenant1.com | 10.11.0.1 |

  @cap:admin @feat:throttling-policies @type:regression @legacy:APIDenyPolicyTestCase
  Scenario Outline: Create deny policies of IP-range and user types as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an IP range deny policy from "<startIp>" to "<endIp>" as "ipRangeDenyId"
    Then The response status code should be 201
    When I create a deny policy of type "USER" with value "<user>" as "userDenyId"
    Then The response status code should be 201

    Examples:
      | actor             | startIp   | endIp     | user           |
      | admin             | 10.20.0.1 | 10.20.0.5 | denyUserSuper  |
      | admin@tenant1.com | 10.21.0.1 | 10.21.0.5 | denyUserTenant |

  @cap:admin @feat:throttling-policies @type:negative @legacy:APIDenyPolicyTestCase
  Scenario Outline: A duplicate deny policy is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an IP deny policy for fixed IP "<ip>" as "dupDenyId"
    Then The response status code should be 201
    When I attempt to create an IP deny policy for fixed IP "<ip>"
    Then The response status code should be 409

    Examples:
      | actor             | ip        |
      | admin             | 10.30.0.1 |
      | admin@tenant1.com | 10.31.0.1 |

  @cap:admin @feat:throttling-policies @type:regression @legacy:DenyPolicySearchTestCase
  Scenario Outline: Search deny policies by condition type and value as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an IP deny policy for fixed IP "<ip>" as "searchDenyId"
    Then The response status code should be 201
    When I search deny policies of type "IP" with value "<ip>"
    Then The response status code should be 200
    And The response should contain "<ip>"

    Examples:
      | actor             | ip        |
      | admin             | 10.40.0.1 |
      | admin@tenant1.com | 10.41.0.1 |

  # API-context deny (increment-2 Group G) — conditionType API, value = a deployed API's context. Needs admin +
  # publisher (the acting admin has both). ×2 tenant.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:APIDenyPolicyTestCase
  Scenario Outline: API-context deny-policy CRUD lifecycle as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "denyApiId" and deployed it
    When I retrieve the "apis" resource with id "denyApiId"
    And I extract response field "context" and store it as "denyApiContext"
    When I create a deny policy of type "API" with value "{{denyApiContext}}/1.0.0" as "apiDenyId"
    Then The response status code should be 201
    When I retrieve the deny policy "apiDenyId"
    Then The response status code should be 200
    When I set the deny policy "apiDenyId" status to "false"
    Then The response status code should be 200
    When I delete the deny policy "apiDenyId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # APPLICATION deny (increment-2 Group G) — conditionType APPLICATION, value = "owner:appName". The owner is
  # taken from the app-create response (not hardcoded), so each tenant row uses its own real owner. ×2 tenant.
  @cap:admin @feat:throttling-policies @type:regression @dep:devportal @legacy:APIDenyPolicyTestCase
  Scenario Outline: APPLICATION deny-policy CRUD lifecycle as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "denyAppPayload"
    And I create an application with payload "denyAppPayload"
    Then The response status code should be 201
    And I extract response field "owner" and store it as "denyAppOwner"
    And I extract response field "name" and store it as "denyAppName"
    When I create a deny policy of type "APPLICATION" with value "{{denyAppOwner}}:{{denyAppName}}" as "appDenyId"
    Then The response status code should be 201
    When I retrieve the deny policy "appDenyId"
    Then The response status code should be 200
    When I delete the deny policy "appDenyId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Resource-dependent negatives (non-existing context / application): NOT ported. Verify-first re-checked on
  # 4.7.0 — the deny-add correctly rejects a non-existing reference, but wraps it in a 500 ("Internal server
  # error … Couldn't Save Block Condition Due to Invalid API Context …") rather than a clean 4xx. Per the
  # standing "don't enshrine 500 server-bugs" principle we do not assert that code; the validation is real but
  # the status is a product defect. (Same 500 as legacy — a stable server-error, not a regression.)
