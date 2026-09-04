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
    # X-JWT-Assertion. Decode it and assert the standard backend-JWT claims. A short claim suffix is resolved to
    # its dialect-qualified key (http://wso2.org/claims/<suffix>) and compared exactly, so each Examples row
    # asserts ITS OWN subscriber via <actor> rather than a value that is merely a substring of both.
    When I invoke the API at gateway context "{{jwtApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "applicationname" with value "{{jwtAppName}}"
    And The reflected backend JWT should contain claim "apiname" with value "{{jwtApiName}}"
    And The reflected backend JWT should contain claim "version" with value "1.0.0"
    And The reflected backend JWT should contain claim "subscriber" with value "<actor>"
    # iss and applicationtier are gateway-generator invariants: iss is fixed to the AM product URI and
    # applicationtier reflects the app's Unlimited tier. Pinned exactly so a generator change that drops or
    # renames either (leaving a backend JWT the backend can no longer attribute to the issuer/tier) fails here.
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    # typ in the JOSE header declares the assertion is a JWT; every claim assertion above reads the payload only,
    # so a header that stops declaring its type is invisible to them without this check.
    And The reflected backend JWT header should contain "typ" with value "JWT"
    # The assertion is SIGNED, not merely present: [apim.jwt] signing_algorithm defaults to SHA256withRSA, and
    # setting it to NONE disables signing. Every claim assertion above reads the payload, which is identical
    # either way, so without this the gateway could stop signing and the suite would stay green.
    And The reflected backend JWT should be signed with algorithm "RS256"
    # kid names the key the backend selects to verify the signature; a blank/absent kid leaves the backend unable
    # to pick the verification key. The value is base64(DN+serial) of the shipped wso2carbon signing certificate
    # (decodes to "CN=localhost, OU=WSO2, O=WSO2, L=Mountain View, ST=CA, C=US#<serial>"), so it is deterministic
    # for this distribution and identical across tenants — a mismatch means the backend-JWT signing key changed.
    And The reflected backend JWT header should contain "kid" with value "Q049bG9jYWxob3N0LCBPVT1XU08yLCBPPVdTTzIsIEw9TW91bnRhaW4gVmlldywgU1Q9Q0EsIEM9VVMjMjExMjc5NDc5Njg2NTExMjgzMzcxNzY2ODEwMjc1MjAyMjU0ODQ5MzE4NzgwMzI3"
    # The signature is VERIFIED, not merely declared. Every assertion above reads header/payload text, so an
    # assertion with alg=RS256, the right kid and a garbage signature passes them all. This fetches the gateway's
    # own JWKS at runtime (no key material in the repo) and checks BOTH that the JWT's kid is exactly the key
    # JWKS publishes and that the signature verifies against it. Ports BackendJWTUtil.verifySignature +
    # the JWKS-kid comparison of BackendJWTUtil.verifyJWTHeader.
    And The reflected backend JWT should be verifiably signed by the key the gateway JWKS endpoint publishes
    # A claim the app/token never configured must NOT appear: on the client-credentials path there is no resource
    # owner, so the user-profile claim "mobile" is absent. Without this, a generator that leaked every available
    # claim into the backend JWT would still pass every positive assertion above.
    And The reflected backend JWT should not contain claim "mobile"
    # activityid CORRELATION. The gateway mints a correlation id per call, sends it to the backend as the
    # "activityid" request header and returns THE SAME value on the response — so a backend log line and the
    # client's response can be tied to one transaction. Legacy asserts this by having its in-gateway backend copy
    # the incoming activityid into an "in_activityid" response header and comparing the two; the reflecting
    # backend echoes its received request headers instead, so the request-path value is read from there and
    # compared EXACTLY with the response-path header. Nothing else in v2 exercises correlation-id propagation,
    # and no config is needed — the ids are minted unconditionally (observed on a live 4.7.0 container).
    And I extract response field "headers.activityid" and store it as "jwtBackendActivityId"
    And The response should contain the header "activityid" with value "{{jwtBackendActivityId}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Same backend-JWT claim family reached via an AUTHORIZATION_CODE token instead of a direct client-credentials
  # grant. token_issuance.feature already proves an authcode token is ISSUED and invokes the gateway; what was
  # missing is the backend-JWT INSPECTION on this path — the claim-assembly leg where the token's resource owner
  # arrives through the interactive authorize/consent flow rather than a direct grant. Reuses the C11a header and
  # generator-invariant assertions (typ/alg/kid, iss, applicationtier). The app/API/subscription claims are
  # grant-invariant so they are pinned too; enduser/sub are the resource-owner-path discriminators.
  @cap:key-manager @feat:backend-jwt @rule:authcode-credential @type:regression @dep:gateway @legacy:JWTTestCase
  Scenario Outline: The backend JWT is generated for an authorization-code token invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "acApiId" and deployed it
    And the "apis" resource "acApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "acApiId"
    Then The lifecycle status of API "acApiId" should be "Published"
    When I retrieve the "apis" resource with id "acApiId"
    And I extract response field "context" and store it as "acApiContext"
    And I extract response field "name" and store it as "acApiName"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "acAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "acAppPayload"
    And I create an application with payload "acAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    And I extract response field "name" and store it as "acAppName"

    # Keys support authorization_code (with the registered callback + scopes token_issuance.feature already uses).
    When I put the following JSON payload in context as "acKeys"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code"], "callbackUrl": "http://localhost:8490/callback", "scopes": ["openid", "am_application_scope", "default"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "acKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "acSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "acApiId" using application "createdAppId" with payload "acSub" as "acSubId"
    Then The response status code should be 201

    # The token's resource owner is the acting actor, established through the authorize/consent flow.
    When I request an OAuth access token via authorization code grant with scope "PRODUCTION"
    Then The response status code should be 200
    # The resource owner's user id, read from the token this grant issued. It is minted when the block provisions
    # the actor, so the enduser/sub claims below are pinned against it rather than against a literal.
    And I extract JWT claim "sub" from access token "generatedAccessToken" and store it as "acEndUserId"

    When I invoke the API at gateway context "{{acApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # App/API/subscription claims are grant-invariant — identical to the client-credentials path above.
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "applicationname" with value "{{acAppName}}"
    And The reflected backend JWT should contain claim "apiname" with value "{{acApiName}}"
    And The reflected backend JWT should contain claim "version" with value "1.0.0"
    And The reflected backend JWT should contain claim "subscriber" with value "<actor>"
    # iss and applicationtier are the gateway-generator invariants pinned exactly on every backend-JWT path.
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    # JOSE header: typ declares the assertion is a JWT, alg/kid pin the signing key (same shipped wso2carbon cert).
    And The reflected backend JWT header should contain "typ" with value "JWT"
    And The reflected backend JWT should be signed with algorithm "RS256"
    And The reflected backend JWT header should contain "kid" with value "Q049bG9jYWxob3N0LCBPVT1XU08yLCBPPVdTTzIsIEw9TW91bnRhaW4gVmlldywgU1Q9Q0EsIEM9VVMjMjExMjc5NDc5Njg2NTExMjgzMzcxNzY2ODEwMjc1MjAyMjU0ODQ5MzE4NzgwMzI3"
    # A user-profile claim the resource owner never configured must NOT leak — admin has no mobile claim.
    And The reflected backend JWT should not contain claim "mobile"
    # DISCRIMINATORS of the resource-owner path, both observed on a live 4.7.0 container: enduser is the resource
    # owner's USER ID qualified with the tenant domain (the api-key path carries the bare username instead), and
    # that same user id is present as sub (the api-key path has no sub at all).
    And The reflected backend JWT should contain claim "enduser" with value "{{acEndUserId}}@<tenantDomain>"
    And The reflected backend JWT should contain claim "sub" with value "{{acEndUserId}}"

    Examples:
      | actor             | tenantDomain |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |

  # Same backend-JWT claim family reached via a DEVPORTAL API KEY instead of an OAuth access token. The four legacy
  # api-key JWT methods (testAPIKeyOnlySecuredAPIInvocation, testEnableJWTAndClaimsForAPIKeyApp,
  # testOpaqueAPIKeyForAPIKeyApp, testOpaqueAPIKeyOnlySecuredAPIInvocation) collapse to ONE arc: all four use the
  # same JWT-type application and the same devportal generate-api-keys endpoint (the "Opaque" pair is a misnomer —
  # there is no separate opaque api-key credential in this product). Two claims DISCRIMINATE the api-key path from
  # the client-credentials scenario above: enduser is the bare username and sub is absent.
  @cap:key-manager @feat:backend-jwt @rule:api-key-credential @type:regression @dep:gateway @legacy:JWTTestCase
  Scenario Outline: The backend JWT is generated for a devportal API-KEY invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "kApiId" and deployed it

    # The reflect API defaults to oauth2 only; enable the api_key scheme and redeploy so the gateway honours a
    # devportal API key on it, then publish.
    When I retrieve the "apis" resource with id "kApiId"
    Then The response status code should be 200
    And I put the response payload in context as "kApiPayload"
    When I update the "apis" resource "kApiId" and "kApiPayload" with configuration type "securityScheme" and value:
      """
      ["oauth2", "api_key"]
      """
    Then The response status code should be 200
    When I deploy the API with id "kApiId"
    Then The response status code should be 201
    And I wait until "apis" "kApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "kApiId"
    Then The lifecycle status of API "kApiId" should be "Published"
    When I retrieve the "apis" resource with id "kApiId"
    And I extract response field "context" and store it as "kApiContext"

    # Same application shape as the client-credentials scenario: tokenType JWT and the required application
    # attribute (the block overlay declares one). The api-key credential is issued against THIS application.
    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "kAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "kAppPayload"
    And I create an application with payload "kAppPayload"
    Then The response status code should be 201

    When I put the following JSON payload in context as "kSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "kApiId" using application "createdAppId" with payload "kSub" as "kSubId"
    Then The response status code should be 201

    # Mint a devportal API key for the subscribed application (the api-key credential needs no OAuth keys).
    When I put the following JSON payload in context as "kApiKeyGen"
    """
    {"keyName": "BackendJwtApiKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "kApiKeyGen"
    Then The response status code should be 200

    # Invoke with the API key; the reflecting backend echoes the gateway-injected X-JWT-Assertion.
    When I invoke the API at gateway context "{{kApiContext}}/1.0.0/reflect-headers" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # iss and applicationtier are the gateway-generator invariants shared with the client-credentials path — pinned
    # exactly so a generator change dropping or renaming either fails here too.
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    # typ in the JOSE header declares the assertion is a JWT; the payload-claim assertions below are blind to it.
    And The reflected backend JWT header should contain "typ" with value "JWT"
    # alg pins the signing algorithm on the api-key path too (RS256 = SHA256withRSA); an alg:none downgrade would
    # leave the backend trusting an unverifiable identity assertion, invisible to every payload-claim check below.
    And The reflected backend JWT header should contain "alg" with value "RS256"
    # kid names the verification key the backend selects; a blank/absent kid leaves it unable to pick one. Same
    # base64(DN+serial) of the shipped wso2carbon signing certificate as the client-credentials path.
    And The reflected backend JWT header should contain "kid" with value "Q049bG9jYWxob3N0LCBPVT1XU08yLCBPPVdTTzIsIEw9TW91bnRhaW4gVmlldywgU1Q9Q0EsIEM9VVMjMjExMjc5NDc5Njg2NTExMjgzMzcxNzY2ODEwMjc1MjAyMjU0ODQ5MzE4NzgwMzI3"
    # DISCRIMINATOR 1: on the api-key path enduser is the bare username (the client-credentials path gives
    # admin@carbon.super). Parameterised per row; both row values are measured — see the note at the Examples table.
    And The reflected backend JWT should contain claim "enduser" with value "<enduser>"
    # DISCRIMINATOR 2: sub is ABSENT on the api-key path (client-credentials carries a UUID sub) — proves the
    # credential path differs; asserted on the parsed claim set so a prefix name cannot mask a leak.
    And The reflected backend JWT should not contain claim "sub"

    # JWTDecodingTestCase#testJWTDecodingforCustomApplicationWithOpaqueKey repeats the api-key invocation on this
    # same JWT-type application: a SECOND identical call must still yield a correctly-assembled backend JWT, guarding
    # a caching/replay regression where the repeat call behaves differently from the first. Only the repeat is ported
    # here — the method's other dimensions (JWT-type app, api-key credential, the same claim set) already coincide
    # with this scenario's first invoke, and its dotted-username leg lives on the password-grant path (dotted-username
    # scenario below), so re-invoking + re-asserting the discriminators is the sole non-redundant addition.
    When I invoke the API at gateway context "{{kApiContext}}/1.0.0/reflect-headers" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "enduser" with value "<enduser>"
    And The reflected backend JWT should not contain claim "sub"

    # Both enduser values are observed on a live 4.7.0 container: the api-key path drops the default carbon.super
    # domain ("admin") but retains an explicit tenant domain ("admin@tenant1.com"), which is why the two cells differ.
    Examples:
      | actor             | enduser           |
      | admin             | admin             |
      | admin@tenant1.com | admin@tenant1.com |

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
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    # (The gateway masks the JWT subject/enduser to a pseudonymous UUID in 4.7.0, so the username is not asserted
    #  — a clean 200 proves backend-JWT generation succeeded for the dotted-username owner, matching the legacy
    #  which asserts only the status code.)
    # Repeat — the dotted-username decoding failure surfaced on the SECOND call before the fix. The CLAIM
    # assertion below is the detector, not the status: a cache entry stored under a mis-split username yields a
    # wrong/absent claim here even when the call still returns 200. The status gate stays "until 200" precisely so
    # the response IS stored for that claim to be read — the non-retrying exact-count step asserts the status
    # without publishing a response, which would silently leave the claim re-read against the FIRST call's. The
    # retry costs nothing here because the regression is deterministic: a poisoned cache fails every attempt.
    When I invoke the API at gateway context "{{dApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # The API-KEY arm of JWTDecodingTestCase (testJWTDecodingforCustomApplication's apikey half +
  # testJWTDecodingforCustomApplicationWithOpaqueKey), on an application whose OWNER carries a dotted
  # ([string].[string]) username. The credential's identity claim is derived from that owner's username, which is
  # exactly where the defect lived: the dot could be mis-split into nested claim segments, and the SECOND call —
  # served from the cache populated by the first — is what regressed. So the key is minted BY the dotted user
  # (making them the application owner and the key's auth user) and the invocation is asserted TWICE, pinning the
  # owner-derived claims each time.
  #
  # Legacy forged its JWT-format api key with the product's private key. That is not needed and not repeated: the
  # forgery only bought an arbitrary (dotted) owner, which is obtained legitimately here by provisioning the user
  # and issuing a REAL product key. NOTE on the JWT-vs-opaque split — in 4.7.0 the devportal application api-key
  # endpoint has ONE format: APIConsumerImpl#generateApiKey(app,user,validity,ip,referer,keyName) always calls
  # generateOpaqueKey() (32 random bytes), and the JWT-emitting overload is @Deprecated with no caller. There is
  # therefore no per-request flag, key-manager setting or tenant-config that selects a JWT-format application api
  # key, so the two legacy methods collapse onto this single credential. The key itself is opaque and carries no
  # decodable claims (and the api-key listing DTO exposes only keyUUID/keyName/issuedOn/validityPeriod/lastUsed —
  # no owner), so the owner-derived claim assertion is made where the product DOES surface it: the
  # gateway-injected backend JWT, whose enduser/subscriber claims are built from the api key's auth user.
  @cap:key-manager @feat:backend-jwt @rule:dotted-username @type:regression @dep:gateway @legacy:JWTDecodingTestCase
  Scenario Outline: The backend JWT is generated for an api-key credential owned by a dotted-username user in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "jwtdecode.keyowner" with roles "Internal/subscriber" in tenant "<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "dkApiId" and deployed it

    # The reflect API defaults to oauth2 only; enable api_key and redeploy so the gateway honours a devportal
    # API key on it, then publish.
    When I retrieve the "apis" resource with id "dkApiId"
    Then The response status code should be 200
    And I put the response payload in context as "dkApiPayload"
    When I update the "apis" resource "dkApiId" and "dkApiPayload" with configuration type "securityScheme" and value:
      """
      ["oauth2", "api_key"]
      """
    Then The response status code should be 200
    When I deploy the API with id "dkApiId"
    Then The response status code should be 201
    And I wait until "apis" "dkApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "dkApiId"
    Then The lifecycle status of API "dkApiId" should be "Published"
    When I retrieve the "apis" resource with id "dkApiId"
    And I extract response field "context" and store it as "dkApiContext"

    # Switch to the DOTTED user for every devportal operation, so the application's owner — and hence the api
    # key's auth user — is the dotted username rather than admin.
    Given The system is ready and I have valid devportal access token as "jwtdecode.keyowner<suffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "dkAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "dkAppPayload"
    And I create an application with payload "dkAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    # The dotted username survives INTACT as the recorded owner (not truncated at the dot) — the precondition
    # that makes the claim assertions below meaningful.
    And The value of response field "owner" should be "<owner>"
    And I extract response field "name" and store it as "dkAppName"

    When I put the following JSON payload in context as "dkSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "dkApiId" using application "createdAppId" with payload "dkSub" as "dkSubId"
    Then The response status code should be 201

    # Mint the api key AS the dotted user, so its auth user is the dotted username.
    When I put the following JSON payload in context as "dkApiKeyGen"
    """
    {"keyName": "DottedOwnerApiKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "dkApiKeyGen"
    Then The response status code should be 200

    # FIRST invocation. The key must WORK (200), and the backend JWT the gateway assembles from it must carry the
    # dotted username as ONE claim value — a mis-split would yield a truncated/nested value and fail these.
    When I invoke the API at gateway context "{{dkApiContext}}/1.0.0/reflect-headers" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "enduser" with value "<owner>"
    And The reflected backend JWT should contain claim "subscriber" with value "<owner>"
    And The reflected backend JWT should contain claim "applicationname" with value "{{dkAppName}}"
    # sub is ABSENT on the api-key path (see the api-key-credential scenario above) — pinned so a claim-decoding
    # change that starts emitting a mis-split identity under sub cannot slip through unnoticed.
    And The reflected backend JWT should not contain claim "sub"

    # REPEAT — the whole point of the legacy method. The first call populates the gateway's cache keyed on the
    # decoded username; before the fix the second call failed. The CLAIM re-assertions below are the real detector:
    # a cache entry stored under a mis-split key yields wrong claims here even though the call returns 200.
    # (The status gate is still "until 200" only because no by-CONTEXT api-key step invokes without retrying, and
    # the "at path" variant double-prefixes tenant contexts. That is tolerable because the regression is
    # deterministic — a poisoned cache fails every retry — but the claims, not the status, are what pin it.)
    When I invoke the API at gateway context "{{dkApiContext}}/1.0.0/reflect-headers" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "enduser" with value "<owner>"
    And The reflected backend JWT should contain claim "subscriber" with value "<owner>"
    And The reflected backend JWT should contain claim "applicationname" with value "{{dkAppName}}"
    And The reflected backend JWT should not contain claim "sub"

    Examples:
      | tenant       | suffix       | owner                          |
      | carbon.super |              | jwtdecode.keyowner             |
      | tenant1.com  | @tenant1.com | jwtdecode.keyowner@tenant1.com |

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
