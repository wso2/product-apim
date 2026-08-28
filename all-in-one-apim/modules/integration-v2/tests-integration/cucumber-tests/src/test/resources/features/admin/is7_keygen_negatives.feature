@cap:admin @feat:external-key-manager @rule:keygen @type:negative
Feature: External Key Manager Key Generation Negatives

  Key-generation failure modes for the IS7 external key manager. Each scenario registers its own uniquely-named
  IS7 KM (control-plane) and generates keys for a FRESH application against it - a fresh app avoids the
  "Key Mappings already exists" (409) conflict that a reused app would raise, so the response reflects the KM
  condition under test rather than a mapping clash. The KM and application are cleaned up by the runner.

  Scenario Outline: Key generation against a disabled WSO2-IS-7 key manager is refused as <actor>
    # A disabled KM must refuse key generation before any DCR to IS.
    Given I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-disabled.json" as "disabledKm"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "disabledAppPayload"
    And I create an application with payload "disabledAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "disabledKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{disabledKmName}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "disabledKeygenPayload"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  Scenario Outline: Key generation against an unreachable WSO2-IS-7 key manager yields no usable credential as <actor>
    # A KM whose endpoints are unreachable must never hand back working credentials. Asserted on the RESULTING
    # APPLICATION STATE, not on the keygen response, and that choice is deliberate:
    #
    # The keygen response is not the product's. APIM commits the key-mapping row, then the DCR to the dead endpoint
    # fails, and the real answer is 500 with the management API's GENERIC envelope
    # ({"code":900967,"message":"General Error","description":"Server Error Occurred","moreInfo":"","error":[]} -
    # measured; it says nothing about unreachability). SimpleHTTPClient matches 900967 and re-POSTs after 2000ms,
    # and the retry collides with the row attempt 1 left behind, so a caller sees 409 "Key Mappings already exists".
    # That 409 is a TEST-CLIENT artifact: a real Developer Portal user sees the 500. Asserting it would pin our
    # harness AND break the moment the 900967 retry is scoped away from non-idempotent verbs.
    #
    # The end state is identical either way - DCR never ran, so there is no credential - which is why it is the
    # sound observable. It is also the property actually worth guaranteeing: an unreachable IdP must not yield a
    # usable credential. The entry COUNT is the one part that merely characterises today's behaviour (the create is
    # not atomic, so a mapping survives a failed DCR); if APIM is ever made atomic it becomes 0 and this needs
    # revisiting. It is pinned regardless, because a count assertion is what stops the null checks below being
    # satisfied by an empty list.
    Given I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-unreachable.json" as "unreachableKm"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "unreachableAppPayload"
    And I create an application with payload "unreachableAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "unreachableKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{unreachableKmName}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "unreachableKeygenPayload"
    When I retrieve existing application keys for "createdAppId"
    Then The response status code should be 200
    And The response array field "list" should have exactly 1 entries
    And The value of response field "list[0].keyManager" should be "{{unreachableKmName}}"
    And The value of response field "list[0].keyType" should be "PRODUCTION"
    And The response field "list[0].consumerKey" should be null
    And The response field "list[0].consumerSecret" should be null

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @legacy:ExternalIDPJWTTestCase
  Scenario Outline: Key generation against a key manager that does not support OAuth app creation is refused as <actor>
    # A KM with enableOAuthAppCreation=false does not provision OAuth apps, so key generation is refused up front
    # with 400 / 901405 "Key Manager doesn't support generating OAuth applications" - before any DCR to IS.
    # Legacy parity: ExternalIDPJWTTestCase.generateKeysNegative, x2 tenant as its Factory ran it (both the
    # key-manager registry and the refusal are per-tenant). The sibling cause - a token-exchange-only key manager,
    # whose tokenType check fires BEFORE this one - is admin/external_idp_jwt.feature. Because BOTH causes throw the
    # same ExceptionCodes constant, the code alone cannot tell them apart, so the message is asserted too (its
    # description field is the same string literal by construction, so it adds nothing).
    # Tokens are minted per actor here rather than only selecting the actor: the tenant row needs a TENANT admin
    # token to register the key manager, and this block's setup only obtains the super-tenant one.
    Given I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-no-oauth-app-creation.json" as "noAppCreationKm"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "noAppCreationAppPayload"
    And I create an application with payload "noAppCreationAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "noAppCreationKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{noAppCreationKmName}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "noAppCreationKeygenPayload"
    Then The response status code should be 400
    And The value of error response field "code" should be "901405"
    And The value of error response field "message" should be "Key Manager doesn't support generating OAuth applications"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
