@setup
Feature: Setup credential-type confusion APIs

  Builds the fixture the credential-type confusion matrix asserts against — two APIs that differ ONLY in which
  application-security schemes they declare, plus one application subscribed to both and the three DISTINCT
  credential kinds the gateway must keep apart:

    * ctcApi<suffix>        — oauth2 + api_key + oauth_basic_auth_api_key_mandatory (accepts an OAuth bearer OR an API key)
    * ctcKeyOnlyApi<suffix> — api_key + oauth_basic_auth_api_key_mandatory (accepts an API key ONLY)

  The three credentials handed off are all REAL, LIVE credentials — that is the whole point of the matrix: the
  existing garbage-string negatives prove only that nonsense is rejected, not that the gateway discriminates
  between credential TYPES. They are:

    * generatedAccessToken — an OAuth2 access token of an application subscribed to both APIs
    * apiKey               — a DevPortal application API key of that same application
    * ctcInternalKey<suffix>       — a publisher internal API key issued FOR ctcApi<suffix> (apis/{id}/generate-key)

  The API key is deliberately issued for the application AFTER both subscriptions so it is valid on both APIs,
  and the OAuth token is minted after the second subscription so the JWT carries both (a JWT access token embeds
  the subscriptions it was issued with).

  Asserts nothing about product behaviour; the sparse status checks are fail-fast gates so a fixture failure
  surfaces here rather than as a "No value found in context" cascade later. Resources are registered for the
  runner's AfterClass sweep (NOT per-scenario cleanup — the matrix scenarios consume this fixture).

  Scenario Outline: Publish the two application-security APIs, subscribe an application and mint all three credential kinds as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    # --- oauth2 + api_key, application security MANDATORY: both an OAuth bearer and an API key are legitimate here.
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ctcApiId<suffix>" and deployed it
    And I retrieve the "apis" resource with id "ctcApiId<suffix>"
    Then The response status code should be 200
    And I put the response payload in context as "ctcApiPayload<suffix>"
    When I update the "apis" resource "ctcApiId<suffix>" and "ctcApiPayload<suffix>" with configuration type "securityScheme" and value:
      """
      ["oauth2", "api_key", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "ctcApiId<suffix>"
    Then The response should contain "api_key"
    And I extract response field "context" and store it as "ctcContext<suffix>"
    When I deploy the API with id "ctcApiId<suffix>"
    Then The response status code should be 201
    And I wait until "apis" "ctcApiId<suffix>" revision is deployed in the gateway
    When I publish the "apis" resource with id "ctcApiId<suffix>"
    Then The lifecycle status of API "ctcApiId<suffix>" should be "Published"

    # --- api_key ONLY, application security MANDATORY: an OAuth bearer and HTTP Basic are both illegitimate here.
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ctcKeyOnlyApiId<suffix>" and deployed it
    And I retrieve the "apis" resource with id "ctcKeyOnlyApiId<suffix>"
    Then The response status code should be 200
    And I put the response payload in context as "ctcKeyOnlyApiPayload<suffix>"
    When I update the "apis" resource "ctcKeyOnlyApiId<suffix>" and "ctcKeyOnlyApiPayload<suffix>" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "ctcKeyOnlyApiId<suffix>"
    Then The response should contain "api_key"
    And I extract response field "context" and store it as "ctcKeyOnlyContext<suffix>"
    When I deploy the API with id "ctcKeyOnlyApiId<suffix>"
    Then The response status code should be 201
    And I wait until "apis" "ctcKeyOnlyApiId<suffix>" revision is deployed in the gateway
    When I publish the "apis" resource with id "ctcKeyOnlyApiId<suffix>"
    Then The lifecycle status of API "ctcKeyOnlyApiId<suffix>" should be "Published"

    # One application subscribed to BOTH APIs; the token is re-minted after the second subscription so it carries
    # both, and the api-key is generated last so it is valid on both.
    When I have set up application with keys, subscribed to API "ctcApiId<suffix>", and obtained access token for "ctcSubId<suffix>"
    Then The response status code should be 200
    When I put the following JSON payload in context as "ctcKeyOnlySubPayload<suffix>"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "ctcKeyOnlyApiId<suffix>" using application "createdAppId" with payload "ctcKeyOnlySubPayload<suffix>" as "ctcKeyOnlySubId<suffix>"
    Then The response status code should be 201
    When I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200

    # A DevPortal application API key (stored as "apiKey").
    When I put the following JSON payload in context as "ctcApiKeyGenPayload<suffix>"
    """
    {"keyName": "CredentialConfusionKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "ctcApiKeyGenPayload<suffix>"
    Then The response status code should be 200
    And I put value "apiKey" in context as "ctcApiKey<suffix>"

    # A publisher internal API key issued FOR ctcApi<suffix> — a third, distinct credential kind (Internal-Key header).
    When I generate an internal API key for API "ctcApiId<suffix>" and store it as "ctcInternalKey<suffix>"
    Then The response status code should be 200
    And I put value "generatedAccessToken" in context as "ctcAccessToken<suffix>"

    Examples:
      | actor             | suffix       |
      | admin             |              |
      | admin@tenant1.com | @tenant1.com |
