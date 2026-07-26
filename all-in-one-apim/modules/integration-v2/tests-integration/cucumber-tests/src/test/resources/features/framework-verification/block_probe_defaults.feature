@framework
Feature: Framework Verification 4.14 (defaults) - a block with no overlay param defaults safely

  Drives the no-parameter path of BlockLifecycleListener. This block's <test> sets no tomlOverlayPath,
  so the listener must fall back to the base deployment.toml and still boot a ready container - it must
  not NPE on the absent overlay. The booted server's deployment.toml must therefore carry no overlay
  marker (it is the unmodified base toml).

  Scenario: A block with no overlay param boots on the base toml
    Then I wait for the APIM server to be ready
    And the shared baseUrl is present
    And I record the block observation
    And the in-container deployment.toml does not contain the marker "FV-4.14-OVERLAY-MARKER"
