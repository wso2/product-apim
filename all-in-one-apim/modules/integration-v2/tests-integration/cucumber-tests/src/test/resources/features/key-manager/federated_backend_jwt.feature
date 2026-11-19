@cleanup
Feature: Backend JWT resource-owner claims (FederatedUserJWTTestCase)

  Ports both tests of the legacy FederatedUserJWTTestCase, which checks the resource owner's claims in the
  gateway-generated backend JWT. The authorization-code scenario federates the login to an external OIDC Identity
  Server: APIM redirects the OAuth client to a real block-scoped WSO2 IS container, the user authenticates there,
  APIM handles the federated callback, and the exchanged token invokes an API whose backend reflects the JWT. The
  password-grant scenario uses local APIM users whose profile claims are set on APIM.

  Both scenarios need backend JWT generation and the declared application attributes; they send OAuth tokens in
  the standard Authorization header.

  @cap:key-manager @feat:backend-jwt @rule:federated-authorization-code @type:regression @dep:gateway @legacy:FederatedUserJWTTestCase
  Scenario Outline: Federated OIDC authorization-code claims reach the backend JWT in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I set the OAuth callback URI for this topology
    And I register the OIDC user-profile claim mappings and scope in tenant "<tenant>"
    And I register an external OIDC IdP for backend JWT in tenant "<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "fedJwtApiId" and deployed it
    When I publish the "apis" resource with id "fedJwtApiId"
    Then The lifecycle status of API "fedJwtApiId" should be "Published"
    When I retrieve the "apis" resource with id "fedJwtApiId"
    And I extract response field "context" and store it as "fedJwtApiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "fedJwtAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "fedJwtAppPayload"
    And I create an application with payload "fedJwtAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    And I extract response field "name" and store it as "fedJwtAppName"
    When I put the following JSON payload in context as "fedJwtKeys"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["authorization_code"],"callbackUrl":"{{fedJwtCallback}}"}
    """
    And I generate client credentials for application id "createdAppId" with payload "fedJwtKeys"
    Then The response status code should be 200
    When I configure OAuth consumer key "consumerKey" for federated OIDC authentication in tenant "<tenant>"
    And I put the following JSON payload in context as "fedJwtSub"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Gold"}
    """
    And I subscribe to API "fedJwtApiId" using application "createdAppId" with payload "fedJwtSub" as "fedJwtSubId"
    Then The response status code should be 201

    When I obtain an OAuth authorization-code token through the external OIDC IdP
    Then The response status code should be 200
    When I invoke the API at gateway context "{{fedJwtApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Pin APIM's default backend-JWT claims on the federated flow too. The separate client-credentials JWT scenario
    # exercises these on another grant, while the legacy federated test checks them on this authorization-code path.
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "subscriber" with value "<adminActor>"
    And The reflected backend JWT should contain claim "applicationname" with value "{{fedJwtAppName}}"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "givenname" with value "{{fedJwtUser}}"
    And The reflected backend JWT should contain claim "lastname" with value "SSO"
    And The reflected backend JWT should contain claim "mobile" with value "94123456987"
    And The reflected backend JWT should contain claim "telephone" with value "424479772294778"
    And The reflected backend JWT should contain claim "organization" with value "FederatedOrg"
    And The reflected backend JWT should contain claim "emailaddress" with value "{{fedJwtEmail}}"
    And The reflected backend JWT header should contain "typ" with value "JWT"
    And The reflected backend JWT should be signed with algorithm "RS256"
    And The reflected backend JWT should be verifiably signed by the key the gateway JWKS endpoint publishes
    And The reflected backend JWT should not contain claim "wrongclaim"

    Examples:
      | tenant       | suffix       | adminActor        |
      | carbon.super |              | admin             |
      | tenant1.com  | @tenant1.com | admin@tenant1.com |

  # User-profile claims in the backend JWT: a resource owner's profile claims (givenname / lastname / mobile /
  # organization), set on the user via setUserClaimValue, surface in the gateway-injected X-JWT-Assertion. Keep
  # the two email-shaped usernames from FederatedUserJWTTestCase as distinct rows in each tenant; these are
  # usernames containing @, not emailUserMode aliases. Actor keys deliberately differ from the stored usernames
  # because Identity actor references use @tenant as their final delimiter.
  @cap:key-manager @feat:backend-jwt @rule:user-profile-claims @type:regression @dep:gateway @legacy:JWTTestCase @legacy:FederatedUserJWTTestCase
  Scenario Outline: The backend JWT carries the resource owner's profile claims for <username> in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I register the OIDC user-profile claim mappings and scope in tenant "<tenant>"
    And I provision user "<username>" as actor "<actorKey>" with roles "Internal/subscriber" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/givenname" to "first name <username>" for user "<username>" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/lastname" to "last name <username>" for user "<username>" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/mobile" to "94123456987" for user "<username>" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/organization" to "ABC <username>" for user "<username>" in tenant "<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "upApiId" and deployed it
    When I publish the "apis" resource with id "upApiId"
    Then The lifecycle status of API "upApiId" should be "Published"
    When I retrieve the "apis" resource with id "upApiId"
    And I extract response field "context" and store it as "upApiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_empty_attribute.json" in context as "upAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "upAppPayload"
    And I create an application with payload "upAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    And I extract response field "name" and store it as "upAppName"
    When I put the following JSON payload in context as "upKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "upKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "upSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "upApiId" using application "createdAppId" with payload "upSub" as "upSubId"
    Then The response status code should be 201

    # Configure the app's OAuth service provider to REQUEST the user-profile claims (the backend JWT only surfaces
    # claims the SP requests). Must happen before the token is minted.
    When I configure the service provider for consumer key "consumerKey" to request the user-profile claims in tenant "<tenant>"

    # Mint a password-grant token (openid scope) whose resource owner is the claim-populated user, then invoke.
    When I act as "<actorKey><suffix>"
    And I request an OAuth access token for the current user using password grant with scope "openid profile"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{upApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "subscriber" with value "admin<suffix>"
    And The reflected backend JWT should contain claim "applicationname" with value "{{upAppName}}"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT header should contain "typ" with value "JWT"
    And The reflected backend JWT should be signed with algorithm "RS256"
    And The reflected backend JWT should be verifiably signed by the key the gateway JWKS endpoint publishes
    And The reflected backend JWT should contain claim "givenname" with value "first name <username>"
    And The reflected backend JWT should contain claim "lastname" with value "last name <username>"
    And The reflected backend JWT should contain claim "mobile" with value "94123456987"
    And The reflected backend JWT should contain claim "organization" with value "ABC <username>"
    # Assert the whole map so missing defaults and unexpected extra attributes fail on this password-grant JWT.
    And The reflected backend JWT applicationAttributes claim should exactly equal
    """
    {"External Reference Id":"c1237890","Optional attribute":"","Sandbox access required":"Yes","Production access required":"Yes"}
    """
    And The reflected backend JWT should not contain claim "wrongclaim"

    Examples:
      | tenant       | suffix       | username                     | actorKey       |
      | carbon.super |              | subscriberUser2               | jwtClaimBare   |
      | carbon.super |              | subscriberUser2@wso2.com      | jwtClaimWso2   |
      | carbon.super |              | subscriberUser2@abc.com       | jwtClaimAbc    |
      | tenant1.com  | @tenant1.com | subscriberUser2               | jwtClaimBare   |
      | tenant1.com  | @tenant1.com | subscriberUser2@wso2.com      | jwtClaimWso2   |
      | tenant1.com  | @tenant1.com | subscriberUser2@abc.com       | jwtClaimAbc    |
