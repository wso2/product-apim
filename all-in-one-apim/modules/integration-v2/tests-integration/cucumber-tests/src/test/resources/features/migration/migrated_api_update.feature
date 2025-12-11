Feature: Migrated API Updates

  Background:
    Given The system is ready and I have valid access tokens for current user

# Step 1: Find the api
  Scenario Outline: Migrated API Retrieval
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"

    Examples:
      | apiName                  | apiVersion   | apiID         | apiUpdatePayload              |
      | ADPRestAPI               | 1.0.0        | RestApiId     | ADPRestAPIPayload             |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  | ADPGraphQLAPIPayload          |
      | ADPPhoneVerificationAPI  | 1.0.0        | SoapApiId     | ADPPhoneVerificationAPIPayload|
      | ADPIfElseAPI             | 1.0.0        | AsyncApiId    | ADPIfElseAPIPayload           |


# Step 2: Update Runtime configurations(refer artifacts/payloads/MigratedAPIs for existing configs)
  Scenario Outline: Update Runtime configurations
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
   | apiID            |  apiUpdatePayload                 | configType              | configValue          |
   | RestApiId        | ADPRestAPIPayload                 |responseCachingEnabled   | true                 |
   | RestApiId        | ADPRestAPIPayload                 |enableSchemaValidation   | false                |
   | RestApiId        | ADPRestAPIPayload                 |transport                | ["https"]            |
   | RestApiId        | ADPRestAPIPayload                 |corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
   | GraphQLApiId     | ADPGraphQLAPIPayload              |responseCachingEnabled   | true                 |
   | GraphQLApiId     | ADPGraphQLAPIPayload              |enableSchemaValidation   | true                 |
   | GraphQLApiId     | ADPGraphQLAPIPayload              |transport                | ["https"]            |
   | GraphQLApiId     | ADPGraphQLAPIPayload              |corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
   | SoapApiId        | ADPPhoneVerificationAPIPayload    |responseCachingEnabled   | true                 |
   | SoapApiId        | ADPPhoneVerificationAPIPayload    |enableSchemaValidation   | true                 |
   | SoapApiId        | ADPPhoneVerificationAPIPayload    |transport                | ["https"]            |
   | SoapApiId        | ADPPhoneVerificationAPIPayload    |corsConfiguration        | {"corsConfigurationEnabled":true,"accessControlAllowOrigins":["*"],"accessControlAllowMethods":["GET","POST"],"accessControlAllowHeaders":["Authorization"],"accessControlAllowCredentials":true} |
   | AsyncApiId       | ADPIfElseAPIPayload               |apiThrottlingPolicy      | "Unlimited"          |


# Step 3: Modify resources(refer artifacts/payloads/MigratedAPIs for existing configs)
  Scenario Outline: Update Resources
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The response should contain "newlyAddedResource"

    Examples:
      | apiID            |  apiUpdatePayload                 | configType       | configValue                                             |
      | RestApiId        | ADPRestAPIPayload                 |operations        | [{"verb": "POST", "target": "/newlyAddedResource"}]     |
      | GraphQLApiId     | ADPGraphQLAPIPayload              |operations        | [{"verb": "QUERY", "target": "newlyAddedResource"}]     |
      | SoapApiId        | ADPPhoneVerificationAPIPayload    |operations        | [{"verb": "POST", "target": "/newlyAddedResource"}]     |
      | AsyncApiId       | ADPIfElseAPIPayload               |operations        | [{"verb": "SUBSCRIBE", "target": "/newlyAddedResource"}]|


# Step 4: subscription tiers/plans(refer artifacts/payloads/MigratedAPIs for existing configs)
  Scenario Outline: Update Subscription plans
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | apiID            |  apiUpdatePayload                 | configType     | configValue                   |
      | RestApiId        | ADPRestAPIPayload                 |policies        | ["Gold","Unlimited"]          |
      | GraphQLApiId     | ADPGraphQLAPIPayload              |policies        | ["ADPBrass"]                  |
      | SoapApiId        | ADPPhoneVerificationAPIPayload    |policies        | ["Gold", "ADPBrass"]          |
      | AsyncApiId       | ADPIfElseAPIPayload               |policies        | ["AsyncSilver", "AsyncGold"]  |


# Step 5: Modify operations by updating existing scopes to resource(refer artifacts/payloads/MigratedAPIs for existing configs)
  Scenario Outline: Update Scopes
    When I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

# Modify APIs with existing scopes
    Examples:
      | apiID            |  apiUpdatePayload                 | configType       | configValue                                                                                           |
      | RestApiId        | ADPRestAPIPayload                 |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"POST","uriMapping":null,"throttlingPolicy":"Unlimited","target":"/newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["adp-shared-scope-without-roles"],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |
      | GraphQLApiId     | ADPGraphQLAPIPayload              |operations        | [{"payloadSchema":null,"operationPolicies":{"request":[],"response":[],"fault":[]},"verb":"QUERY","uriMapping":null,"throttlingPolicy":"Unlimited","target":"newlyAddedResource","amznResourceContentEncode":null,"usedProductIds":[],"amznResourceName":null,"id":"","scopes":["adp-admin"],"amznResourceTimeout":null,"authType":"Application & Application User"}]    |


## Step 6: Custom properties (refer artifacts/payloads/MigratedAPIs for existing configs)
  Scenario Outline: Update Custom Properties
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    Examples:
      | apiID            |  apiUpdatePayload                 | configType              | configValue                                                    |
      | RestApiId        | ADPRestAPIPayload                 |additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | GraphQLApiId     | ADPGraphQLAPIPayload              |additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | SoapApiId        | ADPPhoneVerificationAPIPayload    |additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|
      | AsyncApiId       | ADPIfElseAPIPayload               |additionalProperties     | [{"name": "newProperty", "value": "newValue", "display": true}]|

# Step 7: Endpoint modifications(includes updates to endpoint url, configurations, and security)
# Note: artifacts/payloads/update_api_endpoint.json carries update for endpoint : urls, advanced configurations and security

  Scenario Outline: Update Endpoint configurations
    When I prepare an endpoint update with "<type>", "<productionEndpoint>" and "<sandboxEndpoint>" as "<endpointUpdateConfig>"
    And I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "endpointConfig" and value:
      """
      <endpointUpdateConfig>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "endpointConfig" as:
      """
      <endpointUpdateConfig>
      """

    Examples:
      | apiID            |  apiUpdatePayload                 | type              | productionEndpoint                    |   sandboxEndpoint                   |
      | RestApiId        | ADPRestAPIPayload                 |https              | https://jsonplaceholder.typicode.com/ |https://jsonplaceholder.typicode.com/|
      | GraphQLApiId     | ADPGraphQLAPIPayload              |https              | https://jsonplaceholder.typicode.com/ |https://jsonplaceholder.typicode.com/|
      | SoapApiId        | ADPPhoneVerificationAPIPayload    |https              | https://jsonplaceholder.typicode.com/ |https://jsonplaceholder.typicode.com/|
      | AsyncApiId       | ADPIfElseAPIPayload               |WS                 | wss://ws.postman-echo.com/raw         |wss://ws.postman-echo.com/raw        |