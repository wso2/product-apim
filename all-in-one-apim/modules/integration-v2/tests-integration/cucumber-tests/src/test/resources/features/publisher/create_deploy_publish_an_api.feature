Feature: Publisher API Creation and Deployment
  This feature tests API creation, deployment, deletion, and error handling via the Publisher REST API.

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1.1: Create Rest apis and soap apis
  Scenario Outline: Create REST/SOAP APIs
    When I put JSON payload from file "<payloadFile>" in context as "<apiPayload>"
    And I create an "apis" resource with payload "<apiPayload>" as "<apiID>"

    Examples:
      |payloadFile                                             | apiPayload          | apiID          |
      |artifacts/payloads/create_apim_test_api.json            | restAPIPayload      |  RestAPIId     |
      |artifacts/payloads/create_apim_test_soap_api.json       | soapAPIPayload      |  SoapAPIId     |
      |artifacts/payloads/create_apim_test_websocket_api.json  | asyncAPIPayload     |  AsyncAPIId     |

  # Step 1.2:Create graphQL API
  Scenario: Create GraphQL API
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "GraphQLAPIId"

  Scenario Outline: Create an API Through the Publisher Rest API

    # Step 2: Verify created APIs
    When  I retrieve the "apis" resource with id "<apiID>"
    Then The response status code should be 200
    And The response should contain "<apiName>"
    And The response should contain "<apiContext>"
    And The response should contain "<apiVersion>"
    And I put the response payload in context as "<retrievedApiPayload>"

    # Step 3: Deploy the API
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And  I make a request to create a revision for "apis" resource "<apiID>" with payload "<createRevisionPayload>"
    And I put the following JSON payload in context as "<deployRevisionPayload>"
    """
    [
      {
        "name": "{{gatewayEnvironment}}",
        "vhost": "localhost",
        "displayOnDevportal": true
      }
    ]
    """
    And I make a request to deploy revision "<revisionId>" of "apis" resource "<apiID>" with payload "<deployRevisionPayload>"
    Then The response status code should be 201
    Then The lifecycle status of API "<apiID>" should be "Created"
    And I wait for deployment of the resource in "<retrievedApiPayload>"

    # Step 4: Publish the API
    And I publish the "apis" resource with id "<apiID>"
    Then The response status code should be 200
    Then The lifecycle status of API "<apiID>" should be "Published"

  Examples:
    | apiID          | apiName        | apiContext           | apiVersion|
    |  RestAPIId     |APIMTest        |apiTestContext        | 1.0.0     |
    |  SoapAPIId     |APIMTestSoap    |apiTestSoapContext    |1.0.0      |
    |  GraphQLAPIId  |APIMTestGraphQL |apiTestGraphQLContext |1.0.0      |
    |  AsyncAPIId    |APIMAsyncTest   |apimAsyncContext      |1.0.0      |


    Scenario: Application creation
      When I have set up a application with keys

    Scenario Outline: Invoking apis
      When I subscribe to resource "<apiID>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
      When I put the following JSON payload in context as "<Payload>"
      """
        <payload>
      """
      And I invoke the API resource at path "<resourcePath>" with method "<method>" using access token "<generatedAccessToken>" and payload "<Payload>"
      Then The response status code should be 200

      When I delete the subscription with id "<subscriptionID>"
      Then The response status code should be 200

      Examples:
        | apiID      | subscriptionID       |  payload                                 | resourcePath                            | method|
        |RestAPIId   | restSubscriptionId   | ""                                       | apiTestContext/1.0.0/customers/123/     | GET   |
        |GraphQLAPIId| graphqlSubscriptionId|  {"query": "{languages{code name}}"}     | apiTestGraphQLContext/1.0.0             | POST  |


    Scenario Outline: Invoking SOAP api
      When I subscribe to resource "<apiID>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
      When I put the following JSON payload in context as "<Payload>"
      """
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <soap:Body>
                    <CheckPhoneNumber xmlns="http://ws.cdyne.com/PhoneVerify/query">
                    <PhoneNumber>18006785432</PhoneNumber>
                    <LicenseKey>18006785432</LicenseKey>
                    </CheckPhoneNumber>
                </soap:Body>
            </soap:Envelope>
      """
      And I invoke the SOAP API at path "<resourcePath>" using access token "<generatedAccessToken>" and payload "<Payload>" and soap action "<soapAction>"
      Then The response status code should be 200

      When I delete the subscription with id "<subscriptionID>"
      Then The response status code should be 200

      Examples:
        | apiID    | subscriptionID     | Payload  | resourcePath                  | soapAction                                               |
        | SoapAPIId| soapSubscriptionId | soapBody | apiTestSoapContext/1.0.0      |  http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber  |

    Scenario: Delete the app
      When I delete the application with id "createdAppId"
      Then The response status code should be 200

    Scenario Outline: Delete the created resources
      When I delete the "apis" resource with id "<apiID>"
      Then The response status code should be 200

      Examples:
        | apiID          |
        |  RestAPIId     |
        |  SoapAPIId     |
        |  GraphQLAPIId  |
        |  AsyncAPIId    |
