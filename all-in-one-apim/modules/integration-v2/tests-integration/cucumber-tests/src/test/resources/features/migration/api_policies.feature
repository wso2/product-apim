Feature: Migrated Applications

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Create APIs, Application and find APIs
  Scenario: Create/Find API and create application
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "RestAPIId" and deployed it
    And I retrieve the "apis" resource with id "RestAPIId"
    And I put the response payload in context as "RestAPIPayload"
    And I wait for deployment of the resource in "RestAPIPayload"
    And I publish the "apis" resource with id "RestAPIId"

    When I find the apiUUID of the API created with the name "APIM18PublisherTest" and version "1.0.0" as "migratedAPIId"
    And I retrieve the "apis" resource with id "migratedAPIId"
    Then The response status code should be 200
    And I put the response payload in context as "migratedAPIPayload"

    When I have set up a application with keys
    Then The response status code should be 200

  # Step 2: Create a new common policy
  Scenario: Create new common policy
    When I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "newCommonPolicyId"
    Then The response status code should be 201

  # Step 3: Add common policy at resource level(DELETE resources)
  Scenario Outline: Resource Level policies
    # Update subscription tiers to include "Unlimited" : for generalize the below steps
    When I update the "apis" resource "<apiId>" and "<apiPayload>" with configuration type "policies" and value:
      """
      ["Bronze","Gold","Unlimited"]
      """
    And I retrieve the "apis" resource with id "<apiId>"
    And I put the response payload in context as "<apiPayload>"
    And The "apis" resource should reflect the updated "policies" as:
      """
     ["Bronze","Gold","Unlimited"]
      """

    When I update the "apis" resource "<apiId>" and "<apiPayload>" with configuration type "operations" and value:
    """
      [
        {
            "target": "/customers/{id}",
            "verb": "GET",
            "authType": "Application & Application User",
            "throttlingPolicy": "Unlimited",
            "scopes": [],
            "operationPolicies": {
                "request": [],
                "response": [],
                "fault": []
            }
        },
        {
            "target": "/customers/{id}",
            "verb": "DELETE",
            "authType": "Application & Application User",
            "throttlingPolicy": "Unlimited",
            "scopes": [],
            "operationPolicies": {
                "request": [
                    {
                        "policyName": "custom_add_common_header",
                        "policyVersion": "v1",
                        "policyType": "common",
                        "parameters": {}
                    }
                ],
                "response": [
                    {
                        "policyName": "custom_add_common_header",
                        "policyVersion": "v1",
                        "policyType": "common",
                        "parameters": {}
                    }
                ],
                "fault": []
            }
        }
      ]
    """
    Then The response status code should be 200

    When I retrieve the "apis" resource with id "<apiId>"
    And I put the response payload in context as "<apiPayload>"
    And I deploy the API with id "<apiId>"
    Then The response status code should be 201
    And I wait until "apis" "<apiId>" revision is deployed in the gateway

    # These responses should not include x-common-header since it was only applied to DELETE resource
    When I subscribe to resource "<apiId>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should not contain the header "x-common-header" with value "x-common-value"

    # These responses should include x-common-header
    When I invoke the API resource at path "<apiResource>" with method "DELETE" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "x-common-header" with value "x-common-value"
    And I delete the subscription with id "<subscriptionID>"

    # Remove created revision
    When I undeploy revision "revisionId" of "apis" resource "<apiId>"
    And I Delete the "apis" resource revision with "revisionId" for "<apiId>"
    Then The response status code should be 200

    Examples:
      | apiId           | apiPayload         | subscriptionID        | apiResource                        |
      |  RestAPIId      |  RestAPIPayload    | restSubscriptionId    |/apiTestContext/1.0.0/customers/126/|
      |  migratedAPIId  |  migratedAPIPayload| migratedSubscriptionId|/apiContext/1.0.0/customers/127/    |


  # Step 4: Add common policy at API level
  Scenario Outline: Attach common policies to APIs
    When I update the "apis" resource "<apiId>" and "<apiPayload>" with configuration type "apiPolicies" and value:
      """
        {
          "request": [
          {
            "policyName": "custom_add_common_header",
            "policyVersion": "v1",
            "policyType": "common",
            "parameters": {}
          }
          ],
          "response": [
          {
            "policyName": "custom_add_common_header",
            "policyVersion": "v1",
            "policyType": "common",
            "parameters": {}
          }
          ],
          "fault": []
        }
      """
    Then The response status code should be 200

    When I retrieve the "apis" resource with id "<apiId>"
    And I put the response payload in context as "<apiPayload>"
    And I deploy the API with id "<apiId>"
    Then The response status code should be 201
    And I wait until "apis" "<apiId>" revision is deployed in the gateway

    When I subscribe to resource "<apiId>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "x-common-header" with value "x-common-value"
    And I delete the subscription with id "<subscriptionID>"

    # Remove created revision
    When I undeploy revision "revisionId" of "apis" resource "<apiId>"
    And I Delete the "apis" resource revision with "revisionId" for "<apiId>"
    Then The response status code should be 200

      Examples:
      | apiId           | apiPayload         | subscriptionID        | apiResource                        |
      |  RestAPIId      |  RestAPIPayload    | restSubscriptionId    |/apiTestContext/1.0.0/customers/123/|
      |  migratedAPIId  |  migratedAPIPayload| migratedSubscriptionId|/apiContext/1.0.0/customers/123/    |


  # Step 5: Add API specific policies at API level
  Scenario Outline: Create API specific policies
    When I create a new API specific policy for api "<apiId>" with spec "artifacts/payloads/policySpecFiles/custom_add_api_specific_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_api_specific_header.yaml" as "apiLevelPolicyId"
    Then The response status code should be 201
    And I retrieve the "apis" resource with id "<apiId>"
    And I put the response payload in context as "<apiPayload>"

    When I update the "apis" resource "<apiId>" and "<apiPayload>" with configuration type "apiPolicies" and value:
      """
        {
          "request": [
          {
            "policyName": "custom_add_api_specific_header",
            "policyVersion": "v1",
            "policyType": "api",
            "parameters": {}
          }
          ],
          "response": [
          {
            "policyName": "custom_add_api_specific_header",
            "policyVersion": "v1",
            "policyType": "api",
            "parameters": {}
          }
          ],
          "fault": []
        }
      """
    Then The response status code should be 200

    When I retrieve the "apis" resource with id "<apiId>"
    And I put the response payload in context as "<apiPayload>"
    When I deploy the API with id "<apiId>"
    Then The response status code should be 201
    And I wait until "apis" "<apiId>" revision is deployed in the gateway

    When I subscribe to resource "<apiId>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "x-specific-header" with value "x-specific-value"
    And I delete the subscription with id "<subscriptionID>"

    When I delete the api "<apiId>" specific policy "apiLevelPolicyId"
    Then The response status code should be 200


      Examples:
        | apiId           | apiPayload         | subscriptionID        | apiResource                        |
        |  RestAPIId      |  RestAPIPayload    | restSubscriptionId    |/apiTestContext/1.0.0/customers/123/|
        |  migratedAPIId  |  migratedAPIPayload| migratedSubscriptionId|/apiContext/1.0.0/customers/123/    |


  # Step 6: Add global policies
  Scenario: Add Global policies
      When I retrieve available common policies
      Then The response status code should be 200
      And I find the "id" with name "addHeader" as "existingPolicyId"

      When I put JSON payload from file "artifacts/payloads/policySpecFiles/custom_global_policy.json" in context as "globalPolicyPayload"
      And I create a new global policy as "globalPolicyId" with "globalPolicyPayload"
      Then The response status code should be 201

      When I put the following JSON payload in context as "gatewayPolicyPayload"
      """
      [
        {
        "gatewayDeployment" : true,
        "gatewayLabel" : "{{gatewayEnvironment}}"
        }
      ]
      """
      And I engage the gateway policy mapping "globalPolicyId" to the gateways "gatewayPolicyPayload"
      Then The response status code should be 200

  Scenario Outline: Verify global policies
    When I subscribe to resource "<apiId>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "custom_global_header" with value "custom_global_value"
    And I delete the subscription with id "<subscriptionID>"

    Examples:
      | apiId           | subscriptionID        | apiResource                        |
      |  RestAPIId      | restSubscriptionId    |/apiTestContext/1.0.0/customers/123/|
      |  migratedAPIId  | migratedSubscriptionId|/apiContext/1.0.0/customers/123/    |

 # Step 6: Remove global policies, undeploy to gateway
  Scenario: Remove global policy
    When I put the following JSON payload in context as "gatewayPolicyPayload"
      """
      [
        {
        "gatewayDeployment" : false,
        "gatewayLabel" : "{{gatewayEnvironment}}"
        }
      ]
      """
    And I engage the gateway policy mapping "globalPolicyId" to the gateways "gatewayPolicyPayload"
    Then The response status code should be 200

 # Step 7: Remove other created resources
  Scenario: Remove resources
    When I delete the "gateway-policies" resource with id "globalPolicyId"
    Then The response status code should be 200

    When I delete the "operation-policies" resource with id "newCommonPolicyId"
    Then The response status code should be 200

    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
