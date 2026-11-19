@cleanup
Feature: API Governance Deployment Blocking

  Ports the blocking half of the legacy APIComplianceTestCase: a policy with a BLOCK action on API_DEPLOY
  (attaching the default rulesets a bare API violates) rejects deployment — creating a revision fails with 903300.
  This is synchronous and deterministic, so it runs ×2 tenant. The policy is labelled global, so while it exists
  it blocks every API deployment in the tenant: run this feature only where no other runner deploys concurrently.
  Teardown via @cleanup removes the created API and the governance policy (policy before its rulesets).

  @cap:governance @feat:compliance @type:regression @dep:publisher @legacy:APIComplianceTestCase
  Scenario Outline: A blocking governance policy rejects API deployment as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance policy "${UNIQUE:BlockPolicy}" attaching the default rulesets blocking API deployment as "blockPolicyId"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "govApiPayload"
    And I create an "apis" resource with payload "govApiPayload" as "govApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "govRevPayload"
    """
    {"description":"governance revision"}
    """
    And I attempt to create a revision for "apis" resource "govApiId" with payload "govRevPayload"
    Then The response status code should be 400
    And The error response should have code "903300" and message "Request does not adhere to governance standards"
    When I delete the governance policy "blockPolicyId"
    Then The response status code should be 204

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |
