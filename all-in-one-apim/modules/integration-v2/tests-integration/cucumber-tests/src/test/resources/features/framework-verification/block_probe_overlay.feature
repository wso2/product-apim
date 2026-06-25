@framework
Feature: Framework Verification 4.14 (overlay) - a block with an overlay param ships it into the container

  Drives the tomlOverlayPath path of BlockLifecycleListener. This block's <test> points tomlOverlayPath
  at a generated toml carrying a distinctive marker. The listener must ship that toml into the booted
  container, so cat-ing /repository/conf/deployment.toml inside the live server must reveal the marker -
  proving the parameter actually reached the running container, not merely that the file exists on disk.

  Scenario: A block with an overlay param boots on the overlaid toml
    Then I wait for the APIM server to be ready
    And the shared baseUrl is present
    And I record the block observation
    And the in-container deployment.toml contains the marker "FV-4.14-OVERLAY-MARKER"
