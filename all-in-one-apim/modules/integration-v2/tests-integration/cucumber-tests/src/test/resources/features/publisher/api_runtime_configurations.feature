Feature: API Runtime Configurations
  This feature validates runtime configuration updates (response caching, schema validation, CORS, and transports)
  for an API created.

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Create base APIs
  Scenario Outline: Take an existing API or creating an API
    Given I have created an api from "<payloadFile>" as "<apiID>" and deployed it
    And I retrieve the API with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"

    Examples:
      |payloadFile                                            | apiID        | apiUpdatePayload|
      |artifacts/payloads/create_apim_test_api.json           |  RestAPIId   | RestAPIPayload  |
      |artifacts/payloads/create_apim_test_soap_api.json      |  SoapAPIId   | SoapAPIPayload  |
      |artifacts/payloads/create_apim_test_websocket_api.json | AsyncAPIId   | AsyncAPIPayload |

    Scenario: Create GraphQL API
      When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
      And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "GraphQLAPIId"
      And I retrieve the API with id "GraphQLAPIId"
      And I put the response payload in context as "graphQLAPIPayload"

 # Step 2: Update runtime configurations and check
  Scenario Outline: Update runtime configuration of API
    When I update the API "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the API with id "<apiID>"
    And The API should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | apiID      |  apiUpdatePayload  | configType               | configValue          |
      | RestAPIId  |  RestAPIPayload                  |responseCachingEnabled   | true                 |
      | RestAPIId  |  RestAPIPayload                 |cacheTimeout             | 400                  |
      | RestAPIId  |  RestAPIPayload                  |enableSchemaValidation   | true                 |
      | RestAPIId  |  RestAPIPayload                 |transport                | ["http","https"]     |
      | RestAPIId  |  RestAPIPayload                  |corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
      | RestAPIId  |  RestAPIPayload                 |responseCachingEnabled   | false                |
      | RestAPIId  |  RestAPIPayload                 |enableSchemaValidation   | false                |
      | RestAPIId  |  RestAPIPayload                 |transport                | ["https"]            |
      | RestAPIId  |  RestAPIPayload                  |corsConfiguration        | {"accessControlAllowOrigins":[],"accessControlAllowCredentials":false,"corsConfigurationEnabled":false,"accessControlAllowHeaders":[],"accessControlAllowMethods":[]} |
      | SoapAPIId  |  SoapAPIPayload                  |responseCachingEnabled   | true                 |
      | SoapAPIId  |  SoapAPIPayload                 |cacheTimeout             | 400                  |
      | SoapAPIId  |  SoapAPIPayload                  |enableSchemaValidation   | true                 |
      | SoapAPIId  |  SoapAPIPayload                 |transport                | ["http","https"]     |
      | SoapAPIId  |  SoapAPIPayload                  |corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
      | GraphQLAPIId  |  graphQLAPIPayload                  |responseCachingEnabled   | true                 |
      | GraphQLAPIId  |  graphQLAPIPayload                 |cacheTimeout             | 400                  |
      | GraphQLAPIId  |  graphQLAPIPayload                  |enableSchemaValidation   | true                 |
      | GraphQLAPIId  |  graphQLAPIPayload                 |transport                | ["http","https"]     |
      | GraphQLAPIId  |  graphQLAPIPayload                  |corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
      | AsyncAPIId       | AsyncAPIPayload               |apiThrottlingPolicy      | "Unlimited"          |

 # Step 3: Remove created APIs
  Scenario Outline: Remove the APIs
    When I delete the API with id "<apiID>"
    Then The response status code should be 200

    Examples:
    |   apiID       |
    | RestAPIId     |
    | SoapAPIId     |
    | GraphQLAPIId  |
    | AsyncAPIId    |


