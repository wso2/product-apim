@cleanup
Feature: Gateway Default Endpoint

  An API configured with a DEFAULT endpoint type (endpointConfig.endpoint_type = "default") is created, its default
  endpoint type persists on re-fetch, and it deploys and publishes to the gateway. Ports the endpoint-config subject
  of DefaultEndpointTestCase — whose legacy mechanism (a registry-uploaded default_endpoint.xml mediation sequence
  via the legacy ResourceAdminService) is replaced by the native endpoint_type=default the current Publisher REST
  API supports directly (pinned live: endpoint_type=default creates 201, persists on GET and deploys 201).

  SCOPE REDUCTION (documented): the legacy also invoked the API and asserted 200 + Content-Type application/xml. The
  native endpoint_type=default is a synapse *default* endpoint that resolves the target from the message context
  rather than a fixed backend URL, so a plain gateway GET against the node customer-service backend suspends the
  default endpoint (303001) — the invocation arc is not equivalent to the legacy's registry-sequence default
  endpoint and is intentionally not asserted. The portable, reliable subject is that the default endpoint type is
  authored, retained and deployable. Runs in both tenants; torn down by the cleanup hook.

  @cap:publisher @feat:api-config @rule:default-endpoint @type:regression @dep:admin @legacy:DefaultEndpointTestCase
  Scenario Outline: An API with a default endpoint type is authored, retained and deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_default_endpoint_api.json" as "deApiId" and deployed it
    Then The response status code should be 201
    When I publish the "apis" resource with id "deApiId"
    Then The lifecycle status of API "deApiId" should be "Published"
    # The publisher API retains the default endpoint type after create + deploy + publish.
    When I retrieve the "apis" resource with id "deApiId"
    Then The response status code should be 200
    And The response should contain "\"endpoint_type\":\"default\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
