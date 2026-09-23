@setup
Feature: Throttle-data registry infrastructure setup

  The distributed throttle-data consumer records its broker subscription in the Carbon governance registry. This
  setup repairs only the required tenant-scoped collection hierarchy when a fresh database did not contain it;
  enforcement coverage remains in conditional_throttling.feature.

  Scenario: Initialize the throttle-data registry for every enforcement tenant
    Given The system is ready
    When I ensure the throttleData registry hierarchy exists as "admin"
    And I ensure the throttleData registry hierarchy exists as "admin@tenant1.com"
    And I gracefully restart the API Manager server
    And the Traffic Manager throttleData publisher and Gateway throttleData consumer are ready within 180 seconds
