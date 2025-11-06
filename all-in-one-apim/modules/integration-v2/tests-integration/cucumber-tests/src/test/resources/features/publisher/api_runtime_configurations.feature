Feature: API Runtime Configurations
  This feature validates runtime configuration updates (like caching, schema validation, CORS, and transports)
  for an API created.

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Take an existing API or creating an API
    # Step 1: Create base API - "createdApiId" stored in context, and consider create_apim_test_api.json payload
    Given I have created an api and deployed it

  Scenario Outline: Update runtime configuration of API
#    When I update the API with "<apiID>" configuration type "<configType>" and value "<configValue>"
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
      | apiID         | configType               | configValue          |
      | createdApiId  | responseCachingEnabled   | true                 |
      | createdApiId  | cacheTimeout             | 400                  |
      | createdApiId  | enableSchemaValidation   | true                 |
      | createdApiId  | transport                | ["http","https"]     |
      | createdApiId  | corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
      | createdApiId  | responseCachingEnabled   | false                |
      | createdApiId  | enableSchemaValidation   | false                |
      | createdApiId  | transport                | ["https"]            |
      | createdApiId  | corsConfiguration        | {"accessControlAllowOrigins":[],"accessControlAllowCredentials":false,"corsConfigurationEnabled":false,"accessControlAllowHeaders":[],"accessControlAllowMethods":[]} |

  Scenario: Remove the API
    When I delete the API with id "<createdApiId>"
    Then The response status code should be 200


