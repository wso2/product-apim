Feature: API Other Common Configurations
  This feature validates other configuration updates (Subscription plans, and Custom properties, Resource definitions )
  for an API created.

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Take an existing API or creating an API
    # Step 1: Create base API - "createdApiId" stored in context, and consider create_apim_test_api.json payload
    Given I have created an api and deployed it

  Scenario Outline: Update Subscription of API
    When I update the API with "<apiID>" configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    And The API should reflect the updated "<configType>" as:
      """
      <configValue>
      """
    Examples:
      | apiID         | configType               | configValue                   |
      | createdApiId  | policies                 | ["Gold","Unlimited"]          |
      | createdApiId  | policies                 | ["Bronze","Gold","Silver"]    |

  Scenario Outline: Configuring Custom properties
    When I update the API with "<apiID>" configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    And The API should reflect the updated "<configType>" as:
      """
      <configValue>
      """
    Examples:
      | apiID         | configType               | configValue                                                    |
      | createdApiId  | additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | createdApiId  | additionalProperties     | []                                                             |

  Scenario Outline: Configuring Resources
    When I update the API with "<apiID>" configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200

    Examples:
      | apiID         | configType               | configValue                                                     |
      | createdApiId  | operations               | [{"verb": "POST", "target": "/comments"}]                       |
      | createdApiId  | operations               | [{"verb": "GET", "target": "/customers/{id}"}]                  |

  Scenario: Remove the API
    When I delete the API with id "<createdApiId>"
    Then The response status code should be 200



