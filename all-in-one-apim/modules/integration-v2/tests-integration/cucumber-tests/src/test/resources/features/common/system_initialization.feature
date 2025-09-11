Feature: System Initialization

  Scenario: Initialize the NodeApp servers and the API Manager
    Given I have initialized the NodeApp server container
    And I have initialized the Default API Manager container
    And I wait for 5 seconds
