@cleanup
Feature: Backend JWT Claims

  Key-manager plane: with backend JWT generation enabled ([apim.jwt] enable), the gateway injects an
  X-JWT-Assertion header carrying the application, API and subscriber claims towards the backend. The API
  routes to the header-reflecting backend (/reflect-headers) so the decoded assertion can be inspected. Runs
  in the backend-JWT-enabled block (shared with application attributes, whose overlay turns on backend JWT and
  declares a required application attribute — so applications here supply it). Ports JWTTestCase (default
  app/API/subscriber claims). The dotted-username and user-profile-claim cases are ported separately.

  @cap:key-manager @feat:backend-jwt @rule:backend-jwt-claims @type:regression @dep:gateway @legacy:JWTTestCase
  Scenario Outline: The backend JWT carries the application, API and subscriber claims as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "jwtApiId" and deployed it
    When I publish the "apis" resource with id "jwtApiId"
    Then The lifecycle status of API "jwtApiId" should be "Published"
    When I retrieve the "apis" resource with id "jwtApiId"
    And I extract response field "context" and store it as "jwtApiContext"
    And I extract response field "name" and store it as "jwtApiName"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "jwtAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "jwtAppPayload"
    And I create an application with payload "jwtAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    And I extract response field "name" and store it as "jwtAppName"

    When I put the following JSON payload in context as "jwtKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "jwtKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "jwtSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "jwtApiId" using application "createdAppId" with payload "jwtSub" as "jwtSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "jwtToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "jwtToken"
    Then The response status code should be 200

    # Invoke; the reflecting backend returns the headers it received, including the gateway-injected
    # X-JWT-Assertion. Decode it and assert the standard backend-JWT claims (substring match, so the short
    # claim suffix suffices for the dialect-prefixed names). subscriber "admin" is a substring of both the
    # super-tenant "admin" and the tenant "admin@tenant1.com" subscriber values.
    When I invoke the API at gateway context "{{jwtApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "applicationname" with value "{{jwtAppName}}"
    And The reflected backend JWT should contain claim "apiname" with value "{{jwtApiName}}"
    And The reflected backend JWT should contain claim "version" with value "1.0.0"
    And The reflected backend JWT should contain claim "subscriber" with value "admin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # applicationAttributes empty-value flag: with enable_empty_values_in_application_attributes = true (set in
  # this block's overlay), an OPTIONAL application attribute left with an EMPTY value still appears in the
  # backend JWT's applicationAttributes claim (as ""). Ports the empty-value assertion of JWTTestCase (which
  # asserts the claim carries "Optional attribute":""). ×2 tenant — the block provisions tenant1 actors and
  # the flag is tenant-agnostic. Only the TRUE side is covered: the FALSE side (empty optional attribute
  # ABSENT with the flag defaulted off) would require a separate default-config overlay/block, which §13
  # forbids adding solely for the false side — deferred as a follow-up (documented in the overlay).
  @cap:key-manager @feat:backend-jwt @rule:app-attributes-empty-value @type:regression @dep:gateway @legacy:JWTTestCase
  Scenario Outline: An empty optional application attribute surfaces in the backend JWT claim as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "eApiId" and deployed it
    When I publish the "apis" resource with id "eApiId"
    Then The lifecycle status of API "eApiId" should be "Published"
    When I retrieve the "apis" resource with id "eApiId"
    And I extract response field "context" and store it as "eApiContext"

    # The application supplies the required attribute and leaves the optional attribute EMPTY.
    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_empty_attribute.json" in context as "eAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "eAppPayload"
    And I create an application with payload "eAppPayload"
    Then The response status code should be 201

    When I put the following JSON payload in context as "eKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "eKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "eSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "eApiId" using application "createdAppId" with payload "eSub" as "eSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "eToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "eToken"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{eApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # enable_empty_values_in_application_attributes=true => the empty optional attribute is present as "".
    And The reflected backend JWT applicationAttributes claim should contain "Optional attribute" with an empty value
    # The required attribute is still carried with its non-empty value.
    And The reflected backend JWT should contain application attribute "External Reference Id" with value "c1237890"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Backend-JWT generation for a resource owner whose username matches the [string].[string] pattern (contains a
  # dot). This previously broke JWT claim decoding; the fix is guarded by invoking TWICE with a password-grant
  # token whose subject is the dotted user (the repeat call failed before the fix). Ported ×2 tenant — the dot is
  # tenant-agnostic, so it is exercised in both the super tenant and tenant1.com. Ports JWTDecodingTestCase.
  @cap:key-manager @feat:backend-jwt @rule:dotted-username @type:regression @dep:gateway @legacy:JWTDecodingTestCase
  Scenario Outline: The backend JWT is generated for a dotted-username ([string].[string]) resource owner in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "jwtdecode.user" with roles "Internal/subscriber" in tenant "<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "dApiId" and deployed it
    When I publish the "apis" resource with id "dApiId"
    Then The lifecycle status of API "dApiId" should be "Published"
    When I retrieve the "apis" resource with id "dApiId"
    And I extract response field "context" and store it as "dApiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "dAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "dAppPayload"
    And I create an application with payload "dAppPayload"
    Then The response status code should be 201
    # The app supports the password grant so a token can be minted for the dotted resource owner.
    When I put the following JSON payload in context as "dKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "dKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "dSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "dApiId" using application "createdAppId" with payload "dSub" as "dSubId"
    Then The response status code should be 201

    # Mint a password-grant token whose resource owner is the dotted user, then invoke twice.
    When I act as "jwtdecode.user<suffix>"
    And I request an OAuth access token for the current user using password grant with scope ""
    Then The response status code should be 200
    When I invoke the API at gateway context "{{dApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # (The gateway masks the JWT subject/enduser to a pseudonymous UUID in 4.7.0, so the username is not asserted
    #  — a clean 200 proves backend-JWT generation succeeded for the dotted-username owner, matching the legacy
    #  which asserts only the status code.)
    # Repeat — the dotted-username decoding failure surfaced on the SECOND call before the fix.
    When I invoke the API at gateway context "{{dApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # User-profile claims in the backend JWT: a resource owner's profile claims (givenname / lastname / mobile /
  # organization), set on the user via setUserClaimValue, surface in the gateway-injected X-JWT-Assertion. A
  # password-grant token (openid scope) is minted for that user so the gateway generates the backend JWT with
  # their profile. Attempted ×2 tenant — the claim/scope/SP SOAP operations are tenant-scoped. Ports the
  # user-profile-claims part of JWTTestCase.
  @cap:key-manager @feat:backend-jwt @rule:user-profile-claims @type:regression @dep:gateway @legacy:JWTTestCase
  Scenario Outline: The backend JWT carries the resource owner's profile claims in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I register the OIDC user-profile claim mappings and scope in tenant "<tenant>"
    And I provision user "jwtclaims.user" with roles "Internal/subscriber" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/givenname" to "ProfileFirstName" for user "jwtclaims.user" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/lastname" to "ProfileLastName" for user "jwtclaims.user" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/mobile" to "94123456987" for user "jwtclaims.user" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/organization" to "ProfileOrgABC" for user "jwtclaims.user" in tenant "<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "upApiId" and deployed it
    When I publish the "apis" resource with id "upApiId"
    Then The lifecycle status of API "upApiId" should be "Published"
    When I retrieve the "apis" resource with id "upApiId"
    And I extract response field "context" and store it as "upApiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "upAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "upAppPayload"
    And I create an application with payload "upAppPayload"
    Then The response status code should be 201
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
    When I act as "jwtclaims.user<suffix>"
    And I request an OAuth access token for the current user using password grant with scope "openid profile"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{upApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "givenname" with value "ProfileFirstName"
    And The reflected backend JWT should contain claim "lastname" with value "ProfileLastName"
    And The reflected backend JWT should contain claim "mobile" with value "94123456987"
    And The reflected backend JWT should contain claim "organization" with value "ProfileOrgABC"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
