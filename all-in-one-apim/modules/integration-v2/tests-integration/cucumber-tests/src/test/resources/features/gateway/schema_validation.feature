@cleanup
Feature: Gateway Schema Validation

  Gateway-plane enforcement of an API's OpenAPI schema. An API is imported from a petstore OpenAPI with
  enableSchemaValidation=true, deployed and published; the gateway then validates each request against the
  operation's request schema (body + required headers/params) and each backend response against the response
  schema — rejecting schema-invalid requests (400) and responses (500) while letting valid ones through (200).
  The schemas must come from an imported OpenAPI (a create-from-payload API carries only operation targets, not
  the body schemas), and the backend returns schema-shaped bodies that branch on isAvailable so one resource
  drives both response-validation outcomes. Runs in both the super tenant and tenant1.com as the tenant admin
  (the flow spans import/publish + subscribe + invoke). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:schema-validation @type:regression @dep:publisher @legacy:SchemaValidationTestCase
  Scenario Outline: The gateway validates requests and responses against the API schema as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/schema_validation_petstore.json" with additional properties "artifacts/payloads/schema_validation_additional_properties.json" as "svApiId"
    Then The response status code should be 201
    When I deploy the API with id "svApiId"
    When I publish the "apis" resource with id "svApiId"
    Then The lifecycle status of API "svApiId" should be "Published"
    When I retrieve the "apis" resource with id "svApiId"
    And I extract response field "context" and store it as "svContext"
    When I have set up application with keys, subscribed to API "svApiId", and obtained access token for "svSubId"
    Then The response status code should be 200

    # 1. Invalid request body (missing the required "name") → the gateway's request schema validation fails.
    When I put the following JSON payload in context as "invalidPetBody"
      """
      {"category":"dog","status":"available"}
      """
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pets" with method "POST" using access token "generatedAccessToken" and payload "invalidPetBody" until response status code becomes 400 within 60 seconds
    Then The response should contain "Schema validation failed in the Request"

    # 2. Missing the required X-Request-ID header → request schema validation fails.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pets" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 400 within 60 seconds
    Then The response should contain "Schema validation failed in the Request"

    # 3. Required header present (case-insensitive) → request passes and the valid backend array passes response
    # validation → 200.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pets" with method "GET" using access token "generatedAccessToken" and payload "" with request header "x-request-id" set to "787878" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # 4. Backend returns a body missing the required "id" → the gateway's RESPONSE schema validation fails.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pets/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 500 within 60 seconds
    Then The response should contain "Schema validation failed in the Response:"

    # 5. Valid request body → 200.
    When I put the following JSON payload in context as "validPetBody"
      """
      {"id":8999898,"name":"max","tag":"terrier"}
      """
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pets" with method "POST" using access token "generatedAccessToken" and payload "validPetBody" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # 6. Backend returns a schema-valid Pet (isAvailable branch) → response validation passes → 200.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pets/123?isAvailable=false" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # 7. Unsecured resource (x-auth-type None) invoked WITHOUT the required "status" query → request validation
    # fails even without a token.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/pet/findByStatus" with method "GET" without authentication until response status code becomes 400 within 60 seconds
    Then The response should contain "Schema validation failed in the Request"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
