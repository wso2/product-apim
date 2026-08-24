@cleanup
Feature: Gateway Audience Validation

  Gateway-plane enforcement of the JWT audience (aud) claim: an API with no configured audiences accepts any
  valid subscription token; once the API is configured with audiences that do NOT include the token's audience
  (an APIM-issued token's aud is the application's consumer key), the gateway rejects the call with 403 and error
  900914; configuring the API's audiences to include the token's consumer key restores access. Ports
  AudienceValidationTestCase in full — the API half with a production-key token, and the API PRODUCT half (a
  separate gateway artifact with its own audiences field and revision cycle) with a sandbox-key token. Runs in the
  gateway block (backend up) as the tenant admin, in both tenants. Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:security-enforcement @rule:audience @type:regression @dep:publisher @legacy:AudienceValidationTestCase
  Scenario Outline: Audience claim validation gates gateway access as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "audApiId" and deployed it
    When I publish the "apis" resource with id "audApiId"
    Then The lifecycle status of API "audApiId" should be "Published"
    When I retrieve the "apis" resource with id "audApiId"
    And I extract response field "context" and store it as "audApiContext"
    # Subscribe an application and obtain an access token (its aud claim is the application's consumer key).
    When I have set up application with keys, subscribed to API "audApiId", and obtained access token for "audSubId"
    Then The response status code should be 200

    # No audiences configured -> the token is accepted (200).
    When I invoke the API at gateway context "{{audApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Configure audiences that do NOT include the token's audience -> the gateway rejects it with 403 + 900914.
    When I retrieve the "apis" resource with id "audApiId"
    And I put the response payload in context as "audApiFull"
    And I update the "apis" resource "audApiId" and "audApiFull" with configuration type "audiences" and value:
    """
    ["Hello"]
    """
    Then The response status code should be 200
    When I deploy the API with id "audApiId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{audApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900914" message "Access Denied" and description containing "The access token does not allow you to access the requested resource"

    # Configure audiences to include the token's consumer key -> access is restored (200).
    When I retrieve the "apis" resource with id "audApiId"
    And I put the response payload in context as "audApiFull2"
    And I update the "apis" resource "audApiId" and "audApiFull2" with configuration type "audiences" and value:
    """
    ["{{consumerKey}}"]
    """
    Then The response status code should be 200
    When I deploy the API with id "audApiId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{audApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The API PRODUCT half of AudienceValidationTestCase, which the API scenario above does not cover: a product is
  # a SEPARATE gateway artifact with its own audiences field, its own revision/deploy cycle and (here) its own
  # SANDBOX-key token, so none of the three legs is implied by the API result. All three legacy legs are ported —
  # no audiences -> 200; audiences NOT containing the token's audience -> 403 + 900914; audiences CONTAINING the
  # consumer key -> 200. The pass leg keeps the non-matching "Hello" entry alongside the consumer key, matching
  # legacy: that proves audience validation is LIST MEMBERSHIP, not equality with a single configured value.
  # The token is the one issued with the application's SANDBOX keys, so its aud claim is the SANDBOX consumer key
  # (a different credential from the production one the API scenario uses).
  @cap:gateway @feat:security-enforcement @rule:audience @type:regression @dep:publisher @legacy:AudienceValidationTestCase
  Scenario Outline: Audience claim validation gates gateway access to an API product as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "audProdSourceApiId" and deployed it
    When I publish the "apis" resource with id "audProdSourceApiId"
    Then The lifecycle status of API "audProdSourceApiId" should be "Published"

    # Aggregate the API into a product, deploy and publish it.
    When I create an API product "${UNIQUE:AudienceProduct}" with context "${UNIQUE:audienceProductCtx}" from API "audProdSourceApiId" as "audProductId"
    Then The response status code should be 201
    When I deploy the "api-products" resource with id "audProductId"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "audProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "audProductId"
    And I extract response field "context" and store it as "audProductContext"

    # An application with SANDBOX keys, subscribed to the PRODUCT. The token issued with those keys carries the
    # SANDBOX consumer key as its aud claim.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "audProdAppPayload"
    And I create an application with payload "audProdAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "audProdKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "audProdKeysPayload"
    Then The response status code should be 200
    And I extract response field "token.accessToken" and store it as "audProdSandboxToken"
    When I put the following JSON payload in context as "audProdSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "audProductId" using application "createdAppId" with payload "audProdSubPayload" as "audProdSubId"
    Then The response status code should be 201

    # No audiences configured on the product -> the sandbox token is accepted (200).
    When I invoke the API at gateway context "{{audProductContext}}/1.0.0/customers/123/" with method "GET" using access token "audProdSandboxToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Audiences that do NOT include the token's audience -> the gateway rejects it with 403 + 900914.
    When I retrieve the "api-products" resource with id "audProductId"
    And I put the response payload in context as "audProductFull"
    And I update the "api-products" resource "audProductId" and "audProductFull" with configuration type "audiences" and value:
    """
    ["Hello"]
    """
    Then The response status code should be 200
    When I deploy the "api-products" resource with id "audProductId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{audProductContext}}/1.0.0/customers/123/" with method "GET" using access token "audProdSandboxToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900914" message "Access Denied" and description containing "The access token does not allow you to access the requested resource"

    # Audiences CONTAINING the sandbox consumer key (alongside the non-matching entry) -> access is restored (200).
    When I retrieve the "api-products" resource with id "audProductId"
    And I put the response payload in context as "audProductFull2"
    And I update the "api-products" resource "audProductId" and "audProductFull2" with configuration type "audiences" and value:
    """
    ["Hello","{{consumerKey}}"]
    """
    Then The response status code should be 200
    When I deploy the "api-products" resource with id "audProductId"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{audProductContext}}/1.0.0/customers/123/" with method "GET" using access token "audProdSandboxToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
