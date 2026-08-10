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
  (value owner:appName; needs an application), the API-type search semantics (an unquoted conditionValue matches
  as a SUBSTRING, a quoted one matches EXACTLY), and the malformed/dangling-reference create negatives (malformed IP,
  malformed IP range, non-existing application, non-existing user, non-existing API context).

  @cap:admin @feat:throttling-policies @type:regression @legacy:APIDenyPolicyTestCase
  Scenario Outline: Deny-policy CRUD lifecycle for a fixed IP as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an IP deny policy for fixed IP "<ip>" as "ipDenyId"
    Then The response status code should be 201
    When I retrieve the deny policy "ipDenyId"
    Then The response status code should be 200
    And The response should contain "<ip>"
    And The value of response field "conditionStatus" should be "true"
    When I set the deny policy "ipDenyId" status to "false"
    Then The response status code should be 200
    # The PATCH must actually flip the flag, not merely answer 200 — assert the returned AND the re-read value.
    And The value of response field "conditionStatus" should be "false"
    When I retrieve the deny policy "ipDenyId"
    Then The response status code should be 200
    And The value of response field "conditionStatus" should be "false"
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
    And The value of response field "conditionValue" should be "{{denyApiContext}}/1.0.0"
    When I set the deny policy "apiDenyId" status to "false"
    Then The response status code should be 200
    And The value of response field "conditionStatus" should be "false"
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

  # API-type conditionValue search semantics (ports DenyPolicySearchTestCase). Two APIs whose contexts NEST — the
  # second is the first plus a path segment — so a matching hit and a non-hit can be told apart. Legacy used the
  # fixed contexts /test and /test/abc; here the base segment is generated per run so the exact hit COUNTS below
  # are safe on a shared container (no other scenario's condition value can contain it). Both the API and the
  # deny policy are per-scenario and swept by the cleanup hook.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:DenyPolicySearchTestCase
  Scenario Outline: An API deny-policy search matches a condition value as a substring as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique alphanumeric value and store it as "nestBase"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "nestApi1Payload"
    And I set the field "context" to "/dn{{nestBase}}" in the payload "nestApi1Payload"
    And I create an "apis" resource with payload "nestApi1Payload" as "nestApi1Id"
    And I deploy the API with id "nestApi1Id"
    When I retrieve the "apis" resource with id "nestApi1Id"
    And I extract response field "context" and store it as "nestCtx1"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "nestApi2Payload"
    And I set the field "context" to "/dn{{nestBase}}/abc" in the payload "nestApi2Payload"
    And I create an "apis" resource with payload "nestApi2Payload" as "nestApi2Id"
    And I deploy the API with id "nestApi2Id"
    When I retrieve the "apis" resource with id "nestApi2Id"
    And I extract response field "context" and store it as "nestCtx2"
    When I create a deny policy of type "API" with value "{{nestCtx1}}/1.0.0" as "nestDeny1Id"
    Then The response status code should be 201
    When I create a deny policy of type "API" with value "{{nestCtx2}}/1.0.0" as "nestDeny2Id"
    Then The response status code should be 201
    # Confirm BOTH conditions are stored and visible to this admin BEFORE searching, so an empty search result
    # below can only mean the search filter did not match — never that the fixture failed to materialize.
    When I retrieve all deny policies
    Then The response status code should be 200
    And The response should contain "{{nestCtx1}}/1.0.0"
    And The response should contain "{{nestCtx2}}/1.0.0"
    # An unquoted value is a SUBSTRING match: searching the shallower context returns BOTH nested conditions,
    # since the deeper context contains the shallower one.
    When I search deny policies of type "API" with value "{{nestCtx1}}"
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list "{{nestCtx1}}/1.0.0,{{nestCtx2}}/1.0.0"
    # Pick the deeper condition out of those two hits by JSONPath predicate and check it carries the id returned
    # when it was created. The predicate deliberately carries a {{...}} placeholder in the PATH: that shape used to
    # match nothing silently (the path was handed to JsonPath unresolved), reading as an empty result against a
    # correct response. This line is what keeps the placeholder resolution in the field path honest.
    And The response field "list[?(@.conditionValue=='{{nestCtx2}}/1.0.0')].conditionId" should be exactly the list "{{nestDeny2Id}}"
    # Searching the DEEPER context excludes the shallower one — exactly one hit, and it is the deeper condition.
    When I search deny policies of type "API" with value "{{nestCtx2}}"
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list "{{nestCtx2}}/1.0.0"
    # A mid-value substring that is not a path prefix at all still matches both — the match is a plain substring
    # test on the stored value, not a path-prefix test.
    When I search deny policies of type "API" with value "dn{{nestBase}}"
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list "{{nestCtx1}}/1.0.0,{{nestCtx2}}/1.0.0"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # QUOTING the conditionValue switches the search from substring to EXACT match, so a quoted PARTIAL value (a
  # context without its version suffix) matches nothing at all, while the same value unquoted matched above.
  # Ports DenyPolicySearchTestCase#testGetBlockConditionsByConditionTypeAndExactValue.
  @cap:admin @feat:throttling-policies @type:regression @dep:publisher @legacy:DenyPolicySearchTestCase
  Scenario Outline: A quoted deny-policy condition value matches exactly and excludes partial values as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique alphanumeric value and store it as "exactBase"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "exactApi1Payload"
    And I set the field "context" to "/de{{exactBase}}" in the payload "exactApi1Payload"
    And I create an "apis" resource with payload "exactApi1Payload" as "exactApi1Id"
    And I deploy the API with id "exactApi1Id"
    When I retrieve the "apis" resource with id "exactApi1Id"
    And I extract response field "context" and store it as "exactCtx1"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "exactApi2Payload"
    And I set the field "context" to "/de{{exactBase}}/abc" in the payload "exactApi2Payload"
    And I create an "apis" resource with payload "exactApi2Payload" as "exactApi2Id"
    And I deploy the API with id "exactApi2Id"
    When I retrieve the "apis" resource with id "exactApi2Id"
    And I extract response field "context" and store it as "exactCtx2"
    When I create a deny policy of type "API" with value "{{exactCtx1}}/1.0.0" as "exactDeny1Id"
    Then The response status code should be 201
    When I create a deny policy of type "API" with value "{{exactCtx2}}/1.0.0" as "exactDeny2Id"
    Then The response status code should be 201
    # Quoted + partial (no version suffix) → NO results, for either context.
    When I search deny policies of type "API" with value "\"{{exactCtx1}}\""
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list ""
    When I search deny policies of type "API" with value "\"{{exactCtx2}}\""
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list ""
    # The same values UNQUOTED and complete → exactly one hit each, carrying that exact condition value.
    When I search deny policies of type "API" with value "{{exactCtx1}}/1.0.0"
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list "{{exactCtx1}}/1.0.0"
    When I search deny policies of type "API" with value "{{exactCtx2}}/1.0.0"
    Then The response status code should be 200
    And The response field "list[*].conditionValue" should be exactly the list "{{exactCtx2}}/1.0.0"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # ---- Malformed / dangling-reference deny-policy creates -----------------------------------------------------
  # These five cases were UNFALSIFIABLE in legacy APIDenyPolicyTestCase: each sat in a
  # `try { assert 201 == 201 } catch (e) { assert e.code == <n> }`, so the test passed whether the product
  # accepted or rejected the input. Each is pinned below against the status the product ACTUALLY returns on
  # 4.7.0, so a change in EITHER direction is detected.
  #
  # PRODUCT WART, pinned deliberately: four of the five are answered with 500 "Internal server error", not a
  # clean 4xx. Every one of them is a caller-input error the API detects and reports in the body
  # ("… is an invalid ip address format", "Couldn't Save Block Condition Due to Invalid API Context …"), so the
  # correct status would be 400 (malformed IP / IP range) or 404/400 (dangling application or API-context
  # reference). ApiMgtDAO#addBlockConditions raises a bare APIManagementException for all of them and
  # GlobalThrowableMapper maps that to 500. These assertions pin 500 rather than leave the case unported, so that
  # a fix returning a proper 4xx shows up as a test failure to be updated instead of being silently absorbed —
  # and equally, so the validation itself cannot silently disappear (which would turn these into 201s).

  @cap:admin @feat:throttling-policies @type:negative @legacy:APIDenyPolicyTestCase
  Scenario Outline: A deny policy for a malformed fixed IP is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create an IP deny policy for fixed IP "127..0.0.1"
    # 500 is the product's wart; 400 would be correct. The body proves it IS input validation, not a crash.
    Then The response status code should be 500
    And The response should contain "127..0.0.1 is an invalid ip address format"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Legacy's "invalid IP range" case submitted conditionType IP with a RANGE-shaped value (startingIp/endingIp and
  # no fixedIp) — its condition type did not match its value shape, so it never exercised range validation at all.
  # The meaningful test is the one legacy meant to write: an IPRANGE condition whose starting IP is malformed.
  @cap:admin @feat:throttling-policies @type:negative @legacy:APIDenyPolicyTestCase
  Scenario Outline: A deny policy for an IP range with a malformed starting IP is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create an IP range deny policy from "127..0.0.1" to "127.0.0.5"
    Then The response status code should be 500
    And The response should contain "Condition type: IPRANGE"
    And The response should contain "127..0.0.1 is an invalid ip address format"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:negative @legacy:APIDenyPolicyTestCase
  Scenario Outline: A deny policy for a non-existing application is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create a deny policy of type "APPLICATION" with value "<owner>:noSuchApp${UNIQUE:A}"
    Then The response status code should be 500
    And The response should contain "Couldn't Save Block Condition Due to Invalid Application name"

    Examples:
      | actor             | owner             |
      | admin             | admin             |
      | admin@tenant1.com | admin@tenant1.com |

  # NOT a negative: verify-first FINDING on 4.7.0 — a USER deny condition for a user that does not exist is
  # ACCEPTED (201). ApiMgtDAO#addBlockConditions checks only that the value's tenant domain matches the caller's;
  # it never looks the user up, unlike the APPLICATION and API branches. Legacy hinted 409 in its unreachable
  # catch, which is wrong. The stored conditionValue is also qualified with the caller's tenant domain, so that
  # normalization is pinned here too.
  @cap:admin @feat:throttling-policies @type:regression @legacy:APIDenyPolicyTestCase
  Scenario Outline: A deny policy for a non-existing user is accepted and qualified with the tenant domain as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a deny policy of type "USER" with value "noSuchUser${UNIQUE:U}" as "absentUserDenyId"
    Then The response status code should be 201
    And The value of response field "conditionType" should be "USER"
    When I retrieve the deny policy "absentUserDenyId"
    Then The response status code should be 200
    And The response should contain "@<tenantDomain>"

    Examples:
      | actor             | tenantDomain |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |

  @cap:admin @feat:throttling-policies @type:negative @legacy:APIDenyPolicyTestCase
  Scenario Outline: A deny policy for a non-existing API context is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create a deny policy of type "API" with value "/denyNonExisting${UNIQUE:X}/1.0.0/"
    Then The response status code should be 500
    And The response should contain "Couldn't Save Block Condition Due to Invalid API Context"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
