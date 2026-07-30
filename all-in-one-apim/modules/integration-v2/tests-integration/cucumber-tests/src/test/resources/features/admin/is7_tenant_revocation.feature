@cap:admin @feat:external-key-manager @rule:revocation @type:regression @dep:gateway @cleanup
Feature: External Key Manager Tenant Token Revocation

  Runs the self-validate token arc of is7_key_manager_boot.feature in the tenant1.com org. Key-manager configs
  are org-scoped (a super-tenant-registered key manager exists only in the super tenant - pinned by the T1 tenant
  isolation scenario), so the tenant admin first registers its OWN WSO2-IS-7 key manager (same IS,
  its own org, unique name) before the API/app/keys/subscription arc. The revocation leg pins a cross-tenant behaviour that
  nothing else covers: the IS-side revocation notification carries the IS tenancy of the revoked token
  (tenantDomain=carbon.super - all KM-DCR'd apps live in IS's super tenant), NOT the APIM org that owns the key
  manager, so this scenario proves the gateway still enforces the revocation for a tenant-org token (the
  revoked-JWT lookup is keyed by the token, not the notification's tenantDomain). per-scenario cleanup via the cleanup hook (applications are swept before key
  managers, so the tenant KM's key mappings are gone before its delete).

  Scenario: A tenant-org IS7 token invokes through the gateway and is rejected after revocation at IS
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "tenantKmId"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{tenantKmIdName}}", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    # The tenant API's context already carries the /t/tenant1.com prefix - invoke it verbatim.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Revoke at IS; prove IS itself reflects it, then that the tenant-org gateway context enforces it.
    When I revoke the access token at the external key manager
    Then The response status code should be 200
    And the access token should be inactive at the external key manager introspection endpoint
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
