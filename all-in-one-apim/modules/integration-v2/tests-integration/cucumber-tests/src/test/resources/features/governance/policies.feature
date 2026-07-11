@cleanup
Feature: API Governance Policies

  Ports the legacy PolicyMgtTestCase: governance policy CRUD over /api/am/governance/v1/policies. A policy is a
  JSON entity that attaches one or more rulesets, targets governable lifecycle states (e.g. API_UPDATE) and is
  scoped by labels (global). Covers: the built-in default policy is present; the create -> update -> delete
  lifecycle of a policy attaching a ruleset; and the ruleset<->policy integrity rule that a ruleset attached to
  a policy cannot be deleted (409 / 990101) — placed here (rather than in rulesets.feature) because it needs a
  policy to attach the ruleset. ×2 tenant, since policy management is tenant-scoped and tenant-agnostic.
  Teardown via @cleanup deletes governance policies before their rulesets (a policy references its rulesets).

  @cap:governance @feat:policies @type:smoke @legacy:PolicyMgtTestCase
  Scenario Outline: The built-in default policy is present as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I retrieve all governance policies
    Then The response status code should be 200
    And The response should contain "WSO2 API Management Best Practices"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  @cap:governance @feat:policies @type:regression @legacy:PolicyMgtTestCase
  Scenario Outline: Create, update, then delete a governance policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance ruleset "${UNIQUE:PolicyRuleset}" from content file "artifacts/apim-governance/simple-spectral-ruleset.yaml" as "rulesetId"
    Then The response status code should be 201
    When I create a governance policy "${UNIQUE:Policy}" attaching ruleset "rulesetId" as "policyId"
    Then The response status code should be 201
    When I update the governance policy "policyId" setting its description to "Updated policy description"
    Then The response status code should be 200
    And The response should contain "Updated policy description"
    When I delete the governance policy "policyId"
    Then The response status code should be 204

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  @cap:governance @feat:policies @type:negative @legacy:PolicyMgtTestCase @legacy:RulesetMgtTestCase
  Scenario Outline: A ruleset attached to a policy cannot be deleted as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance ruleset "${UNIQUE:AttachedRuleset}" from content file "artifacts/apim-governance/simple-spectral-ruleset.yaml" as "attachedRulesetId"
    Then The response status code should be 201
    When I create a governance policy "${UNIQUE:AttachingPolicy}" attaching ruleset "attachedRulesetId" as "attachingPolicyId"
    Then The response status code should be 201
    When I delete the governance ruleset "attachedRulesetId"
    Then The response status code should be 409
    And The response should contain "990101"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |
