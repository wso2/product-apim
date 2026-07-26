@framework
Feature: Framework Verification 7.4b - multi-feature runner handoff (consumer)

  Second feature in the same runner. Proves the id+payload the setup feature stored are visible here (runner-
  local scope is shared across the runner's features, in array order), which is the _setup_* fixture pattern.

  Scenario: the resource created by the setup feature is visible to the consumer feature
    Given I act as "admin"
    When I retrieve the "apis" resource with id "fvHandoffApiId"
    Then The response status code should be 200
