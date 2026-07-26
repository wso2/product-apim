@cleanup
Feature: DevPortal Custom Application Attributes

  Ports the legacy ApplicationAttributesTestCase: a custom application attribute ("External Reference Id")
  declared server-side via [[apim.devportal.application_attributes]] is stored on the application and, when
  the application invokes a subscribed API, surfaced to the backend in the X-JWT-Assertion backend JWT under
  the http://wso2.org/claims/applicationAttributes claim. The block enables backend JWT generation
  ([apim.jwt] enable=true) so the gateway injects the assertion towards the backend, and the API routes to a
  header-reflecting backend route (/reflect-headers) so the test can read the header the backend received —
  the v2 analogue of the legacy jwt_backend echo. The attribute is declared required=true, so creating an
  application without it is rejected (the negative). Legacy verified both a JWT-type and an OAuth-type
  application; both are covered via the token-type column, and x2 tenant (super + tenant) though legacy was
  super-only. Teardown via the per-scenario cleanup hook removes the application and API.

  @cap:devportal @feat:application-attributes @type:regression @dep:gateway @dep:key-manager @legacy:ApplicationAttributesTestCase
  Scenario Outline: A custom application attribute is stored and surfaced in the backend JWT for a <tokenType> application as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "createAppPayload"
    And I set the field "tokenType" to "<tokenType>" in the payload "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    # Attribute is stored on the application.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "External Reference Id"
    And The response should contain "c1237890"

    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200

    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I put the following JSON payload in context as "createApplicationAccessTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200

    # Invoke through the gateway; the reflecting backend returns the headers it received, including the
    # gateway-injected X-JWT-Assertion carrying the applicationAttributes claim.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain application attribute "External Reference Id" with value "c1237890"

    Examples:
      | tokenType | actor             |
      | JWT       | admin             |
      | OAUTH     | admin             |
      | JWT       | admin@tenant1.com |
      | OAUTH     | admin@tenant1.com |

  # NOTE: a "create without the required attribute is rejected" negative is intentionally NOT included.
  # Legacy (ApplicationAttributesTestCase) never tested it, and probing 4.7.0 live shows the product mishandles
  # the case: the server recognises it ("GlobalThrowableMapper: Bad Request. Required application attribute not
  # provided") but the exception mapper returns 500 (900967 General Error) instead of a clean 400. Per the
  # no-500-enshrinement principle this server bug is documented (increment-2 backlog), not asserted.

  # L3: a custom application attribute can be UPDATED (create → change value → verify). Salvages the delta of
  # the (legacy-disabled, otherwise-duplicate) ApplicationWithCustomAttributesTestCase — the create+enforce+JWT
  # -claim coverage is already above; this adds the update-mutation path.
  @cap:devportal @feat:application-attributes @type:regression @legacy:ApplicationWithCustomAttributesTestCase
  Scenario Outline: A custom application attribute can be updated as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_app_with_attribute.json" in context as "attrCreatePayload"
    And I create an application with payload "attrCreatePayload"
    Then The response status code should be 201
    And I extract response field "name" and store it as "attrAppName"
    When I retrieve the application with id "createdAppId"
    Then The response should contain "c1237890"
    When I put the following JSON payload in context as "attrUpdatePayload"
    """
    {"name":"{{attrAppName}}","throttlingPolicy":"Unlimited","description":"updated attributes","attributes":{"External Reference Id":"c1237890_updated"}}
    """
    And I update the application "createdAppId" with payload "attrUpdatePayload"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "c1237890_updated"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
