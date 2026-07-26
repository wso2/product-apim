@framework
Feature: Framework Verification 4.8 - failing probe still releases its block container

  Drives the teardown-on-FAILURE gate. The block's container is booted by BlockLifecycleListener; the
  probe waits for readiness, records its observation, then deliberately FAILS. onFinish must still stop
  the container and release its ports - teardown is not conditional on test success.

  Scenario: A failing probe must not prevent block teardown
    Then I wait for the APIM server to be ready
    And I record the block observation
    And I deliberately fail the probe
