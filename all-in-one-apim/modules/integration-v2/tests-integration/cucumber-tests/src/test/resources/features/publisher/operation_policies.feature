@cleanup
Feature: Publisher Operation Policies

  Publisher-plane operation-policy management: create a reusable common (tenant-global) operation policy and
  an API-specific operation policy from a synapse template + spec, assert the persisted spec (not just the
  status code), confirm the common policy is discoverable, then delete both. Runs in both the super tenant and
  tenant1.com as admin — operation-policy management requires the admin scope in the default configuration; a
  least-privilege publisher token is rejected with 401 (verified), so admin is the minimum role here. Negative
  scenarios assert role enforcement (a subscriber cannot create policies) and
  spec validation (a malformed spec is rejected). Teardown is the per-scenario cleanup hook; created common
  policies are also registered for the sweep (ResourceCleanup) since they are tenant-global and outlive the
  API. Improves on the legacy, which asserted only 201/200 status codes and had no negative coverage.

  @cap:publisher @feat:operation-policies @type:regression @legacy:GovernancePolicyBaselineTestCase
  Scenario Outline: Create, list and delete common and API-specific operation policies as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it

    # Common (reusable) operation policy — assert the persisted spec, not only the status.
    When I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "commonPolicyId"
    Then The response status code should be 201
    And The response should contain "custom_add_common_header"
    And The response should contain "Mediation"

    # It is discoverable among the tenant's common policies.
    When I retrieve available common policies
    Then The response status code should be 200
    And The response should contain "custom_add_common_header"

    # API-specific operation policy — scoped to this API, asserted on its persisted spec.
    When I create a new API specific policy for api "createdApiId" with spec "artifacts/payloads/policySpecFiles/custom_add_api_specific_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_api_specific_header.yaml" as "apiSpecificPolicyId"
    Then The response status code should be 201
    And The response should contain "custom_add_api_specific_header"

    # Both can be deleted (the delete path is under test; the cleanup hook backstops on early failure).
    When I delete the api "createdApiId" specific policy "apiSpecificPolicyId"
    Then The response status code should be 200
    When I delete the "operation-policies" resource with id "commonPolicyId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:operation-policies @type:negative @legacy:GovernancePolicyBaselineTestCase
  Scenario Outline: A subscriber-role user cannot create a common operation policy as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "rejectedPolicyId"
    Then The response status code should be 401

    Examples:
      | actor                      |
      | subscriberUser             |
      | subscriberUser@tenant1.com |

  # A malformed spec (missing the required `name`) is rejected by spec validation — the policy is NOT created.
  # NOTE: the server surfaces the validation failure as HTTP 500 (not a 400); we assert the actual contract and
  # match the validation message so this characterises the validation path rather than locking in any 500.
  @cap:publisher @feat:operation-policies @type:negative @legacy:GovernancePolicyBaselineTestCase
  Scenario Outline: Creating a common operation policy with an invalid spec is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_invalid_header.j2" and "artifacts/payloads/policySpecFiles/custom_invalid_header.yaml" as "invalidPolicyId"
    Then The response status code should be 500
    And The response should contain "Policy specification validation failure"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A common operation policy survives an export/delete/import round-trip: create it, export it as a YAML-format
  # archive, delete it (it disappears from the tenant list), re-import the archive (it re-appears), and a second
  # import of the same archive is rejected as a duplicate (409). Also asserts a non-existing export is a 404.
  # Ports the export/import slice of OperationPolicyTestCase. Runs as admin (op-policy management is admin-scoped).
  @cap:publisher @feat:operation-policies @rule:import-export @type:regression @legacy:OperationPolicyTestCase
  Scenario Outline: A common operation policy survives an export/delete/import round-trip as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "rtPolicyId"
    Then The response status code should be 201

    # Export the policy as a YAML-format archive; a non-existing policy export is a 404.
    When I export the common operation policy named "custom_add_common_header" version "v1" format "yaml" as "rtArchive"
    And I export a non-existing common operation policy named "no_such_policy_xyz" version "v1" format "yaml" expecting status 404

    # Delete it — it disappears from the tenant's common-policy list.
    When I delete the common operation policy "rtPolicyId"
    Then The response status code should be 200
    When I retrieve available common policies
    Then The response status code should be 200
    And The response should not contain "custom_add_common_header"

    # Import the exported archive back — it re-appears in the list.
    When I import the common operation policy archive "rtArchive" as "rtReimportedId"
    Then The response status code should be 201
    When I retrieve available common policies
    Then The response status code should be 200
    And The response should contain "custom_add_common_header"

    # Importing the same archive again is rejected as a duplicate.
    When I import the common operation policy archive "rtArchive" as "rtDuplicateId"
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
