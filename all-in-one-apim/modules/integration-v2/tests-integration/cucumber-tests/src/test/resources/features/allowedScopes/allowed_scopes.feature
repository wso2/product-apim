#Feature: Validate API invocation with allowed scopes
#
#  Background:
##    Given I have initialized test instance
#    Given I have initialized the NodeApp server container
##    And I have initialized the Default API Manager container
#    And I have initialized the Custom API Manager container with label "allowedScope" and deployment toml file path at "/src/test/resources/features/allowedScopes/deployment.toml"
#    And I initialize the Publisher REST API client with username "admin", password "admin" and tenant "carbon.super"
#    And I initialize the Store REST API client with username "admin", password "admin" and tenant "carbon.super"
#
#  Scenario: Create an API with allowed scopes and invoke it based on scopes
#    When I create an API with name "allowedScopesAPI", context "allowedScopesAPI" and version "1.0.0"
#    And I create scope "some_random_scope" with roles "admin"
#    And I create scope "scope2" with roles "admin"
#    And I add scopes "some_random_scope,scope2" to the created API with id "<createdApiId>"
#    And I add "/customers/{id}" operation with scopes "some_random_scope,scope2" to the created API with id "<createdApiId>"
#    And I deploy a revision of the API with id "<createdApiId>"
#    And I publish the API with id "<createdApiId>"
#    And I create an application named "TestAppScope" with throttling tier "Unlimited"
#    And I subscribe to API "<createdApiId>" using application "<createdAppId>" with throttling policy "Gold"
#    And I generate client credentials for application id "<createdAppId>" with key type "PRODUCTION"
#
#    # Access Token with Scope 1
#    And I request an access token using grant type "client_credentials" with scope "some_random_scope"
#    And I invoke API of ID "<createdApiId>" with path "/customers/123" and method GET using access token "<generatedAccessToken>"
#    Then the API response status should be 200
#
#    # Access Token with Scope 2
#    And I request an access token using grant type "client_credentials" with scope "scope2"
#    And I invoke API of ID "<createdApiId>" with path "/customers/123" and method GET using access token "<generatedAccessToken>"
#    Then the API response status should be 200
#
#    # Access Token with Scope 3 (not allowed)
#    And I request an access token using grant type "client_credentials" with scope "scope3"
#    And I invoke API of ID "<createdApiId>" with path "/customers/123" and method GET using access token "<generatedAccessToken>"
#    Then the API response status should be 403
#
#    # Access Token without scope
#    And I request an access token using grant type "client_credentials" without any scope
#    And I invoke API of ID "<createdApiId>" with path "/customers/123" and method GET using access token "<generatedAccessToken>"
#    Then the API response status should be 403
