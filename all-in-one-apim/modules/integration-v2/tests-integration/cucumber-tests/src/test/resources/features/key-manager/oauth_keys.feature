@cleanup
Feature: Key Manager OAuth Application Keys

  Key-manager-plane OAuth key generation: generate production consumer credentials for an application and
  exchange them for an application access token. This is the key-generation arc factored out of
  devportal/applications. Runs as admin in both the super tenant and tenant1.com. Teardown via the
  per-scenario cleanup hook.

  @cap:key-manager @feat:oauth-keys @type:smoke @legacy:ApplicationKeyGenerationTestCase
  Scenario Outline: Generate OAuth keys for an application and obtain an access token as <actor>
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
    And The response should contain "consumerKey"
    And The response should contain "consumerSecret"

    When I put the following JSON payload in context as "createApplicationAccessTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200
    And The response should contain "accessToken"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # @legacy:APIMANAGER5327... — the PGSQL partial-key-cleanup regression (APIMANAGER-5327) is the SAME behaviour:
  # generate keys, then clean up the registration without error. The legacy test switched to a live PostgreSQL
  # datasource and hit a removed Jaggery endpoint (cleanUpApplicationRegistration.jag); this modern REST scenario
  # is DB-agnostic, so when the suite is matrixed onto PostgreSQL it exercises the same cleanup path on PGSQL.
  @cap:key-manager @feat:oauth-keys @type:regression @legacy:ApplicationTestCase @legacy:APIMANAGER5327KeyGenerationWithPGSQLTestCase
  Scenario Outline: Clean up an application's key registration as <actor>
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

    When I clean up the key registration for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Legacy GrantTypeTokenGenerateTestCase#testApplicationCreationWithoutCallBackURL asserted only that key
  # generation with an empty callbackUrl fails. On its own that is vacuous — an empty callbackUrl could be
  # rejected for any reason. The second leg here is the control that gives it meaning: the SAME empty callbackUrl
  # is ACCEPTED (200) when only client_credentials/password are requested, so the rejection is provably about the
  # redirect-based grant types and not about the empty string.
  @cap:key-manager @feat:oauth-keys @rule:callback-url @type:negative @legacy:GrantTypeTokenGenerateTestCase
  Scenario Outline: Key generation is refused without a callback URL only when a redirect-based grant is requested as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201

    When I put the following JSON payload in context as "noCallbackRedirectGrantsPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "authorization_code", "implicit"], "callbackUrl": ""}
    """
    And I generate client credentials for application id "createdAppId" with payload "noCallbackRedirectGrantsPayload"
    Then The response status code should be 400
    And The response should contain "The callback url must have at least one URI value when using Authorization code or implicit grant types."

    When I put the following JSON payload in context as "noCallbackPlainGrantsPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"], "callbackUrl": ""}
    """
    And I generate client credentials for application id "createdAppId" with payload "noCallbackPlainGrantsPayload"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # TODO(coverage): consumer-secret rotation (ports ApplicationRegenerateConsumerSecretTestCase) is not
  # covered here.
  # WHAT THE MISSING SCENARIO MUST ASSERT: POST /applications/{appId}/oauth-keys/{keyMappingId}/regenerate-secret
  # returns 200 with a consumerSecret that DIFFERS from the previous one, and the rotation is then proven at the
  # token endpoint — a client_credentials request with the OLD secret is refused, and one with the NEW secret
  # succeeds. Asserting the 200 alone would not show the secret actually rotated.
  # NOT the same thing as multiple_client_secrets.feature, which covers ADDITIONAL secrets on a key mapping
  # (generate / list / revoke extras) — a different endpoint. The PRIMARY secret's rotation is uncovered.
  # OBSERVED TODAY: the endpoint answers 500 (900967) on that happy path against the resident key manager, so
  # there is nothing stable to pin. Settle the intended behaviour first rather than encoding today's response.

  # Ports the ASSERTION of CAPIMGT12CallBackURLOverwriteTestCase (the CAPIMGT-12 regression) — the half
  # devportal/applications.feature's owner-isolation scenario cannot reach. That scenario proves only that two
  # owners may hold same-named applications (both 201, distinct ids); it never sets, updates or re-reads a KEY
  # callbackUrl, so the actual defect CAPIMGT-12 guards — updating one owner's key callbackUrl overwriting the
  # similarly-named application's row of ANOTHER owner in IDN_OAUTH_CONSUMER_APPS — would go undetected.
  # Here each owner generates a PRODUCTION key with its OWN distinct callbackUrl on a SAME-NAMED application;
  # owner1's value is asserted BEFORE owner2's update (the baseline, without which the after-value proves
  # nothing), then re-asserted after, and the two owners' final values must differ.
  # NOTE the application-level callbackUrl of ApplicationCallbackURLTestCase is NOT portable — ApplicationDTO has
  # no callbackUrl field on this build (see the report); callbackUrl exists only on the KEY, which is what this
  # scenario pins.
  @cap:key-manager @feat:oauth-keys @rule:owner-isolation @type:regression @dep:devportal @legacy:CAPIMGT12CallBackURLOverwriteTestCase
  Scenario Outline: One owner's key callbackUrl update does not overwrite another owner's same-named application as <owner>
    Given The system is ready
    And I have valid access tokens as "<owner>"

    # Owner 1 (the acting admin): a same-named application with a PRODUCTION key carrying ITS OWN callbackUrl.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "cbOwner1App"
    And I set the field "name" to "CbSharedApp${UNIQUE:Cb}" in the payload "cbOwner1App"
    And I create an application with payload "cbOwner1App"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "cbOwner1AppId"
    And I extract response field "name" and store it as "cbSharedName"
    When I put the following JSON payload in context as "cbOwner1KeysPayload"
    """
    {"keyType": "PRODUCTION", "callbackUrl": "https://owner1.callback.example.com/cb", "grantTypesToBeSupported": ["client_credentials", "authorization_code"]}
    """
    And I generate client credentials for application id "cbOwner1AppId" with payload "cbOwner1KeysPayload"
    Then The response status code should be 200
    And I extract response field "keyMappingId" and store it as "cbOwner1KeyMappingId"

    # Owner 2 (a different user in the same tenant): an application with the SAME name and its own key/callbackUrl.
    Given I have a valid DCR application as "<otherOwner>"
    And I have a valid Devportal access token as "<otherOwner>"
    And I act as "<otherOwner>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "cbOwner2App"
    And I set the field "name" to "{{cbSharedName}}" in the payload "cbOwner2App"
    And I create an application with payload "cbOwner2App"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "cbOwner2AppId"
    When I put the following JSON payload in context as "cbOwner2KeysPayload"
    """
    {"keyType": "PRODUCTION", "callbackUrl": "https://owner2.callback.example.com/cb", "grantTypesToBeSupported": ["client_credentials", "authorization_code"]}
    """
    And I generate client credentials for application id "cbOwner2AppId" with payload "cbOwner2KeysPayload"
    Then The response status code should be 200
    And I extract response field "keyMappingId" and store it as "cbOwner2KeyMappingId"

    # BASELINE — owner 1's key callbackUrl, read as owner 1, BEFORE owner 2 updates anything.
    Given I act as "<owner>"
    When I fetch the oauth key details for application "cbOwner1AppId" with key mapping "cbOwner1KeyMappingId"
    Then The response status code should be 200
    And The value of response field "callbackUrl" should be "https://owner1.callback.example.com/cb"

    # Owner 2 UPDATES its own key's callbackUrl.
    Given I act as "<otherOwner>"
    When I put the following JSON payload in context as "cbOwner2UpdatePayload"
    """
    {"keyType": "PRODUCTION", "callbackUrl": "https://owner2-updated.callback.example.com/cb", "supportedGrantTypes": ["client_credentials", "authorization_code"]}
    """
    And I update the keys for application "cbOwner2AppId" with key mapping "cbOwner2KeyMappingId" using payload "cbOwner2UpdatePayload"
    Then The response status code should be 200
    And The value of response field "callbackUrl" should be "https://owner2-updated.callback.example.com/cb"

    # Owner 1's key callbackUrl is UNCHANGED — the CAPIMGT-12 regression assertion.
    Given I act as "<owner>"
    When I fetch the oauth key details for application "cbOwner1AppId" with key mapping "cbOwner1KeyMappingId"
    Then The response status code should be 200
    And The value of response field "callbackUrl" should be "https://owner1.callback.example.com/cb"
    And I extract response field "callbackUrl" and store it as "cbOwner1FinalCallback"

    # ...and the two owners' final callback URLs differ (neither was collapsed onto the other).
    Given I act as "<otherOwner>"
    When I fetch the oauth key details for application "cbOwner2AppId" with key mapping "cbOwner2KeyMappingId"
    Then The response status code should be 200
    And I extract response field "callbackUrl" and store it as "cbOwner2FinalCallback"
    And The stored value "cbOwner1FinalCallback" should not equal "cbOwner2FinalCallback"

    Examples:
      | owner             | otherOwner                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
