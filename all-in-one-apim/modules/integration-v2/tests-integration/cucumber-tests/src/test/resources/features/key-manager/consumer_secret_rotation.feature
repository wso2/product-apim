@cleanup
Feature: Consumer Secret Rotation (single-secret model)

  Rotating an application's PRIMARY consumer secret, proven at the GATEWAY rather than on the management plane.
  Ports ApplicationRegenerateConsumerSecretTestCase.

  WHY THIS NEEDS ITS OWN BLOCK — the finding that produced it. On DEFAULT config the rotation endpoint
  (POST /applications/{appId}/oauth-keys/{keyMappingId}/regenerate-secret) answers 500 (900967): APIM's own DCR
  call is refused, because AMDefaultKeyManagerImpl#getNewApplicationConsumerSecret passes
  Base64.getUrlEncoder().encodeToString(clientId) where keymanager-operations' path parameter
  /dcr/register/{clientId}/regenerate-consumer-secret is documented as the RAW client identifier. With
  [oauth.multiple_client_secrets] enable = false the same call returns 200 — MEASURED, not inferred. So rotation
  is a capability of the SINGLE-SECRET model, and this block pins it there. The multiple-secret model's own
  behaviour is covered by multiple_client_secrets.feature on the default block; the two are complementary halves
  of the same feature area, deliberately split by the config that selects between them.

  WHAT IS ASSERTED, AND WHY EACH LEG EARNS ITS PLACE. A 200 from the rotation endpoint proves only that the
  request was accepted. The contract a caller actually depends on is that the OLD credential STOPS working and
  the NEW one starts — so both are proven by minting a real client_credentials token and invoking the gateway
  with it. Without the old-secret leg this would pass even if rotation issued a new secret while leaving the old
  one live, which is precisely the security property rotation exists to provide.
  Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:token-issuance @rule:secret-rotation @type:regression @dep:devportal @legacy:ApplicationRegenerateConsumerSecretTestCase
  Scenario Outline: Rotating a consumer secret revokes the old credential and issues a working new one as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "rotApiId" and deployed it
    When I publish the "apis" resource with id "rotApiId"
    Then The lifecycle status of API "rotApiId" should be "Published"
    When I retrieve the "apis" resource with id "rotApiId"
    And I extract response field "context" and store it as "rotApiContext"

    # An application with client_credentials keys, subscribed so its token can reach the gateway.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "rotAppPayload"
    And I create an application with payload "rotAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "rotKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "rotKeysPayload"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "rotConsumerKey"
    And I extract response field "consumerSecret" and store it as "rotOldSecret"
    When I put the following JSON payload in context as "rotSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{rotApiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "rotApiId" using application "createdAppId" with payload "rotSubPayload" as "rotSubId"
    Then The response status code should be 201

    # BEFORE: the original secret mints a token that the gateway accepts.
    When I request a client-credentials token using consumer key "rotConsumerKey" and secret "rotOldSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "generatedAccessToken"
    When I invoke the API at gateway context "{{rotApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # ROTATE. This is the call that answers 500 on default config — here it must succeed and yield a DIFFERENT
    # secret, which is the whole point of the operation.
    When I regenerate the consumer secret for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200
    And I extract response field "consumerSecret" and store it as "rotNewSecret"
    And The stored value "rotNewSecret" should not equal "rotOldSecret"

    # AFTER: the NEW secret works end to end.
    When I request a client-credentials token using consumer key "rotConsumerKey" and secret "rotNewSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "generatedAccessToken"
    When I invoke the API at gateway context "{{rotApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # AND the OLD secret is dead. Without this leg, a rotation that merely ADDED a secret would pass.
    When I request a client-credentials token using consumer key "rotConsumerKey" and secret "rotOldSecret"
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Deleting the key mapping removes the credential entirely: no token can be minted with it afterwards.
  # Legacy never covered this; it is the natural end of the credential lifecycle and the counterpart to the
  # rotation above — rotation replaces a credential, deletion removes it.
  @cap:key-manager @feat:token-issuance @rule:secret-rotation @type:negative @dep:devportal @legacy:ApplicationRegenerateConsumerSecretTestCase
  Scenario Outline: Deleting an application's keys leaves its consumer credential unusable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "delAppPayload"
    And I create an application with payload "delAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "delKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "delKeysPayload"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "delConsumerKey"
    And I extract response field "consumerSecret" and store it as "delSecret"

    # The credential is live before the delete — otherwise the assertion after it proves nothing.
    When I request a client-credentials token using consumer key "delConsumerKey" and secret "delSecret"
    Then The response status code should be 200

    When I delete the keys for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200

    # The same credential no longer mints a token.
    When I request a client-credentials token using consumer key "delConsumerKey" and secret "delSecret"
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
