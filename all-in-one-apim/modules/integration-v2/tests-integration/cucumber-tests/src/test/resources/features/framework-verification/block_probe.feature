@framework
Feature: Framework Verification 4.x - per-block probe

  Minimal probes for the parallel-on-shared-container lane. The block's single APIM server is booted
  by BlockLifecycleListener (once per TestNG <test>), not inside these scenarios. Each probe only
  asserts framework state - the server is ready and the block's shared baseUrl/gateway URL are
  published into the shared scope - so the 4.4-4.12 gates can reason about boot-once, readiness,
  skip-on-failure, teardown/release, and two-level concurrency without any server-flow logic.

  Scenario: The block server is ready and its shared URLs are published
    Then I wait for the APIM server to be ready
    And the shared baseUrl is present
    And the shared gateway URL is present
    And I record the block observation
