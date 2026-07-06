@framework
Feature: Framework Verification 4.8 - erroring probe still releases its block container

  Companion to block_probe_fail.feature. The probe waits for readiness, records its observation, then
  throws an uncaught exception (an ERROR rather than an assertion FAILURE). onFinish must still stop the
  container and release its ports - teardown is not conditional on the test outcome.

  Scenario: An erroring probe must not prevent block teardown
    Then I wait for the APIM server to be ready
    And I record the block observation
    And I deliberately error the probe
