@cleanup
Feature: Key Manager Token Issuance

  Key-manager-plane token issuance across grant/scope variants: JWT-format production tokens, OpenID-scoped
  tokens (+ userinfo), refresh-token re-issuance, and sandbox-key tokens. Runs as admin in both the super
  tenant and tenant1.com. The refresh and sandbox variants invoke the gateway to prove the issued token works
  end-to-end. Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:token-issuance @type:smoke @rule:jwt-format @legacy:JWTTokenFormatTestCase
  Scenario Outline: Generate a production OAuth token in JWT format as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Also covers OpenIDTokenAPITestCase (openid-scoped password-grant token → scope contains openid; userinfo →
  # 200; the tenant-path userinfo variant is covered by the admin@tenant1.com row, whose @domain routes userinfo
  # through /t/<tenant>/).
  @cap:key-manager @feat:token-issuance @type:smoke @rule:openid @legacy:OpenIDTokenTestCase @legacy:OpenIDTokenAPITestCase
  Scenario Outline: Generate an OpenID-scoped token and call userinfo as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And The response should contain "openid"
    When I invoke the OpenID userinfo endpoint using access token "generatedAccessToken"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @type:regression @rule:refresh @dep:gateway @legacy:RefreshTokenTestCase
  Scenario Outline: Re-issue an access token via refresh token and invoke as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
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
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    When I request a new OAuth access token using refresh token "refreshToken"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Also provides parity for TokenAPITestCase (sandbox-key token invokes the API → 200; the production-key
  # password-grant and client-credential token invocations of TokenAPITestCase are covered by the smoke
  # JWT-token scenario above + gateway/rest-invocation).
  @cap:key-manager @feat:token-issuance @type:regression @rule:sandbox @dep:gateway @legacy:SandboxTokenTestCase @legacy:TokenAPITestCase
  Scenario Outline: Issue a sandbox-scoped token and invoke as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateSandboxApplicationKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateSandboxApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "SANDBOX"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:authcode @type:regression @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: Generate an access token via authorization code grant as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token via authorization code grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:authcode-default-scope @type:regression @legacy:JWTTestCase
  Scenario Outline: Auth code grant without scope issues token with 'default' scope as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token via authorization code grant without requesting any scopes
    Then The response status code should be 200
    And I extract response field "scope" and store it as "tokenScope"
    And the actual value of "tokenScope" should match the expected value:
      """
      default
      """

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:scope-in-token @type:regression @legacy:TokenEncryptionScopeTestCase
  Scenario Outline: A requested shared scope is granted in the issued token as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new shared scope as "encTokenScope"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "encApiCreate"
    And I create an "apis" resource with payload "encApiCreate" as "encApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"encTokenScope","displayName":"encTokenScope","description":"scope in token","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["encTokenScope"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "encRevPayload"
    """
    {"description":"scope revision"}
    """
    And I make a request to create a revision for "apis" resource "encApiId" with payload "encRevPayload"
    When I put the following JSON payload in context as "encDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "encApiId" with payload "encDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "encApiId"
    Then The lifecycle status of API "encApiId" should be "Published"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "encAppPayload"
    And I create an application with payload "encAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "encKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "encKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "encSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "encApiId" using application "createdAppId" with payload "encSubPayload" as "encSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "encTokenScope"
    Then The response status code should be 200
    And The response should contain "encTokenScope"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
