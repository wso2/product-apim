@cleanup
Feature: Admin Throttling Policy CRUD

  Admin-plane CRUD of throttling policies via the admin REST API, across policy types:
    - APPLICATION throttling policy (request-count) — ports the CRUD half of the legacy
      ApplicationThrottlingPolicyServerRestartTestCase;
    - CUSTOM (Siddhi) throttling rule — ports the CRUD of the legacy CustomThrottlingPolicyServerRestartTestCase.
  Both legacy tests' only unique "restart" behaviour ("a deleted policy stays deleted after a restart") was
  intentionally dropped: it is not a functional assertion (a bare admin GET → 404), and DB-survives-restart
  durability is already covered functionally by token_persistence_restart. So this carries NO restart and runs
  in a concurrent block. Each scenario creates a uniquely-named policy (and cleans it up), so it is
  parallel-safe. Admin-plane, run as the tenant admin (super tenant — custom rules are an admin-global feature).

  Background:
    Given The system is ready
    And I have valid access tokens as "admin"

  @cap:admin @feat:throttling-policies @type:regression @legacy:ApplicationThrottlingPolicyServerRestartTestCase
  Scenario: Create, retrieve, and delete an application throttling policy
    When I create an application throttling policy "${UNIQUE:appPolicyCrud}" allowing 10 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    # Retrieve and verify the persisted policy (right name + request-count limit).
    When I retrieve the application throttling policy "appThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{appThrottlePolicyName}}"
    And The response should contain "REQUESTCOUNTLIMIT"
    # Delete it, then confirm it is gone.
    When I delete the application throttling policy "appThrottlePolicyId"
    Then The response status code should be 200
    When I retrieve the application throttling policy "appThrottlePolicyId"
    Then The response status code should be 404

  @cap:admin @feat:throttling-policies @type:regression @legacy:CustomThrottlingPolicyServerRestartTestCase
  Scenario: Create, retrieve, and delete a custom (Siddhi) throttling rule
    # A custom Siddhi rule (the API context is only baked into the Siddhi query; this scenario asserts CRUD, not
    # enforcement — enforcement is covered by gateway/throttling_enforcement).
    When I create a custom throttling policy "${UNIQUE:customPolicyCrud}" throttling API context "crudDummyContext" after 10 requests per minute
    Then The response status code should be 201
    And The response should contain "siddhiQuery"
    # Retrieve and verify the persisted rule (right name).
    When I retrieve the custom throttling policy "customThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{customThrottlePolicyName}}"
    # Delete it, then confirm it is gone.
    When I delete the custom throttling policy "customThrottlePolicyId"
    Then The response status code should be 200
    When I retrieve the custom throttling policy "customThrottlePolicyId"
    Then The response status code should be 404
