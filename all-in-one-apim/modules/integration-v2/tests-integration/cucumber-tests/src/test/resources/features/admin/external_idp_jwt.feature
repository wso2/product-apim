@cap:admin @feat:external-key-manager @legacy:ExternalIDPJWTTestCase
Feature: External IdP Self-Validated JWT and Key Manager Token Type Lifecycle

  Ports the legacy ExternalIDPJWTTestCase: a third-party IdP's OWN JWT presented straight at the gateway. The two
  key managers this block registers never contact their IdP - each self-validates the token signature against a
  pinned PEM certificate whose key pair is committed with the tests, and resolves the application from the token's
  azp claim through a key MAPPING (not a generated key), so no Identity Server is involved.

  What is under test here, and nowhere else in v2:
   * the key manager tokenType lifecycle and what each value does AT RUNTIME - DIRECT accepts the IdP's own token
     at the gateway; EXCHANGED does not (the key manager is not registered in the gateway's JWT-validator map at
     all) but is trusted for an RFC 8693 exchange; BOTH does both simultaneously; and moving back to DIRECT
     DELETES the trusted IdP so the exchange stops trusting that issuer. The tokenType update is applied by each
     scenario for the value it needs, so the scenarios are order-independent despite sharing one key manager.
   * the key manager's claimMapping translating remote claims into the local dialect on the backend JWT, next to
     the two claims that are NOT mapped: one merely unmapped (passes through under its original name) and one
     unmapped AND in the block's excluded_claims (absent) - two distinct rules the legacy test conflated.
   * the unknown-azp (403 / 900908) and untrusted-signer (401 / 900901) refusals, with their exact codes.

  Runs x2 tenant (carbon.super and tenant1.com) - each scenario acts as its tenant's admin and selects that
  tenant's fixture (API, application, key managers, mapped consumer keys) from _setup_external_idp_jwt. Every
  external JWT is minted fresh with its own jti, so the gateway token cache can never serve a verdict reached
  under a previous tokenType. Not per-scenario @cleanup: the fixture must survive the whole runner, so teardown is
  the runner's AfterClass sweep.

  @rule:claim-mapping @type:regression @dep:gateway
  Scenario Outline: A mapped external-IdP JWT invokes the gateway and its claims are translated into the backend JWT as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "DIRECT"
    Then The response status code should be 200
    And The value of response field "tokenType" should be "DIRECT"
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The three claims the key manager maps arrive under the LOCAL dialect names, carrying the remote values.
    And The reflected backend JWT should contain claim "http://wso2.org/claims/givenname" with value "first"
    And The reflected backend JWT should contain claim "http://wso2.org/claims/firstname" with value "last"
    And The reflected backend JWT should contain claim "http://wso2.org/claims/email" with value "first@gmail.com"
    # A remote claim with NO mapping is NOT dropped - the claim transformer passes it through under its original
    # name. Pinned explicitly because it is what makes the exclusion below a real, config-driven outcome.
    And The reflected backend JWT should contain claim "http://idp.org/claims/department" with value "platform"
    # ... whereas this one, equally unmapped but listed in the block's [apim.jwt.gateway_generator]
    # excluded_claims, is absent. (Legacy asserted only this absence, so it could not tell the two rules apart.)
    And The reflected backend JWT should not contain claim "http://idp.org/claims/mobileno"
    # The mapped remote names must not ALSO survive alongside their local translations.
    And The reflected backend JWT should not contain claim "http://idp.org/claims/givenname"
    # typ pins the JOSE header declares a JWT — legacy checked typ/alg = JWT/RS256; the claim assertions above
    # read the payload only, so a header that stopped declaring its type would slip past them.
    And The reflected backend JWT header should contain "typ" with value "JWT"
    # The assertion is SIGNED, and with the algorithm the block's overlay leaves at its default. Every claim
    # assertion above reads the PAYLOAD, which is byte-identical whether or not the gateway signed the assertion -
    # so without this the backend would silently lose the ability to trust the injected identity while the whole
    # arc stayed green. Legacy checked the same header (typ/alg = JWT/RS256) plus a signature verification.
    And The reflected backend JWT should be signed with algorithm "RS256"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:claim-mapping @type:regression @dep:gateway
  Scenario Outline: A second external key manager with its own claim namespace maps onto the same local claims as <actor>
    # Two self-validating key managers co-exist in one tenant, each with its own issuer, its own signing key pair
    # and its own remote claim namespace (http://idp2.org/claims/* here). A token from the second one lands on the
    # SAME local claims as the first, and its unmapped mobileno claim - NOT in excluded_claims, which names only
    # the first namespace - passes through, pinning that the exclusion is per claim NAME, not a blanket
    # unmapped-claim drop.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm2" setting its token type to "DIRECT"
    Then The response status code should be 200
    When I obtain a self-signed JWT from external IdP "idp2" for the mapped consumer key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "http://wso2.org/claims/givenname" with value "first"
    And The reflected backend JWT should contain claim "http://wso2.org/claims/firstname" with value "last"
    And The reflected backend JWT should contain claim "http://wso2.org/claims/email" with value "first@gmail.com"
    And The reflected backend JWT should contain claim "http://idp2.org/claims/mobileno" with value "424479772294778"
    And The reflected backend JWT should not contain claim "http://idp2.org/claims/givenname"
    # typ pins the JOSE header declares a JWT — the claim assertions above read the payload only, so a header that
    # stopped declaring its type would slip past them.
    And The reflected backend JWT header should contain "typ" with value "JWT"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-validation @type:negative @dep:gateway
  Scenario Outline: An external-IdP JWT whose authorized party is not mapped to an application is refused as <actor>
    # A correctly signed token from a trusted issuer, but its azp resolves to no key mapping, so subscription
    # validation cannot find an application: 403 with 900908. The status alone cannot tell this apart from a
    # missing subscription, so the code and the message are asserted too.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "DIRECT"
    Then The response status code should be 200
    When I obtain a self-signed JWT from external IdP "idp1" for an unmapped consumer key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The value of error response field "code" should be "900908"
    And The response should contain "User is NOT authorized to access the Resource. API Subscription validation failed."

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-validation @type:negative @dep:gateway
  Scenario Outline: An external-IdP JWT signed by a key the key manager does not trust is refused as <actor>
    # The token claims the first IdP's issuer (so it resolves to that key manager) but is signed with the SECOND
    # IdP's key pair, which the first key manager's pinned certificate does not correspond to: 401 with 900901 /
    # "Invalid Credentials". Distinct from the tampered-payload negative in is7_token_validation.feature, which
    # mutates a claim of an otherwise legitimately signed token - here the whole signature is from a foreign key.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "DIRECT"
    Then The response status code should be 200
    When I obtain a self-signed JWT from external IdP "idp1" signed by the other IdP key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The value of error response field "code" should be "900901"
    And The value of error response field "message" should be "Invalid Credentials"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-type @type:regression @dep:gateway
  Scenario Outline: A BOTH-type key manager accepts the same external token directly and through a token exchange as <actor>
    # tokenType=BOTH is the only value under which the two invocation methods work SIMULTANEOUSLY: the key manager
    # is registered in the gateway's JWT-validator map (so its issuer's own token authenticates directly) AND it
    # is backed by a trusted IdP (so that same token can be exchanged for an API Manager access token). One
    # minted JWT drives both halves.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "BOTH" with identity provider alias "external-idp-1-audience"
    Then The response status code should be 200
    And The value of response field "tokenType" should be "BOTH"
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    And the generated access token should have the "at+jwt" type header
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-type @type:regression @dep:gateway
  Scenario Outline: Switching a key manager to token-exchange-only stops direct tokens while the exchange still works as <actor>
    # The runtime effect of the tokenType UPDATE: an EXCHANGED key manager is not added to the gateway's
    # JWT-validator map, so the gateway no longer recognises the issuer and refuses the token - while the trusted
    # IdP behind the key manager survives, so the exchange route keeps working. A DIFFERENT cause from
    # token_exchange_disabled.feature, which refuses the GRANT itself in server config.
    #
    # The refusal is 500 / 900900 "Unclassified Authentication Failure", NOT the 401 / 900901 an unrecognised
    # credential would suggest, and that is asserted here deliberately rather than widened to "not 200":
    # with no JWT validator for the issuer, no authenticator in the chain produces a usable failure verdict, so
    # APIAuthenticationHandler#getError falls through to its last-resort branch - the one the product itself
    # comments "ideally this should not exist" - which maps to API_AUTH_GENERAL_ERROR. Nothing is dialled out to:
    # there is no connection attempt in the gateway log, so this is an in-process fallback and not an unreachable
    # introspection endpoint. Legacy asserted a bare 401 here against a "custom"-type key manager; this arc runs a
    # WSO2-IS-7 one and gets a 500, which is reported as a suspected product defect rather than papered over.
    #
    # IF THIS SCENARIO EVER FAILS BECAUSE THE RESPONSE BECAME 401 / 900901, THE PRODUCT HAS BEEN FIXED, NOT BROKEN:
    # 401 is the correct status for an unrecognised credential and 500 / 900900 is the observed-but-wrong behaviour
    # pinned below. Update the two assertions to the 401 - do NOT widen them to "not 200", and do not assume the
    # test is at fault. Provenance of the mechanism above: APIAuthenticationHandler.java:676 (getError) and its
    # 698-701 fallback, plus APISecurityConstants.java:23-24 (API_AUTH_GENERAL_ERROR = 900900), read from the
    # org.wso2.carbon.apimgt.gateway 9.32.147 SOURCES jar - the pinned 9.33.162 ships no sources jar.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "BOTH" with identity provider alias "external-idp-1-audience"
    Then The response status code should be 200
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I update the key manager "extIdpKm1" setting its token type to "EXCHANGED" with identity provider alias "external-idp-1-audience"
    Then The response status code should be 200
    And The value of response field "tokenType" should be "EXCHANGED"
    # A FRESH token, so the refusal cannot be a cached verdict from before the update.
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500
    # Code only: 900900 maps 1:1 onto its message, unlike 901405 which two different causes share, so pinning the
    # message here would add no discriminating power.
    And The value of error response field "code" should be "900900"
    When I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-type @type:regression @dep:gateway
  Scenario Outline: Switching a key manager back to direct restores direct tokens and stops trusting the issuer for exchange as <actor>
    # The mirror image, and the reason the DIRECT transition is not merely cosmetic: moving a key manager from
    # EXCHANGED/BOTH to DIRECT DELETES the trusted IdP that backed it, so the exchange no longer has anything to
    # validate the subject token's issuer against and is refused 400 - while the key manager returns to the
    # gateway's JWT-validator map and its own tokens authenticate again.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "EXCHANGED" with identity provider alias "external-idp-1-audience"
    Then The response status code should be 200
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    When I update the key manager "extIdpKm1" setting its token type to "DIRECT"
    Then The response status code should be 200
    And The value of response field "tokenType" should be "DIRECT"
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 400
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @restore-key-mapping @rule:key-mapping-grants @type:regression
  Scenario Outline: Removing the token-exchange grant from an existing key mapping refuses the exchange and restoring it recovers as <actor>
    # The update-key-mapping round trip: PUT supportedGrantTypes on the application's EXISTING Resident-KM key
    # mapping and assert the returned grant list before and after, with the exchange proving the change took
    # effect at the token endpoint each way. (v2 previously only ever pre-provisioned a separate grantless
    # application, which cannot show that a grant can be taken away from and given back to a live mapping.)
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "BOTH" with identity provider alias "external-idp-1-audience"
    Then The response status code should be 200
    When I register key mapping "keyMappingId" on application "createdAppId" for cleanup restoration
    When I update the supported grant types of key mapping "keyMappingId" on application "createdAppId" to "client_credentials"
    Then The response status code should be 200
    And The response should contain "client_credentials"
    And The response should not contain "urn:ietf:params:oauth:grant-type:token-exchange"
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 400
    When I update the supported grant types of key mapping "keyMappingId" on application "createdAppId" to "client_credentials,urn:ietf:params:oauth:grant-type:token-exchange"
    Then The response status code should be 200
    And The response should contain "client_credentials"
    And The response should contain "urn:ietf:params:oauth:grant-type:token-exchange"
    When I obtain a self-signed JWT from external IdP "idp1" for the mapped consumer key
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-type @type:negative
  Scenario Outline: Key generation against a token-exchange-only key manager is refused as <actor>
    # A token-exchange-only key manager provisions no OAuth client, so key generation is refused with 400 / 901405.
    # 901405 does NOT identify the cause on its own: two different ExceptionCodes constants share that code, and the
    # tokenType gate and the enableOAuthAppCreation gate throw the SAME one - same code, same message. So the cause
    # is isolated by CONSTRUCTION instead: the key manager under test has enableOAuthAppCreation AND
    # enableTokenGeneration both TRUE (wso2is7-config-token-exchange.json), leaving the tokenType gate as the only
    # reachable refusal. is7_keygen_negatives.feature covers the enableOAuthAppCreation gate.
    # The message still matters, because tokenType is checked at TWO depths with DIFFERENT messages - the outer gate
    # (APIConsumerImpl, alongside the app-creation check) says "generating OAuth applications" and the inner one in
    # the registration workflow says "token generation". Asserting the message pins WHICH gate answered, and the
    # outer one must, since it is reached first. Legacy asserted this same string for the EXCHANGED case.
    # Every gate reads the DAO before any DCR, so nothing is asked of an Identity Server and a freshly created key
    # manager needs no propagation wait. A FRESH application is used so the response reflects the key-manager
    # condition and not a mapping clash.
    Given I act as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-token-exchange.json" as "exchangeOnlyKm"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "exchangeOnlyAppPayload"
    And I create an application with payload "exchangeOnlyAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "exchangeOnlyKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{exchangeOnlyKmName}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "exchangeOnlyKeygenPayload"
    Then The response status code should be 400
    And The value of error response field "code" should be "901405"
    And The value of error response field "message" should be "Key Manager doesn't support generating OAuth applications"
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @rule:token-type @type:negative
  Scenario Outline: Mapping an OAuth client to a token-exchange-only key manager is refused as <actor>
    # The same rule at the other entry point: map-keys also refuses a token-exchange-only key manager
    # (400 / 901405), so an operator cannot bind a consumer key to a key manager that will never issue one. Only
    # EXCHANGED is refused - which is why the setup can make its key mappings while the key managers are BOTH.
    # No cause-isolation dance is needed here (unlike the keygen sibling above): the tokenType gate in
    # mapExistingOAuthClient is the ONLY 901405 reachable on the map-keys path, so code + message pin it exactly.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I update the key manager "extIdpKm1" setting its token type to "EXCHANGED" with identity provider alias "external-idp-1-audience"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "mapExchangeOnlyAppPayload"
    And I create an application with payload "mapExchangeOnlyAppPayload"
    Then The response status code should be 201
    When I mint an external IdP consumer key as "mapExchangeOnlyAzp"
    And I map OAuth client "mapExchangeOnlyAzp" to application "createdAppId" via key manager "{{extIdpKm1Name}}"
    Then The response status code should be 400
    And The value of error response field "code" should be "901405"
    And The value of error response field "message" should be "Key Manager doesn't support generating OAuth applications"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
