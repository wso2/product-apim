Feature: Custom Header Test System Initialization

  Scenario: Initialize the NodeApp servers and a custom API Manager
    Given I have initialized the NodeApp server container
    And I have initialized the API Manager container with label "customHeader" and deployment toml changes file path at "/src/test/resources/artifacts/configFiles/customHeaderTest"
    And I wait for the APIM server to be ready
