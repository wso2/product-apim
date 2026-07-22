@setup
Feature: Setup - WSO2 IS 7.x scope-protected API for role-based authorization

  Provisions everything the role-based authorization scenarios reuse (once, via the _setup_ prefix): a SHARED
  scope bound to a plain APIM role (the WSO2-IS-7 connector creates the derived IS role system_primary_<role> and
  its scope binding in IS), a scope-protected API (GET operation requires that shared scope), an application with
  IS-issued keys subscribed to that API, and two IS users - one holding the mapped role and one without it.
  Asserts create success and that the connector created the role in IS. Cleaned up by the runner's AfterClass
  sweep (the IS users vanish with the ephemeral IS container).

  Scenario: Provision the scope-protected API, application and IS users
    Given The system is ready
    # Register the external key manager first (admin capability): the shared-scope create below fans
    # registerScope to the tenant's key managers, which is what creates the IS-side role.
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "roleEnfKm"
    Then The response status code should be 201
    When I create a shared scope bound to a new IS7 role, storing the expected IS7 role name as "is7IsRole"
    Then The response status code should be 201
    And the role stored as "is7IsRole" should exist at the external key manager
    When I create and deploy a scope-protected API requiring the shared scope as "scopedApiId"
    When I publish the "apis" resource with id "scopedApiId"
    Then The lifecycle status of API "scopedApiId" should be "Published"
    When I retrieve the "apis" resource with id "scopedApiId"
    And I extract response field "context" and store it as "scopedApiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{roleEnfKmName}}", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "scopedApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    And I create an IS user "is7roleuser" with password "Wso2Test123!" assigned the IS role stored as "is7IsRole"
    And I create an IS user "is7noroleuser" with password "Wso2Test123!" assigned the IS role stored as ""
