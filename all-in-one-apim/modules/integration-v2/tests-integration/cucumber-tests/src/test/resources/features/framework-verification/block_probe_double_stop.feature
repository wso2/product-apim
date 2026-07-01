@framework
Feature: Framework Verification 4.10 - double-stop is idempotent

  Drives the teardown-idempotency gate. The probe waits for readiness, records its observation, then
  stops the block container itself. onFinish later calls stop() on that already-stopped container, which
  must be a no-op (no exception, no spurious error in the report) and must leave nothing leaked.

  Scenario: Stopping the container twice is a no-op
    Then I wait for the APIM server to be ready
    And I record the block observation
    And I stop the block container
