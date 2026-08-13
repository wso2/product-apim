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

  Scenario Outline: Key generation against an unreachable WSO2-IS-7 key manager fails cleanly as <actor>
    # A KM whose endpoints are unreachable must fail key generation with a clean, bounded error - not hang or 200.
    # Pinned behavior: APIM creates the key-mapping row then the DCR to the unreachable endpoint fails, surfacing
    # (within ~2s) as 409 "Key Mappings already exists" - distinct from reachable (200) and disabled (400). The
    # point of this test is the clean, prompt failure, whatever its exact code.
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
    Then The response status code should be 409

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
