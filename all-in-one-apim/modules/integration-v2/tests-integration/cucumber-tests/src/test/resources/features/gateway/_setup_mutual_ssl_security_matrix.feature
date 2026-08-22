@setup
Feature: Setup mutual-SSL security-matrix APIs

  Builds the fixture the mutual-SSL x application-security matrix asserts against — three APIs that differ ONLY
  in which security schemes they declare mandatory, plus one application subscribed to the two OAuth-bearing ones:

    * mtlsOnlyApi<suffix>   — mutualssl + mutualssl_mandatory (no application security at all)
    * mtlsOptionalApi<suffix> — mutualssl (OPTIONAL) + oauth2 + api_key + oauth_basic_auth_api_key_mandatory
    * mtlsBothApi<suffix>   — mutualssl + mutualssl_mandatory + oauth2 + api_key + oauth_basic_auth_api_key_mandatory

  All three are default-version APIs, so each is reachable at BOTH the versioned (/ctx/1.0.0) and the versionless
  (/ctx) gateway context — the legacy class asserts every case at both, which is what the default version exists
  for. The accepted client certificate (cert_chain_root.cer) is uploaded to each API under a uniquely generated
  alias: the alias is unique per tenant, so a hardcoded one would collide with the sibling
  mutual_ssl_invocation runner uploading the same certificate in the same tenant.

  Asserts nothing about product behaviour; the sparse status checks are fail-fast gates so a fixture failure
  surfaces here rather than as a "No value found in context" cascade later. Resources are registered for the
  runner's AfterClass sweep (NOT per-scenario cleanup — the matrix scenarios consume this fixture).

  Scenario Outline: Publish the three mutual-SSL security-scheme APIs and subscribe an application as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    # --- mutual SSL MANDATORY only: no application security, so the client certificate is the only credential.
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "mtlsOnlyPayload<suffix>"
    And I create an "apis" resource with payload "mtlsOnlyPayload<suffix>" as "mtlsOnlyApiId<suffix>"
    Then The response status code should be 201
    When I generate a unique alphanumeric value and store it as "mtlsOnlyCertAlias<suffix>"
    And I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "{{mtlsOnlyCertAlias<suffix>}}" to API "mtlsOnlyApiId<suffix>" for tier "Unlimited"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "mtlsOnlyApiId<suffix>"
    And I extract response field "context" and store it as "mtlsOnlyContext<suffix>"
    When I deploy the API with id "mtlsOnlyApiId<suffix>"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mtlsOnlyApiId<suffix>"
    Then The lifecycle status of API "mtlsOnlyApiId<suffix>" should be "Published"

    # --- mutual SSL OPTIONAL + application security MANDATORY.
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_optional_oauth_api.json" in context as "mtlsOptionalPayload<suffix>"
    And I create an "apis" resource with payload "mtlsOptionalPayload<suffix>" as "mtlsOptionalApiId<suffix>"
    Then The response status code should be 201
    When I generate a unique alphanumeric value and store it as "mtlsOptionalCertAlias<suffix>"
    And I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "{{mtlsOptionalCertAlias<suffix>}}" to API "mtlsOptionalApiId<suffix>" for tier "Unlimited"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "mtlsOptionalApiId<suffix>"
    And I extract response field "context" and store it as "mtlsOptionalContext<suffix>"
    When I deploy the API with id "mtlsOptionalApiId<suffix>"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mtlsOptionalApiId<suffix>"
    Then The lifecycle status of API "mtlsOptionalApiId<suffix>" should be "Published"

    # --- mutual SSL MANDATORY + application security MANDATORY: both gates must be satisfied.
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_mandatory_oauth_api.json" in context as "mtlsBothPayload<suffix>"
    And I create an "apis" resource with payload "mtlsBothPayload<suffix>" as "mtlsBothApiId<suffix>"
    Then The response status code should be 201
    When I generate a unique alphanumeric value and store it as "mtlsBothCertAlias<suffix>"
    And I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "{{mtlsBothCertAlias<suffix>}}" to API "mtlsBothApiId<suffix>" for tier "Unlimited"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "mtlsBothApiId<suffix>"
    And I extract response field "context" and store it as "mtlsBothContext<suffix>"
    When I deploy the API with id "mtlsBothApiId<suffix>"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mtlsBothApiId<suffix>"
    Then The lifecycle status of API "mtlsBothApiId<suffix>" should be "Published"

    # One application subscribed to BOTH OAuth-bearing APIs; the token is minted AFTER the second subscription so
    # it carries both (a JWT token embeds the subscriptions it was issued with).
    When I have set up application with keys, subscribed to API "mtlsOptionalApiId<suffix>", and obtained access token for "mtlsOptionalSubId<suffix>"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mtlsBothSubPayload<suffix>"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "mtlsBothApiId<suffix>" using application "createdAppId" with payload "mtlsBothSubPayload<suffix>" as "mtlsBothSubId<suffix>"
    Then The response status code should be 201
    When I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200
    And I put value "generatedAccessToken" in context as "mtlsAccessToken<suffix>"

    Examples:
      | actor             | suffix       |
      | admin             |              |
      | admin@tenant1.com | @tenant1.com |
