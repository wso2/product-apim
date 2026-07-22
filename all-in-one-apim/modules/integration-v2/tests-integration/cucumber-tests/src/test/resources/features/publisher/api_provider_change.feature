@cleanup
Feature: Publisher API Provider Change

  Admin-plane transfer of an API's provider (ownership) via POST /api/am/admin/v4/apis/{apiId}/change-provider:
  the API is re-owned by the target provider while its metadata (name, description, endpoints, resources) is
  retained; a change to a provider in a DIFFERENT tenant is rejected. Ports the core of ChangeApiProviderTestCase
  (the REST-API variant; the SOAP / SOAP-to-REST / GraphQL API-type variants share the same change-provider
  mechanism and are represented by this one). Runs as the tenant admin in both tenants; the target provider is a
  second creator/publisher user provisioned inline. Torn down by the cleanup hook.

  @cap:publisher @feat:api-lifecycle @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Changing an API's provider re-owns it and retains its metadata in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "apiNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    # The admin authors the API (with a distinctive description) and deploys it.
    And I act as "admin<suffix>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "cpApiPayload"
    And I set the field "description" to "Provider change retention marker" in the payload "cpApiPayload"
    And I create an "apis" resource with payload "cpApiPayload" as "cpApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "cpApiName"

    # Change the provider to the second user.
    When I change the provider of API "cpApiId" to "apiNewProvider<suffix>"
    Then The response status code should be 200

    # The API is re-owned by the new provider and its metadata is retained.
    When I retrieve the "apis" resource with id "cpApiId"
    Then The response status code should be 200
    And The provider of API "cpApiId" should match actor "apiNewProvider<suffix>"
    And The response should contain "{{cpApiName}}"
    And The response should contain "Provider change retention marker"
    And The response should contain "nodebackend:3001/jaxrs_basic/services/customers/customerservice"
    And The response should contain "/customers/{id}"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # SOAP-API provider change: a WSDL-imported SOAP API keeps its TYPE-SPECIFIC state (SOAP type + retrievable WSDL
  # definition) as well as its provider after ownership transfer — the type variant exists precisely to catch
  # WSDL-binding loss on change, which the REST variant cannot represent. Ports the SOAP variant of
  # ChangeApiProviderTestCase (ChangeSoapApiProvider). GraphQL and SOAP-to-REST variants are documented scope
  # reductions (their schema/sequence retention is analogous; not separately ported).
  @cap:publisher @feat:api-lifecycle @rule:soap @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Changing a SOAP API's provider retains its WSDL binding in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "soapNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "admin<suffix>"
    And I generate a unique value and store it as "cpSoapName"
    And I generate a unique value and store it as "cpSoapCtx"
    When I put the following JSON payload in context as "cpSoapProps"
    """
    {"name":"{{cpSoapName}}","context":"/{{cpSoapCtx}}","version":"1.0.0"}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "cpSoapProps" and implementation type "SOAP" as "cpSoapApiId"
    Then The response status code should be 201
    And The response should contain "SOAP"

    # Change the provider to the second user.
    When I change the provider of API "cpSoapApiId" to "soapNewProvider<suffix>"
    Then The response status code should be 200

    # Re-owned, still SOAP-typed, and the WSDL definition is still retrievable (the WSDL binding survived).
    When I retrieve the "apis" resource with id "cpSoapApiId"
    Then The response status code should be 200
    And The provider of API "cpSoapApiId" should match actor "soapNewProvider<suffix>"
    And The response should contain "SOAP"
    When I retrieve the WSDL definition of API "cpSoapApiId"
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # A cross-tenant provider change is rejected: changing a super-tenant API's provider to a tenant1.com user (and
  # vice versa) fails with 400 and error 901409 (tenant mismatch). Ports the cross-tenant rejection assertion.
  @cap:publisher @feat:api-lifecycle @type:negative @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: A cross-tenant provider change is rejected in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I act as "admin<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ctpApiId" and deployed it
    # Attempt to change the provider to an admin in the OTHER tenant -> 400 + 901409.
    When I change the provider of API "ctpApiId" to "admin<otherSuffix>"
    Then The response status code should be 400
    And The response should contain "901409"

    Examples:
      | tenant       | suffix       | otherSuffix  |
      | carbon.super |              | @tenant1.com |
      | tenant1.com  | @tenant1.com |              |

  # verify-first NOTE: the tenant of `admin` (no suffix) is carbon.super and `admin@tenant1.com` is tenant1.com;
  # the cross-tenant row targets the opposite tenant's admin as the (invalid) new provider.
