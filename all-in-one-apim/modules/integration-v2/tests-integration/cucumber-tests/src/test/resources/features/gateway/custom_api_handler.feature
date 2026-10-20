@cleanup
Feature: Per-API custom gateway handler

  Cover both supported custom-handler paths in an isolated runtime. The template scenarios install the handler JAR and
  documented conditional velocity_template.xml before boot, matching only the API with the stable name marker; a
  non-matching API is the control. The ConfigServiceAdmin scenarios instead attach the handler to one already-live API
  through the Gateway SOAP management service as the super-tenant management administrator, matching the legacy
  management-service contract. Static Synapse routing cases also use ConfigServiceAdmin and run here, where the
  service is explicitly enabled for this isolated block. The template behavior and non-matching control run as both
  the super tenant and tenant1.com without changing other test blocks.

  The block's gateway reads OAuth tokens from the custom `Test-Custom-Header` (`[apim.oauth_config] auth_header`, shared
  with the custom-auth-header feature), so every token-bearing invocation here sends its bearer token in that header.
  The handler itself reads only `CustomAuthorization` and leaves token validation to APIM's authentication handler.
  Applications here are created from create_apim_test_app_with_external_ref.json: the block's overlay declares the required application attribute
  "External Reference Id", which every application create and update must carry.

  @cap:gateway @feat:custom-api-handler @type:regression @dep:publisher @dep:devportal @legacy:AddNewHandlerAndInvokeAPITestCase
  Scenario Outline: A custom handler attached to one API executes for <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_custom_handler_api.json" as "customHandlerApiId" and deployed it
    And the "apis" resource "customHandlerApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "customHandlerApiId"
    Then The lifecycle status of API "customHandlerApiId" should be "Published"
    When I retrieve the "apis" resource with id "customHandlerApiId"
    And I extract response field "context" and store it as "customHandlerContext"
    And I have set up application from payload file "artifacts/payloads/create_apim_test_app_with_external_ref.json" with keys, subscribed to API "customHandlerApiId", and obtained access token for "customHandlerSubscriptionId"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{customHandlerContext}}/1.0.0/handler" with method "GET" using access token "generatedAccessToken" in header "Test-Custom-Header" and payload "" with request header "CustomAuthorization" set to "CustomAuthKey 123456789" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The custom handler response body should be exactly "I was at CustomAPIAuthenticationHandler"

  Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:custom-api-handler @type:regression @dep:publisher @dep:devportal @legacy:AddNewHandlerAndInvokeAPITestCase
  Scenario Outline: ConfigServiceAdmin can attach a handler to one live API for <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_custom_handler_soap_api.json" as "runtimeSoapHandlerApiId" and deployed it
    And the "apis" resource "runtimeSoapHandlerApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "runtimeSoapHandlerApiId"
    Then The lifecycle status of API "runtimeSoapHandlerApiId" should be "Published"
    When I retrieve the "apis" resource with id "runtimeSoapHandlerApiId"
    And I extract response field "context" and store it as "runtimeSoapHandlerContext"
    And I have set up application from payload file "artifacts/payloads/create_apim_test_app_with_external_ref.json" with keys, subscribed to API "runtimeSoapHandlerApiId", and obtained access token for "runtimeSoapHandlerSubscriptionId"
    Then The response status code should be 200

    When I attach the custom gateway handler to API context "{{runtimeSoapHandlerContext}}" through ConfigServiceAdmin
    And I invoke the API once at gateway context "{{runtimeSoapHandlerContext}}/1.0.0/handler" with method "GET" using access token "generatedAccessToken" in header "Test-Custom-Header" and payload "" with request header "CustomAuthorization" set to "CustomAuthKey 123456789"
    Then The response status code should be 200
    And The custom handler response body should be exactly "I was at CustomAPIAuthenticationHandler"

    Examples:
      | actor             |
      | admin             |

  # LocationHeaderTestCase installs an anonymous static Synapse route, then makes a GET with no access token. Run it
  # in both tenant contexts while keeping the route anonymous and preserving the exact Location-header checks.
  @static-synapse-route @cap:gateway @feat:header-transformation @rule:location-header-static-anonymous @type:regression @legacy:LocationHeaderTestCase
  Scenario Outline: An anonymous static Synapse route forwards its absolute Location header without a doubled slash for <domain>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I install the legacy static Location-header Synapse route at context "<staticContext>"
    When I invoke the API at gateway context "<staticContext>/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response header "Location" should end with "/abc/domain"
    And The response header "Location" should not end with "//abc/domain"

    Examples:
      | domain       | actor             | staticContext                 |
      | super-tenant | admin             | /locationheader               |
      | tenant1.com  | admin@tenant1.com | /t/tenant1.com/locationheader  |

  # URLMappingRESTTestCase installs a static Synapse API outside the Publisher API registry, then requests its
  # fixed /test resource with a trailing slash. The static API is removed after each row to avoid leaking config.
  @static-synapse-route @cap:gateway @feat:rest-invocation @rule:static-synapse-trailing-slash @type:regression @legacy:URLMappingRESTTestCase
  Scenario Outline: A static Synapse URL-mapping route accepts a trailing slash in the <domain> context
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I install the legacy static URL-mapping Synapse route at context "<staticContext>"
    When I invoke the API at gateway context "<staticContext>/test/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expectedXmlFragment>"

    Examples:
      | domain       | actor             | staticContext                  | expectedXmlFragment |
      | super-tenant | admin             | /urlmapping/1.0.0              | <symbol>IBM</symbol> |
      | tenant1.com  | admin@tenant1.com | /t/tenant1.com/urlmapping/1.0.0 | <symbol>IBM</symbol> |

  @cap:gateway @feat:custom-api-handler @type:regression @dep:publisher @dep:devportal
  Scenario Outline: The custom-handler template does not attach the handler to a non-matching API for <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_handler_template_control_api.json" as "handlerTemplateControlApiId" and deployed it
    And the "apis" resource "handlerTemplateControlApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "handlerTemplateControlApiId"
    Then The lifecycle status of API "handlerTemplateControlApiId" should be "Published"
    When I retrieve the "apis" resource with id "handlerTemplateControlApiId"
    And I extract response field "context" and store it as "handlerTemplateControlContext"
    And I have set up application from payload file "artifacts/payloads/create_apim_test_app_with_external_ref.json" with keys, subscribed to API "handlerTemplateControlApiId", and obtained access token for "handlerTemplateControlSubscriptionId"
    Then The response status code should be 200

    When I invoke the API once at gateway context "{{handlerTemplateControlContext}}/1.0.0/handler" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Test-Custom-Header" set to "Bearer {{generatedAccessToken}}"
    Then The response status code should be 200
    And The custom handler response body should be exactly ""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
