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

  # Negative: a WSDL import whose context glues the {version} placeholder onto text (not a standalone path
  # segment) is a malformed context — rejected 400 "The API context is malformed" before any API is created.
  # Ports WSDLImportTestCase#testWsdlDefinitionImportWithMalformedContext.
  @cap:publisher @feat:soap-design @rule:wsdl-import @type:negative @legacy:WSDLImportTestCase
  Scenario Outline: Importing a WSDL with a malformed context is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlBadName"
    When I put the following JSON payload in context as "wsdlBadProps"
    """
    {"name":"{{wsdlBadName}}","context":"{{wsdlBadName}}{version}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "wsdlBadProps" and implementation type "SOAP" as "wsdlBadId"
    Then The response status code should be 400
    And The response should contain "The API context is malformed"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Import from a WSDL ARCHIVE (.zip containing the WSDL) — same import endpoint, archive-detected by the file
  # extension — then retrieve the imported API's WSDL definition from the publisher (GET /apis/{id}/wsdl).
  # Ports WSDLImportTestCase#testWsdlDefinitionImport (zip arm) + testGetWsdlDefinitions.
  @cap:publisher @feat:soap-design @rule:wsdl-import @type:regression @legacy:WSDLImportTestCase
  Scenario Outline: An API can be imported from a WSDL archive and its WSDL retrieved as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlZipName"
    And I generate a unique value and store it as "wsdlZipCtx"
    When I put the following JSON payload in context as "wsdlZipProps"
    """
    {"name":"{{wsdlZipName}}","context":"{{wsdlZipCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.zip" with additional properties "wsdlZipProps" and implementation type "SOAP" as "wsdlZipId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "wsdlZipId"
    Then The response status code should be 200
    And The response should contain "{{wsdlZipName}}"
    And The response should contain "SOAP"
    When I retrieve the WSDL definition of API "wsdlZipId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Download the WSDL definition of a DEPLOYED, published API from the DevPortal store
  # (GET /apis/{id}/wsdl?environmentName=). Needs the API deployed to a gateway environment and published so it
  # is visible in the store; the download reads the deployed artifact (no upstream backend required — the
  # gateway is in the same all-in-one JVM). Ports WSDLImportTestCase#testDownloadWsdlDefinitionsFromStore.
  @cap:publisher @feat:soap-design @rule:wsdl-import @type:regression @dep:devportal @legacy:WSDLImportTestCase
  Scenario Outline: A deployed WSDL API's definition downloads from the devportal store as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlDlName"
    And I generate a unique value and store it as "wsdlDlCtx"
    When I put the following JSON payload in context as "wsdlDlProps"
    """
    {"name":"{{wsdlDlName}}","context":"{{wsdlDlCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "wsdlDlProps" and implementation type "SOAP" as "wsdlDlId"
    Then The response status code should be 201
    When I deploy the API with id "wsdlDlId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "wsdlDlId"
    Then The lifecycle status of API "wsdlDlId" should be "Published"
    When I download the WSDL definition of API "wsdlDlId" from the devportal store
    Then The response status code should be 200

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
    # The imported SOAP API's wsdlUrl points at the tenant-scoped registry WSDL path (super vs tenant differ by the
    # /t/<domain> prefix and the registry-encoded provider). Ports SOAPAPIImportExportTestCase#testAPIWSDLUrl.
    And The wsdlUrl of API "soapExpImportedApiId" should be the tenant-scoped registry WSDL path

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
