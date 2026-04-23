Feature: System Initialization

  Scenario: Initialize the NodeApp servers and the default API Manager
    Given I have initialized the NodeApp server container
    And I have initialized the API Manager container with label "default" and deployment toml changes file path at ""
    And I wait for the APIM server to be ready
