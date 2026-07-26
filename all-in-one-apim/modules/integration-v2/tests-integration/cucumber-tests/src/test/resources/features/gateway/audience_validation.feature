@cleanup
Feature: Gateway Audience Validation

  Gateway-plane enforcement of the JWT audience (aud) claim: an API with no configured audiences accepts any
  valid subscription token; once the API is configured with audiences that do NOT include the token's audience
  (an APIM-issued token's aud is the application's consumer key), the gateway rejects the call with 403 and error
  900914; configuring the API's audiences to include the token's consumer key restores access. Ports the API half
  of AudienceValidationTestCase (commented-out in legacy; the API-product half is a separate, heavier arc not
  ported here). Runs in the gateway block (backend up) as the tenant admin, in both tenants. Teardown via the
  per-scenario cleanup hook.

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
    And The response should contain "900914"

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

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
