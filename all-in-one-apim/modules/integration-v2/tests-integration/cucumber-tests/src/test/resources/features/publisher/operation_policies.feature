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

  # A common operation policy survives an export/delete/import round-trip, in BOTH the YAML and JSON archive
  # formats: create it, export it as a <format>-format archive (the extracted archive carries the matching
  # <policy>.<format> spec plus the <policy>.j2 synapse template), delete it (it disappears from the list),
  # re-import the archive (it re-appears — proving <format> IMPORT, not just export), and a second import of the
  # same archive is rejected as a duplicate (409). Also asserts a non-existing export is a 404. Ports the
  # export/import + JSON-content slices of OperationPolicyTestCase (testCommonOperationPolicyExport +
  # testCommonOperationPolicyExportWithJSONContent). Runs as admin (op-policy management is admin-scoped).
  @cap:publisher @feat:operation-policies @rule:import-export @type:regression @legacy:OperationPolicyTestCase
  Scenario Outline: A common operation policy survives an export/delete/import round-trip in <format> format as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "rtPolicyId"
    Then The response status code should be 201

    # Export the policy as a <format>-format archive; the archive carries the matching <format> spec. A
    # non-existing policy export is a 404.
    When I export the common operation policy named "custom_add_common_header" version "v1" format "<format>" as "rtArchive"
    And The exported operation policy archive "rtArchive" should contain a "<format>" spec for policy "custom_add_common_header"
    And I export a non-existing common operation policy named "no_such_policy_xyz" version "v1" format "<format>" expecting status 404

    # Delete it — it disappears from the tenant's common-policy list.
    When I delete the common operation policy "rtPolicyId"
    Then The response status code should be 200
    When I retrieve available common policies
    Then The response status code should be 200
    And The response should not contain "custom_add_common_header"

    # Import the exported <format> archive back — it re-appears in the list (proves <format> import, not just export).
    When I import the common operation policy archive "rtArchive" as "rtReimportedId"
    Then The response status code should be 201
    When I retrieve available common policies
    Then The response status code should be 200
    And The response should contain "custom_add_common_header"

    # Importing the same archive again is rejected as a duplicate.
    When I import the common operation policy archive "rtArchive" as "rtDuplicateId"
    Then The response status code should be 409

    Examples:
      | format | actor             |
      | yaml   | admin             |
      | yaml   | admin@tenant1.com |
      | json   | admin             |
      | json   | admin@tenant1.com |

  # Importing a mismatched/malformed common-operation-policy archive (its inner spec omits the required name and
  # is misnamed vs the archive) is rejected. Ports OperationPolicyTestCase#testImportInvalidCommonOperationPolicy.
  # verify-first: on 4.7.0 this garbage-input path surfaces as HTTP 500 (same as legacy); pinned to characterise
  # it, not to enshrine 500 as the desirable contract.
  @cap:publisher @feat:operation-policies @rule:import-export @type:negative @legacy:OperationPolicyTestCase
  Scenario Outline: Importing a malformed common operation policy archive is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import a malformed common operation policy archive built from spec "artifacts/payloads/policySpecFiles/custom_invalid_header.yaml" and synapse "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" expecting status 500

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Attaching a shipped common operation policy (removeHeader) to an API operation with its required attributes
  # missing (empty parameters) is rejected on update. Ports
  # OperationPolicyTestCase#testOperationPolicyAdditionWithMissingAttributes (400).
  @cap:publisher @feat:operation-policies @type:negative @legacy:OperationPolicyTestCase
  Scenario Outline: Attaching an operation policy with missing required attributes is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "missAttrApiId" and deployed it
    When I attach the common operation policy "removeHeader" to operation 0 of API "missAttrApiId" in flows "request,response" with parameters "{}"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Attaching a policy to a flow it does not support (jsonFault only supports the fault flow, not request/response)
  # is rejected on update. Ports OperationPolicyTestCase#testAddOperationPolicyForNotSupportedFlow (400).
  @cap:publisher @feat:operation-policies @type:negative @legacy:OperationPolicyTestCase
  Scenario Outline: Attaching an operation policy to an unsupported flow is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "badFlowApiId" and deployed it
    When I attach the common operation policy "jsonFault" to operation 0 of API "badFlowApiId" in flows "request,response" with parameters "{}"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Attaching a common operation policy (addHeader, with valid attributes) to an API operation clones it to the
  # API level: the policy recorded on the operation gets a NEW id but an identical md5 (same content). Ports
  # OperationPolicyTestCase#testCommonOperationPolicyCloneToAPILevelWithUpdate.
  @cap:publisher @feat:operation-policies @type:regression @legacy:OperationPolicyTestCase
  Scenario Outline: Attaching a common operation policy clones it to the API level as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "cloneApiId" and deployed it
    When I attach the common operation policy "addHeader" to operation 0 of API "cloneApiId" in flows "request" with parameters "{\"headerName\":\"x-clone-header\",\"headerValue\":\"clone-value\"}"
    Then The response status code should be 200
    And The operation 0 of API "cloneApiId" should have a clone of common policy "addHeader" with a new id and matching md5

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Updating a secret-attribute operation policy with an EMPTY secret value PRESERVES the existing secret (it is
  # not cleared). The API carries the add_secret_headers policy with apiKey=test-api-key-123 (a required Secret
  # attribute); on retrieve the secret is masked to "" but is SET (present), never echoing the real value. A
  # subsequent update that supplies an EMPTY value for apiKey must keep it SET (masked-present), not clear it.
  # This is the publisher-plane preserve-on-empty semantics — the gateway invocation proof of the same behaviour
  # lives in gateway/mediation_policies. Ports OperationPolicyTestCase#testRetrievePolicyWithSecretAttributes +
  # testUpdatePolicyWithSecretAttributes. Runs as admin (secret op-policy management is admin-scoped).
 @cap:publisher @feat:operation-policies @rule:secret-attributes @type:regression @legacy:OperationPolicyTestCase
  Scenario Outline: Updating a secret-attribute policy with an empty value preserves the secret as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/add_secret_headers.j2" and "artifacts/payloads/policySpecFiles/add_secret_headers.yaml" as "secUpdPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_secretpolicy_api.json" as "secUpdApiId" and deployed it

    # On retrieval the secret is masked: the real value is never returned, but apiKey is SET (present, blank).
    When I retrieve the "apis" resource with id "secUpdApiId"
    Then The response status code should be 200
    And The response should not contain "test-api-key-123"
    And The secret parameter "apiKey" of the operation policy in flow "request" of operation 0 of API "secUpdApiId" should be preserved and masked

    # Update the policy parameters supplying an EMPTY apiKey — the existing secret must be PRESERVED, not cleared.
    When I update the parameters of the operation policy in flow "request" of operation 0 of API "secUpdApiId" to "{\"apiKey\":\"\",\"token\":\"\"}"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "secUpdApiId"
    Then The response status code should be 200
    And The response should not contain "test-api-key-123"
    And The secret parameter "apiKey" of the operation policy in flow "request" of operation 0 of API "secUpdApiId" should be preserved and masked

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
