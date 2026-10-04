@cleanup
Feature: Per-API custom gateway handler

  Cover both supported custom-handler paths in an isolated runtime. The template scenarios install the handler JAR and
  documented conditional velocity_template.xml before boot, matching only the API with the stable name marker; a
  non-matching API is the control. The ConfigServiceAdmin scenarios instead attach the handler to one already-live API
  through the Gateway SOAP management service as the super-tenant management administrator, matching the legacy
  management-service contract. The template behavior and non-matching control run as both the super tenant and
  tenant1.com without changing other test blocks.

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
    And I have set up application with keys, subscribed to API "customHandlerApiId", and obtained access token for "customHandlerSubscriptionId"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{customHandlerContext}}/1.0.0/handler" with method "GET" using access token "generatedAccessToken" and payload "" with request header "CustomAuthorization" set to "CustomAuthKey 123456789" until response status code becomes 200 within 60 seconds
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
    And I have set up application with keys, subscribed to API "runtimeSoapHandlerApiId", and obtained access token for "runtimeSoapHandlerSubscriptionId"
    Then The response status code should be 200

    When I attach the custom gateway handler to API context "{{runtimeSoapHandlerContext}}" through ConfigServiceAdmin
    And I invoke the API once at gateway context "{{runtimeSoapHandlerContext}}/1.0.0/handler" with method "GET" using access token "generatedAccessToken" and payload "" with request header "CustomAuthorization" set to "CustomAuthKey 123456789"
    Then The response status code should be 200
    And The custom handler response body should be exactly "I was at CustomAPIAuthenticationHandler"

    Examples:
      | actor             |
      | admin             |

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
    And I have set up application with keys, subscribed to API "handlerTemplateControlApiId", and obtained access token for "handlerTemplateControlSubscriptionId"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{handlerTemplateControlContext}}/1.0.0/handler" once with method "GET" using access token "generatedAccessToken" and payload ""
    Then The response status code should be 200
    And The custom handler response body should be exactly ""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
