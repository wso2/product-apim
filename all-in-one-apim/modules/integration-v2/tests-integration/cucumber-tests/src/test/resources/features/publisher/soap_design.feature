@cleanup
Feature: Publisher SOAP API Design

  Publisher-plane SOAP (passthrough) API design: create + deploy a SOAP API and publish it. Asserts only
  publisher-plane outcomes — SOAP invocation is covered by gateway/soap-invocation. Self-contained
  scenario, torn down by the per-scenario cleanup hook.

  @cap:publisher @feat:soap-design @type:smoke @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: Create, deploy and publish a SOAP API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_soap_api.json" as "soapApiId" and deployed it
    And The lifecycle status of API "soapApiId" should be "Created"
    When I publish the "apis" resource with id "soapApiId"
    Then The lifecycle status of API "soapApiId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:soap-design @type:negative @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: A subscriber-role user cannot create an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_soap_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # Import an API from a WSDL file (SOAP pass-through proxy). The HelloService WSDL is uploaded and APIM creates a
  # SOAP-type API. Publisher-plane only (no gateway invocation), so the endpoint URL is stored metadata. Ports
  # WSDLImportTestCase (file import).
  @cap:publisher @feat:soap-design @rule:wsdl-import @type:regression @legacy:WSDLImportTestCase
  Scenario Outline: An API can be imported from a WSDL file as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlApiName"
    And I generate a unique value and store it as "wsdlApiCtx"
    When I put the following JSON payload in context as "wsdlAddProps"
    """
    {"name":"{{wsdlApiName}}","context":"{{wsdlApiCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "wsdlAddProps" and implementation type "SOAP" as "wsdlApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "wsdlApiId"
    Then The response status code should be 200
    And The response should contain "{{wsdlApiName}}"
    And The response should contain "SOAP"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Import a WSDL as SOAP-TO-REST: APIM generates REST resources from the WSDL operations (sayHello). Ports
  # SoapToRestTestCase (create side). Publisher-plane only.
  @cap:publisher @feat:soap-design @rule:soap-to-rest @type:regression @legacy:SoapToRestTestCase
  Scenario Outline: A WSDL imported as SOAP-to-REST generates REST resources as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "s2rApiName"
    And I generate a unique value and store it as "s2rApiCtx"
    When I put the following JSON payload in context as "s2rAddProps"
    """
    {"name":"{{s2rApiName}}","context":"{{s2rApiCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "s2rAddProps" and implementation type "SOAPTOREST" as "s2rApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "s2rApiId"
    Then The response status code should be 200
    And The response should contain "{{s2rApiName}}"
    And The response should contain "sayHello"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # A WSDL-backed SOAP API (unlike one created from endpoint JSON) can be exported and re-imported — SOAP export
  # bundles the WSDL. Runs as admin (import needs apim:api_import_export). Ports SOAPAPIImportExportTestCase.
  @cap:publisher @feat:soap-design @rule:import-export @type:regression @legacy:SOAPAPIImportExportTestCase
  Scenario Outline: A WSDL-backed SOAP API can be exported to an archive and re-imported as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "soapExpApiName"
    And I generate a unique value and store it as "soapExpApiCtx"
    When I put the following JSON payload in context as "soapExpAddProps"
    """
    {"name":"{{soapExpApiName}}","context":"{{soapExpApiCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "soapExpAddProps" and implementation type "SOAP" as "soapExpApiId"
    Then The response status code should be 201
    When I export the API "soapExpApiId" to an archive as "soapExpArchive"
    When I delete the "apis" resource with id "soapExpApiId"
    Then The response status code should be 200
    When I import the exported archive "soapExpArchive" with additional properties "{}" as "soapExpImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{soapExpApiName}}" and store its id as "soapExpImportedApiId"
    Then The response status code should be 200
    And The response should contain "{{soapExpApiName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
