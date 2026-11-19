@cleanup
Feature: Key Manager Token Issuance

  Key-manager-plane token issuance across grant/scope variants: JWT-format production tokens, OpenID-scoped
  tokens (+ userinfo across JWT/OAUTH tokens, primary/secondary-store actors and both user-tenant routes), refresh-token re-issuance, sandbox-key
  tokens for both a JWT-tokenType and an OAUTH-tokenType application, key-generation validation of the application's
  token-expiry additionalProperties, authorization-code grants, and per-role filtering of the scopes actually
  granted in an issued token. Runs as the block-aware super-tenant admin and tenant1.com admin. The refresh, sandbox,
  token-expiry and role-filtering variants invoke the gateway to prove the issued token works end-to-end. It also
  covers SECONDARY.COM subscriber gateway/refresh flows with a separate store publisher. Runs in standard and
  email-user blocks as block-aware primary actors and secondary-store principals in both tenant contexts. The same
  feature also verifies the Publisher, DevPortal, and Admin planes for admin and non-admin actors; exact physical
  usernames are asserted according to the block's standard or email-user identity mode.
  The scenarios tagged for TokenAPITestCase are also selected in the serialized server-restart blocks, where the
  existing issuance, refresh, opaque-token, expiry, invalid-token, and actor examples run with token persistence enabled.
  Teardown via the per-scenario cleanup hook.

  # Legacy EmailUserNameLoginTestCase: the admin identity reaches Publisher, DevPortal, and Admin. The examples
  # run in both block modes; only the physical username assertion changes. Publisher list presence is asserted,
  # while the following creation case proves the actor can actually use the Publisher API.
  @cap:key-manager @feat:token-issuance @type:smoke @rule:email-username @legacy:EmailUserNameLoginTestCase
  Scenario Outline: An administrator reaches the Publisher, DevPortal, and Admin planes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I store the acting actor credentials as "planeAdminName" and "planeAdminPassword"
    Then the actual value of "planeAdminName" should match standard-mode value "<standardUsername>" and email-mode value "<emailUsername>"
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200
    And The response should contain "list"
    When I list the DevPortal applications
    Then The response status code should be 200
    And The response should contain "DefaultApplication"
    When I retrieve all "application" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

    Examples:
      | actor             | standardUsername | emailUsername                         |
      | superTenantAdmin  | admin            | emailAdmin@email.com@carbon.super    |
      | admin@tenant1.com | admin@tenant1.com | admin@email.com@tenant1.com           |

  # Legacy EmailUserNameLoginTestCase's email user has subscriber, publisher, and creator roles. The framework's
  # userKey1 fixture has the same authorization set. Keep one identity through Publisher API creation/listing,
  # DevPortal app creation/listing, and Admin policy reads, so the three plane checks describe the same user.
  @cap:key-manager @feat:token-issuance @type:regression @rule:email-username @legacy:EmailUserNameLoginTestCase
  Scenario Outline: A subscriber-publisher-creator reaches DevPortal and Admin as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I store the acting actor credentials as "planeUserName" and "planeUserPassword"
    Then the actual value of "planeUserName" should match standard-mode value "<standardUsername>" and email-mode value "<emailUsername>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "planeApiCreate"
    And I create an "apis" resource with payload "planeApiCreate" as "planeApiId"
    Then The response status code should be 201
    And The provider of API "planeApiId" should match actor "<actor>"
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "planeApiId" should be in the list of all APIS
    When I generate a unique alphanumeric value and store it as "planeAppName"
    And I put the following JSON payload in context as "planeAppCreate"
    """
    {"name": "{{planeAppName}}", "throttlingPolicy": "Unlimited", "description": "Token-issuance identity plane"}
    """
    And I create an application with payload "planeAppCreate"
    Then The response status code should be 201
    When I list the DevPortal applications
    Then The response status code should be 200
    And The response should contain "{{planeAppName}}"
    When I retrieve all "application" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

    Examples:
      | actor                | standardUsername          | emailUsername                         |
      | userKey1             | testUser1@carbon.super    | testUser1@email.com@carbon.super      |
      | userKey1@tenant1.com | testUser11@tenant1.com    | testUser11@email.com@tenant1.com      |

  # Retain the email-username feature's lower-privilege Publisher path as well: unlike userKey1, publisherUser
  # has creator+publisher but no subscriber role. Prove password-grant authentication, API ownership, and indexed
  # Publisher listing in both tenant contexts and both block identity modes.
  @cap:key-manager @feat:token-issuance @type:regression @rule:email-username
  Scenario Outline: A publisher-only actor authenticates with its block-specific username and creates an API
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I store the acting actor credentials as "emailPublisherName" and "emailPublisherPassword"
    Then the actual value of "emailPublisherName" should match standard-mode value "<standardUsername>" and email-mode value "<emailUsername>"
    And a password-grant token request for the acting user with password "<password>" returns status 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "emailPublisherApiCreate"
    And I create an "apis" resource with payload "emailPublisherApiCreate" as "emailPublisherApiId"
    Then The response status code should be 201
    And The provider of API "emailPublisherApiId" should match actor "<actor>"
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "emailPublisherApiId" should be in the list of all APIS

    Examples:
      | actor                     | standardUsername                 | emailUsername                          | password       |
      | publisherUser             | publisherUser1@carbon.super      | publisherUser1@email.com@carbon.super  | publisherUser1  |
      | publisherUser@tenant1.com | publisherUser11@tenant1.com      | publisherUser11@email.com@tenant1.com  | publisherUser11 |

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
    And I generate client credentials for fresh application id "createdAppId" with payload "generateApplicationKeysPayload", retrying on 409 stale mapping conflicts
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format

    Examples:
      | actor             |
      | superTenantAdmin             |
      | admin@tenant1.com |

  # TokenAPITestCase's secondary-store actor path. Keep the provider and token owner explicit: SECONDARY.COM publisher
  # creates and publishes the API; SECONDARY.COM subscriber owns the app, subscription, and password-grant token.
  @cap:key-manager @feat:token-issuance @type:regression @rule:store-principal @dep:gateway @legacy:TokenAPITestCase
  Scenario Outline: A secondary-store principal's token invokes a published API as <storeActor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    Given The system is ready and I have valid devportal access token as "<storeActor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
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
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | storeActor                                | providerActor                            |
      | SECONDARY.COM/subscriberUser1             | SECONDARY.COM/publisherUser1             |
      | SECONDARY.COM/subscriberUser1@tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

  # RefreshTokenTestCase's secondary-store actor path: assert the replacement token itself reaches the backend.
  @cap:key-manager @feat:token-issuance @type:regression @rule:store-principal @dep:gateway @legacy:RefreshTokenTestCase @legacy:TokenAPITestCase
  Scenario Outline: A secondary-store principal refreshes a token and invokes as <storeActor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    Given The system is ready and I have valid devportal access token as "<storeActor>"
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
    And The value of response field "throttlingPolicy" should be "Unlimited"
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    # Check the original opaque token before exchanging it, matching the source-only RefreshTokenTestCase flow.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix               | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/    | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"
    When I request a new OAuth access token using refresh token "refreshToken"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "refreshedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{apiContext}}" with access token "refreshedAccessToken" using these request variants:
      | pathSuffix               | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/    | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    Examples:
      | storeActor                                | providerActor                            |
      | SECONDARY.COM/subscriberUser1             | SECONDARY.COM/publisherUser1             |
      | SECONDARY.COM/subscriberUser1@tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

  # Also covers OpenIDTokenAPITestCase (openid-scoped password-grant token → scope contains openid; userinfo → 200).
  # TENANT-PATH VARIANT: the admin@tenant1.com row does exercise t/<tenant>/oauth2/userinfo — but NOT because the
  # actor's @domain re-routes the request; it does not. The userinfo step resolves the ACTING actor's tenant and
  # builds the tenant-qualified path explicitly (Utils.getUserInfoEndpointURL(baseUrl, tenantDomain), mirroring the
  # introspect helper and the legacy tenant branch). Until that was wired, this comment asserted tenant coverage the
  # suite did not have and BOTH rows hit the super-tenant endpoint — so do not "simplify" the step back to the
  # no-arg overload without deleting this claim as well.
  @cap:key-manager @feat:token-issuance @type:smoke @rule:openid @rule:email-username @legacy:EmailUserNameLoginTestCase @legacy:OpenIDTokenAPITestCase
  Scenario Outline: Generate an OpenID-scoped JWT token and call userinfo as <actor> via <route> with <query>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I store the acting actor credentials as "openidActorName" and "openidActorPassword"
    Then the actual value of "openidActorName" should match standard-mode value "<standardUsername>" and email-mode value "<emailUsername>"
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
    And I extract response field "scope" and store it as "openidTokenScope"
    And The stored value "openidTokenScope" should contain "openid"
    When I invoke the OpenID userinfo endpoint using access token "generatedAccessToken" at route "<route>" with query "<query>"
    Then The response status code should be 200

    Examples: super-tenant primary and secondary-store principals
      | actor                         | route       | query         | standardUsername                                | emailUsername                                                |
      | superTenantAdmin              | unqualified | schema=openid | admin                                           | emailAdmin@email.com@carbon.super                           |
      | superTenantAdmin              | unqualified | none          | admin                                           | emailAdmin@email.com@carbon.super                           |
      | SECONDARY.COM/subscriberUser1 | unqualified | schema=openid | SECONDARY.COM/subscriberUser1@carbon.super     | SECONDARY.COM/subscriberUser1@email.com@carbon.super       |

    Examples: tenant primary and secondary-store principals exercise unqualified and tenant-qualified routes
      | actor                                       | route            | query         | standardUsername                                        | emailUsername                                                |
      | admin@tenant1.com                           | unqualified      | schema=openid | admin@tenant1.com                                       | admin@email.com@tenant1.com                                 |
      | admin@tenant1.com                           | unqualified      | none          | admin@tenant1.com                                       | admin@email.com@tenant1.com                                 |
      | admin@tenant1.com                           | tenant-qualified | schema=openid | admin@tenant1.com                                       | admin@email.com@tenant1.com                                 |
      | admin@tenant1.com                           | tenant-qualified | none          | admin@tenant1.com                                       | admin@email.com@tenant1.com                                 |
      | SECONDARY.COM/subscriberUser1@tenant1.com   | unqualified      | schema=openid | SECONDARY.COM/subscriberUser1@tenant1.com             | SECONDARY.COM/subscriberUser1@email.com@tenant1.com        |
      | SECONDARY.COM/subscriberUser1@tenant1.com   | tenant-qualified | schema=openid | SECONDARY.COM/subscriberUser1@tenant1.com             | SECONDARY.COM/subscriberUser1@email.com@tenant1.com        |

  @cap:key-manager @feat:token-issuance @type:regression @rule:refresh @dep:gateway @legacy:RefreshTokenTestCase @legacy:TokenAPITestCase
  Scenario Outline: Re-issue an access token via refresh token and invoke as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
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
    And The generated access token should be in JWT format
    # The original access token must work before the refresh exchange, as well as the replacement token below.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix               | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/    | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"
    When I request a new OAuth access token using refresh token "refreshToken"
    Then The response status code should be 200
    And The generated access token should be in JWT format
    # The REFRESHED token is the credential under test, so the backend payload is pinned — that is what shows the
    # newly-minted token carried the call through to the upstream rather than merely producing a 200.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "refreshedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "refreshedAccessToken" using these request variants:
      | pathSuffix               | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/    | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    Examples:
      | actor             |
      | superTenantAdmin             |
      | admin@tenant1.com |
      | userKey1          |
      | userKey1@tenant1.com |

  # RefreshTokenTestCase is not registered in legacy TestNG, but its active source covers refresh-token exchange
  # with an OAUTH (opaque-token) application. Keep this separate from the JWT application case above: token type
  # changes the key-manager validation path, so sharing only the same refresh assertion would miss that behavior.
  @cap:key-manager @feat:token-issuance @type:regression @rule:refresh @rule:email-username @dep:gateway @legacy:EmailUserNameLoginTestCase @legacy:RefreshTokenTestCase
  Scenario Outline: Refresh an opaque OAUTH application token and invoke before and after refresh as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I store the acting actor credentials as "refreshActorName" and "refreshActorPassword"
    Then the actual value of "refreshActorName" should match standard-mode value "<standardUsername>" and email-mode value "<emailUsername>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
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
    And The value of response field "throttlingPolicy" should be "Unlimited"
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    # Prove the original opaque token is usable before refresh, not merely that the refresh endpoint accepts its grant.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix               | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/    | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"
    When I request a new OAuth access token using refresh token "refreshToken"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "refreshedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "refreshedAccessToken" using these request variants:
      | pathSuffix               | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/    | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    Examples:
      | actor             | standardUsername | emailUsername                         |
      | superTenantAdmin  | admin            | emailAdmin@email.com@carbon.super    |
      | admin@tenant1.com | admin@tenant1.com | admin@email.com@tenant1.com           |

  # Also provides parity for TokenAPITestCase (sandbox-key token invokes the API → 200; the production-key
  # password-grant and client-credential token invocations of TokenAPITestCase are covered by the smoke
  # JWT-token scenario above + gateway/rest-invocation).
  @cap:key-manager @feat:token-issuance @type:regression @rule:sandbox @rule:production @dep:gateway @legacy:SandboxTokenTestCase @legacy:TokenAPITestCase
  Scenario Outline: Issue a sandbox-scoped token and invoke as <actor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateSandboxApplicationKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateSandboxApplicationKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "jwtSandboxApplicationToken"
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    And The value of response field "throttlingPolicy" should be "Unlimited"
    # Exercise the access token returned directly by SANDBOX key generation, separately from the user password-grant
    # token below. JSON remains covered by the existing JSON paths; the XML variant verifies Accept forwarding through
    # the gateway and the fixture's stable JAX-RS response contract.
    When I invoke the API at gateway context "{{apiContext}}" with access token "jwtSandboxApplicationToken" using these request variants:
      | pathSuffix                | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/     |          | json           | 123 | John |
      | /1.0.0/customers/123/     | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"
    When I request an OAuth access token for the current user using password grant with scope "SANDBOX"
    Then The response status code should be 200
    # The SANDBOX-key token is the credential under test; pin the backend payload so the 200 is evidence the token
    # was honoured all the way to the upstream. (It does not identify WHICH endpoint served it — this API's
    # production and sandbox endpoints are the same URL — that distinction is gateway/rest_endpoint_routing's
    # endpoint-routing scenario, which uses two path-echoing endpoints.)
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix                | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/     | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    # TokenAPITestCase also invokes the token returned directly by PRODUCTION key generation. Keep it distinct from
    # the password-grant PRODUCTION token below; they exercise different application-key/token issuance paths.
    When I put the following JSON payload in context as "generateProductionApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateProductionApplicationKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "jwtProductionApplicationToken"
    When I invoke the API at gateway context "{{apiContext}}" with access token "jwtProductionApplicationToken" using these request variants:
      | pathSuffix                | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/     |          | json           | 123 | John |
      | /1.0.0/customers/123/     | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix                | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/     | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    Examples:
      | actor                                       | providerActor                            |
      | superTenantAdmin                           | superTenantAdmin                         |
      | admin@tenant1.com                           | admin@tenant1.com                         |
      | userKey1                                    | userKey1                                  |
      | userKey1@tenant1.com                        | userKey1@tenant1.com                      |
      | SECONDARY.COM/subscriberUser1                | SECONDARY.COM/publisherUser1              |
      | SECONDARY.COM/subscriberUser1@tenant1.com    | SECONDARY.COM/publisherUser1@tenant1.com  |

  # Ports TokenAPITestCase#testInfiniteTokenAPITestCase — the key-generation validation of the application's
  # `additionalProperties.application_access_token_expiry_time`, which no other scenario exercises (no scenario
  # passed additionalProperties to key generation at all, and error code 900970 appeared nowhere in the corpus).
  # Two halves in ONE scenario because the negative needs the positive control on the SAME application: a
  # NEGATIVE expiry is refused with 400 + the exact 900970 envelope, while Long.MAX_VALUE is accepted and BOTH
  # the sandbox and the production token it issues invoke the gateway successfully. Pinning code AND message AND
  # description matters here: 400 is the management API's catch-all for a dozen unrelated validation faults, so a
  # status-only assertion would pass for the wrong reason.
  @cap:key-manager @feat:token-issuance @rule:token-expiry @type:regression @dep:gateway @legacy:TokenAPITestCase
  Scenario Outline: Application token-expiry additionalProperties are validated at key generation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "infTokenApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "infTokenApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "infTokenApiId"
    Then The lifecycle status of API "infTokenApiId" should be "Published"
    When I retrieve the "apis" resource with id "infTokenApiId"
    And I extract response field "context" and store it as "infTokenApiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "infTokenAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "infTokenAppPayload"
    And I create an application with payload "infTokenAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "infTokenSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "infTokenApiId" using application "createdAppId" with payload "infTokenSubPayload" as "infTokenSubId"
    Then The response status code should be 201

    # A NEGATIVE application-token expiry is refused — exactly 400 with the 900970 envelope.
    When I put the following JSON payload in context as "negativeExpiryKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"], "additionalProperties": {"application_access_token_expiry_time": "-1"}}
    """
    And I generate client credentials for application id "createdAppId" with payload "negativeExpiryKeysPayload"
    Then The response status code should be 400
    And The error response should have code "900970" message "Invalid application additional properties" and description containing "cannot have negative values"

    # Long.MAX_VALUE is accepted for the SANDBOX key, and the token it issues invokes the gateway.
    When I put the following JSON payload in context as "infiniteSandboxKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"], "additionalProperties": {"application_access_token_expiry_time": "9223372036854775807"}}
    """
    And I generate client credentials for application id "createdAppId" with payload "infiniteSandboxKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "infiniteSandboxToken"
    When I invoke the API at gateway context "{{infTokenApiContext}}/1.0.0/customers/123/" with method "GET" using access token "infiniteSandboxToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # ...and likewise for the PRODUCTION key — legacy asserts both key types independently.
    When I put the following JSON payload in context as "infiniteProductionKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"], "additionalProperties": {"application_access_token_expiry_time": "9223372036854775807"}}
    """
    And I generate client credentials for application id "createdAppId" with payload "infiniteProductionKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "infiniteProductionToken"
    When I invoke the API at gateway context "{{infTokenApiContext}}/1.0.0/customers/123/" with method "GET" using access token "infiniteProductionToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | superTenantAdmin             |
      | admin@tenant1.com |
      | userKey1          |
      | userKey1@tenant1.com |

  # Ports the scope-FILTERING half of APIScopeTestCase#testSetScopeToResourceTestCase plus #testRESTAPIScopes.
  # Requesting the SAME full apim:* management scope set on ONE keyed application, as three principals of
  # different roles, proves the key manager grants only the scopes each user's roles map to (the tenant-conf
  # RESTAPIScopes role bindings). The two REST legs are the management-plane consequence legacy pins and v2
  # missed: a SUBSCRIBER token cannot list publisher APIs, and a CREATOR token cannot list devportal
  # applications — each with its positive control in the same scenario, so a blanket 401 cannot pass it.
  # Scope membership is compared WHOLE-ENTRY against the response's space-delimited list, not by substring
  # (see "The issued token scope list should include ... and exclude ...").
  @cap:key-manager @feat:scope-issuance @rule:role-filtering @type:regression @dep:publisher @dep:devportal @legacy:APIScopeTestCase
  Scenario Outline: Management scopes granted in a token are filtered by the <store> user's roles in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    # The block seeds primary and SECONDARY.COM subscriber / creator+publisher actors. Provision a creator-only
    # actor in the same store, so each store row checks that apim:api_publish requires PUBLISHER as well as creator.
    And I provision access-control user "<storePrefix>roleScopeCreator" with roles "Internal/creator" in tenant "<tenant>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "roleScopeAppPayload"
    And I create an application with payload "roleScopeAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "roleScopeKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "roleScopeKeysPayload"
    Then The response status code should be 200

    # A SUBSCRIBER receives the consumer scopes and none of the publisher-plane ones.
    When I request an OAuth access token using password grant as "<subscriberActor>" with scope "openid apim:api_view apim:api_create apim:api_publish apim:subscribe apim:app_update"
    Then The response status code should be 200
    And The issued token scope list should include "openid,apim:subscribe,apim:app_update" and exclude "apim:api_view,apim:api_create,apim:api_publish"

    # A CREATOR receives apim:api_create but NOT apim:api_publish, and no consumer scopes.
    When I request an OAuth access token using password grant as "<storePrefix>roleScopeCreator<suffix>" with scope "openid apim:api_view apim:api_create apim:api_publish apim:subscribe apim:app_update"
    Then The response status code should be 200
    And The issued token scope list should include "openid,apim:api_view,apim:api_create" and exclude "apim:api_publish,apim:subscribe,apim:app_update"

    # A CREATOR+PUBLISHER receives both publisher-plane scopes, still no consumer scopes.
    When I request an OAuth access token using password grant as "<publisherActor>" with scope "openid apim:api_view apim:api_create apim:api_publish apim:subscribe apim:app_update"
    Then The response status code should be 200
    And The issued token scope list should include "openid,apim:api_view,apim:api_create,apim:api_publish" and exclude "apim:subscribe,apim:app_update"

    # Publisher plane: a subscriber's token is unauthenticated for the API listing (401 — the management API's
    # answer for a token lacking the required scope, NOT 403); the creator+publisher's token is the control.
    Given The system is ready and I have valid publisher access tokens as "<subscriberActor>"
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 401
    Given The system is ready and I have valid publisher access tokens as "<publisherActor>"
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200

    # Devportal plane, symmetrically: a creator's token cannot list applications; a subscriber's can.
    Given The system is ready and I have valid devportal access token as "<storePrefix>roleScopeCreator<suffix>"
    When I retrieve all applications from the Developer Portal
    Then The response status code should be 401
    Given The system is ready and I have valid devportal access token as "<subscriberActor>"
    When I retrieve all applications from the Developer Portal
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       | storePrefix  | store           | adminActor        | subscriberActor                           | publisherActor                           |
      | carbon.super |              |              | primary         | superTenantAdmin  | subscriberUser                            | publisherUser                            |
      | carbon.super |              | SECONDARY.COM/ | SECONDARY.COM   | superTenantAdmin  | SECONDARY.COM/subscriberUser1             | SECONDARY.COM/publisherUser1             |
      | tenant1.com  | @tenant1.com |              | primary         | admin@tenant1.com | subscriberUser@tenant1.com                | publisherUser@tenant1.com                |
      | tenant1.com  | @tenant1.com | SECONDARY.COM/ | SECONDARY.COM   | admin@tenant1.com | SECONDARY.COM/subscriberUser1@tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

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
      | superTenantAdmin             |
      | admin@tenant1.com |

  # Ports GrantTypeTokenGenerateTestCase#testAuthCode's UNCOVERED legs: the authorize redirect's session cookies,
  # the consent page the product actually renders (testAuthCodeAppDisplayName), and using the authorization_code
  # token to invoke the API at the GATEWAY — the scenario above only proves the token was issued.
  #
  # The display name asserted is the SERVICE PROVIDER name the consent page renders in its <h4>, which the current
  # product builds as <owner>_<applicationUUID>_<keyType>. Legacy asserted the devportal application NAME appeared
  # there; that is no longer reachable — APIM names the OAuth service provider by application UUID, and it sets the
  # SP's display name to that same string (AMDefaultKeyManagerImpl passes OAuthApplicationInfo#getClientName into
  # ClientInfo#setApplicationDisplayName), so the [oauth] show_display_name_in_consent_page toggle legacy enabled
  # cannot surface a different value. Asserting the UUID form is therefore the exact, still-meaningful form of the
  # same check: the consent page names THIS application and no other. The expected SP owner prefix is explicit
  # per organization so both naming paths are pinned.
  @cap:key-manager @feat:token-issuance @rule:authcode-consent @type:regression @dep:gateway @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: The authorization code flow sets session cookies, names the application on the consent page, and yields a token that invokes the API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "authCodeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "authCodeKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I request an OAuth access token via authorization code grant with scope "PRODUCTION"
    Then The response status code should be 200
    And The generated access token should be in JWT format
    # The authorize redirect must establish the per-authorization-request session nonce cookie. Legacy asserted
    # only that SOME Set-Cookie header came back; this names the cookie the OAuth flow actually depends on.
    # Pinned live: this response carries that cookie alone — it does NOT set a JSESSIONID.
    And The stored value "authorizeSetCookies" should contain "sessionNonceCookie-"
    # The consent page the user would have been shown names THIS application.
    And The stored value "consentApplicationName" should be "{{spOwnerName}}_{{createdAppId}}_PRODUCTION"
    And The stored value "consentPageBody" should contain "{{spOwnerName}}_{{createdAppId}}_PRODUCTION"
    # The authorization_code token is a real gateway credential, not just a well-formed JWT.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | superTenantAdmin             |
      | admin@tenant1.com |

  # Ports GrantTypeTokenGenerateTestCase#testImplicit — v2 had NO implicit-grant coverage at all. The implicit
  # grant returns its token in the redirect FRAGMENT with no token-endpoint exchange, so the token itself is the
  # only evidence: it must be a JWT and it must work at the gateway.
  @cap:key-manager @feat:token-issuance @rule:implicit @type:regression @dep:gateway @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: Generate an access token via the implicit grant as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, polling can never recover, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "implicitKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "implicit"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "implicitKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I request an OAuth access token via implicit grant with scope "PRODUCTION"
    Then The generated access token should be in JWT format
    And the actual value of "implicitTokenType" should match the expected value:
      """
      Bearer
      """
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | superTenantAdmin             |
      | admin@tenant1.com |

  # Ports GrantTypeTokenGenerateTestCase#testAuthRequestWithoutCallbackURL. Legacy asserted only that the error
  # redirect CONTAINED the string "oauthErrorCode" — which passes for any OAuth error whatsoever. The product
  # distinguishes two cases here and both are pinned exactly: a MISSING redirect_uri is a protocol-level
  # invalid_request, while a PRESENT-but-unregistered one is invalid_callback.
  @cap:key-manager @feat:token-issuance @rule:redirect-uri @type:negative @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: An authorization request with a <case> redirect URI is refused with <errorCode> as <actor>
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

    When I send an OAuth authorization request with redirect uri "<redirectUri>"
    Then the actual value of "authorizeErrorCode" should match the expected value:
      """
      <errorCode>
      """
    And the actual value of "authorizeErrorMessage" should match the expected value:
      """
      <errorMessage>
      """

    Examples:
      | actor             | case         | redirectUri                        | errorCode        | errorMessage                                          |
      | superTenantAdmin             | missing      |                                    | invalid_request  | Redirect URI is not present in the authorization request |
      | superTenantAdmin             | unregistered | http://localhost:8490/not-my-callback | invalid_callback | callback.not.match                                    |
      | admin@tenant1.com | missing      |                                    | invalid_request  | Redirect URI is not present in the authorization request |
      | admin@tenant1.com | unregistered | http://localhost:8490/not-my-callback | invalid_callback | callback.not.match                                    |

  # Ports GrantTypeTokenGenerateTestCase#testTokenGenerationWithCorruptedClientCredentials. Nothing in v2 asserted
  # the invalid_client error body or the WWW-Authenticate challenge anywhere; both are pinned here. The realm the
  # server advertises is its own hostname, which differs per deployment, so the challenge is asserted on the
  # scheme-and-realm prefix that is invariant — not on the host.
  @cap:key-manager @feat:token-issuance @rule:invalid-client @type:negative @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: A corrupted client credential is refused at the token endpoint as <actor>
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

    When I request an OAuth access token using corrupted client credentials
    Then The response status code should be 401
    And The response should contain "invalid_client"
    And The response should contain "Client credentials are invalid."
    And The response header "WWW-Authenticate" should contain "Basic realm="

    Examples:
      | actor             |
      | superTenantAdmin             |
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
      | superTenantAdmin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:token-issuance @rule:scope-in-token @type:regression @legacy:TokenEncryptionScopeTestCase
  Scenario Outline: A requested <scopeType> scope is granted in the issued token as <actor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    When I create a new shared scope as "encTokenScope"
    Then The response status code should be 201
    And I extract response field "name" and store it as "encTokenScopeName"
    When I retrieve the shared scope with id "scopeID"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list "admin"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "encApiCreate"
    And I create an "apis" resource with payload "encApiCreate" as "encApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"{{encTokenScopeName}}","displayName":"{{encTokenScopeName}}","description":"admin-bound shared scope","bindings":["admin"]}},{"shared":false,"scope":{"name":"{{encTokenScopeName}}_local","displayName":"{{encTokenScopeName}}_local","description":"subscriber-bound API-local scope","bindings":["Internal/subscriber"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "encApiId"
    And I put the response payload in context as "encApiPayload"
    When I update the "apis" resource "encApiId" and "encApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{encTokenScopeName}}","{{encTokenScopeName}}_local"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "encApiId"
    Then The response array field "scopes" should have exactly 2 entries
    And The response field "scopes[?(@.scope.name=='{{encTokenScopeName}}_local')].scope.bindings[*]" should be exactly the list "Internal/subscriber"
    And The response field "operations[0].scopes[*]" should be exactly the list "{{encTokenScopeName}},{{encTokenScopeName}}_local"
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
    Given The system is ready and I have valid devportal access token as "<actor>"
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
    When I request an OAuth access token for the current user using password grant with scope "<requestedScope>"
    Then The response status code should be 200
    And I extract response field "scope" and store it as "encTokenGrantedScope"
    And The stored value "encTokenGrantedScope" should be "<requestedScope>"

    Examples:
      | providerActor     | actor                    | scopeType | requestedScope                 |
      | superTenantAdmin  | superTenantAdmin         | shared    | {{encTokenScopeName}}           |
      | admin@tenant1.com | admin@tenant1.com        | shared    | {{encTokenScopeName}}           |
      | superTenantAdmin  | subscriberUser           | local     | {{encTokenScopeName}}_local     |
      | admin@tenant1.com | subscriberUser@tenant1.com | local   | {{encTokenScopeName}}_local     |

  # An UNREGISTERED scope is granted verbatim rather than dropped or replaced by "default". Pinned because ~20
  # scenarios across the gateway and key-manager suites request "PRODUCTION"/"SANDBOX" — neither of which is a
  # registered scope — and depend on getting a usable token back. If scope validation ever tightened, this names
  # the cause instead of leaving those to fail as downstream 401/403s. Completes the granted-scope trio alongside
  # authcode-default-scope (none requested -> "default") and scope-in-token (a registered scope).
  @cap:key-manager @feat:token-issuance @rule:unregistered-scope @type:regression
  Scenario Outline: A scope not registered on the application is still granted in the issued token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "unregScopeAppPayload"
    And I create an application with payload "unregScopeAppPayload"
    Then The response status code should be 201
    # No "scopes" key in this payload: the application registers NO scopes at all.
    When I put the following JSON payload in context as "unregScopeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "unregScopeKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    # Exact, not containment: the granted scope must be exactly what was requested — no silent substitution to
    # "default" and nothing extra appended.
    And I extract response field "scope" and store it as "unregTokenScope"
    And the actual value of "unregTokenScope" should match the expected value:
      """
      PRODUCTION
      """

    Examples:
      | actor             |
      | superTenantAdmin             |
      | admin@tenant1.com |

  # Ports OpenIDTokenAPITestCase's ACTUAL arc: legacy created its application with tokenType OAUTH (opaque) and
  # presented THAT token at userinfo. The openid scenario above uses the JWT-default app, so the opaque-token-at-
  # userinfo path was untested for every principal (admin included). Discriminator: the fixture's tokenType OAUTH.
  # There is no existing step to assert "not in JWT format", so that assertion is intentionally omitted (see report).
  @cap:key-manager @feat:token-issuance @type:regression @rule:openid @legacy:OpenIDTokenAPITestCase
  Scenario Outline: Call userinfo with an opaque OpenID-scoped token as <actor> via <route> with <query>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
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
    And I extract response field "scope" and store it as "opaqueOpenidTokenScope"
    And The stored value "opaqueOpenidTokenScope" should contain "openid"
    When I invoke the OpenID userinfo endpoint using access token "generatedAccessToken" at route "<route>" with query "<query>"
    Then The response status code should be 200

    Examples: super-tenant primary and secondary-store principals
      | actor                         | route       | query         |
      | superTenantAdmin              | unqualified | schema=openid |
      | superTenantAdmin              | unqualified | none          |
      | SECONDARY.COM/subscriberUser1 | unqualified | schema=openid |

    Examples: tenant primary and secondary-store principals exercise both documented paths
      | actor                                       | route            | query         |
      | admin@tenant1.com                           | unqualified      | schema=openid |
      | admin@tenant1.com                           | tenant-qualified | schema=openid |
      | SECONDARY.COM/subscriberUser1@tenant1.com   | unqualified      | schema=openid |
      | SECONDARY.COM/subscriberUser1@tenant1.com   | tenant-qualified | schema=openid |

  # Ports TokenAPITestCase's OAUTH-tokenType arc: the legacy test exercises both a sandbox-key token and a
  # PRODUCTION-key password-grant token against the same API. Keep both invocations on the opaque OAUTH app.
  @cap:key-manager @feat:token-issuance @type:regression @rule:sandbox @dep:gateway @legacy:TokenAPITestCase
  Scenario Outline: Issue a sandbox-scoped token on an opaque application and invoke as <actor>
    Given The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And the "apis" resource "createdApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateSandboxApplicationKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateSandboxApplicationKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "oauthSandboxApplicationToken"
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    And The value of response field "throttlingPolicy" should be "Unlimited"
    # Legacy sends the SANDBOX access token returned by key generation directly, not a SANDBOX password-grant token.
    # Keep both credentials tested: they exercise different issuance paths against this opaque OAUTH application.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "oauthSandboxApplicationToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "oauthSandboxApplicationToken" using these request variants:
      | pathSuffix                | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/     | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"
    When I request an OAuth access token for the current user using password grant with scope "SANDBOX"
    Then The response status code should be 200
    # The SANDBOX-key token is the credential under test; pin the backend payload so the 200 is evidence the token
    # was honoured all the way to the upstream (same body string the JWT sandbox scenario above asserts).
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # Legacy also generated a PRODUCTION key on this OAUTH application and invoked with its password-grant token.
    When I put the following JSON payload in context as "generateProductionApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateProductionApplicationKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix                | accept   | responseFormat | id  | name |
      | /1.0.0/customers/123/     | text/xml | xml            | 123 | John |
    Then The response status code should be 200
    And The response header "Content-Type" should contain "text/xml"
    And The response should contain "<Customer>"

    Examples:
      | actor                                       | providerActor                            |
      | superTenantAdmin                           | superTenantAdmin                         |
      | admin@tenant1.com                           | admin@tenant1.com                         |
      | userKey1                                    | userKey1                                  |
      | userKey1@tenant1.com                        | userKey1@tenant1.com                      |
      | SECONDARY.COM/subscriberUser1                | SECONDARY.COM/publisherUser1              |
      | SECONDARY.COM/subscriberUser1@tenant1.com    | SECONDARY.COM/publisherUser1@tenant1.com  |
