@cleanup
Feature: API Governance Rulesets

  Ports the legacy RulesetMgtTestCase: governance ruleset CRUD over /api/am/governance/v1/rulesets. A ruleset
  is a set of spectral rules uploaded as a YAML/JSON file via multipart, typed by ruleCategory (SPECTRAL),
  ruleType (API_DEFINITION) and artifactType (REST_API). Governance is its own product API with its own token
  scopes (apim:gov_*), so each scenario mints a governance token in addition to the admin token. Covers: the
  built-in default rulesets are present; the create -> update -> delete lifecycle; rejection of invalid ruleset
  content on both create and update (400 / 990120); and creation from JSON content. The "cannot delete a ruleset
  attached to a policy" case (409 / 990101) lives in policies.feature, where a policy can be built to attach it.
  ×2 tenant, since ruleset management is tenant-scoped and tenant-agnostic. Teardown via @cleanup deletes any
  created ruleset with the governance token.

  @cap:governance @feat:rulesets @type:smoke @legacy:RulesetMgtTestCase
  Scenario Outline: The built-in default rulesets are present as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I retrieve all governance rulesets
    Then The response status code should be 200
    And The response should contain "WSO2 API Management Guidelines"
    And The response should contain "WSO2 REST API Design Guidelines"
    And The response should contain "OWASP Top 10"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  @cap:governance @feat:rulesets @type:regression @legacy:RulesetMgtTestCase
  Scenario Outline: Create, update, then delete a governance ruleset as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance ruleset "${UNIQUE:Ruleset}" from content file "artifacts/apim-governance/simple-spectral-ruleset.yaml" as "rulesetId"
    Then The response status code should be 201
    When I update the governance ruleset "rulesetId" with name "${UNIQUE:UpdatedRuleset}" content file "artifacts/apim-governance/simple-spectral-ruleset.yaml" description "Updated ruleset description" and documentation link "https://wso2.com/updated"
    Then The response status code should be 200
    And The response should contain "Updated ruleset description"
    And The response should contain "https://wso2.com/updated"
    When I delete the governance ruleset "rulesetId"
    Then The response status code should be 204

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  @cap:governance @feat:rulesets @type:negative @legacy:RulesetMgtTestCase
  Scenario Outline: An invalid ruleset is rejected on create as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I attempt to create a governance ruleset "${UNIQUE:BadRuleset}" from content file "artifacts/apim-governance/invalid-spectral-ruleset.yaml"
    Then The response status code should be 400
    And The response should contain "990120"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  @cap:governance @feat:rulesets @type:negative @legacy:RulesetMgtTestCase
  Scenario Outline: An invalid update of a ruleset is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance ruleset "${UNIQUE:Ruleset}" from content file "artifacts/apim-governance/simple-spectral-ruleset.yaml" as "rulesetId"
    Then The response status code should be 201
    When I update the governance ruleset "rulesetId" with name "${UNIQUE:Ruleset}" content file "artifacts/apim-governance/invalid-spectral-ruleset.yaml" description "Attempted invalid update" and documentation link "https://wso2.com"
    Then The response status code should be 400
    And The response should contain "990120"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  @cap:governance @feat:rulesets @type:regression @legacy:RulesetMgtTestCase
  Scenario Outline: Create a governance ruleset from JSON content as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance ruleset "${UNIQUE:JsonRuleset}" from content file "artifacts/apim-governance/simple-spectral-ruleset.json" as "jsonRulesetId"
    Then The response status code should be 201
    When I delete the governance ruleset "jsonRulesetId"
    Then The response status code should be 204

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |
