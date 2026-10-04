@cleanup
Feature: Gateway Default Endpoint Invocation

  Ports the INVOCATION half of DefaultEndpointTestCase. The legacy default endpoint had no fixed backend URL — a
  registry-uploaded mediation sequence (default_endpoint.xml) set the message destination via a synapse "To"
  header, and the native default endpoint then resolved there. The modern equivalent is a request-flow operation
  policy carrying the same synapse "To" header (set_default_endpoint_destination.j2). With that policy attached,
  an API whose endpoint_type=default resolves the destination and the gateway invocation returns 200 — recovering
  the arc that a bare default endpoint suspends (303001, see publisher/default_endpoint.feature). Runs in the
  gateway block (backend + invocation) x2-tenant (super + tenant1) as each tenant's admin; the common policy, API
  and subscription are all tenant-scoped. A second scenario starts with a normal HTTP endpoint, updates that same
  API to endpoint_type=default, and verifies the destination changes at the gateway. Teardown via the per-scenario
  cleanup hook.

  @cap:gateway @feat:rest-invocation @rule:default-endpoint @type:regression @dep:publisher @legacy:DefaultEndpointTestCase
  Scenario Outline: A default-endpoint API resolves its destination via a To-header policy and is invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register the To-header destination policy as a common policy so the API can reference it by name.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/set_default_endpoint_destination.j2" and "artifacts/payloads/policySpecFiles/set_default_endpoint_destination.yaml" as "deDestPolicyId"

    # Create a default-endpoint API whose root operation carries the To-header policy in its request flow.
    And I have created an api from "artifacts/payloads/create_apim_default_endpoint_invoke_api.json" as "deInvApiId" and deployed it
    When I publish the "apis" resource with id "deInvApiId"
    Then The lifecycle status of API "deInvApiId" should be "Published"
    When I retrieve the "apis" resource with id "deInvApiId"
    And I extract response field "context" and store it as "deInvContext"
    When I have set up application with keys, subscribed to API "deInvApiId", and obtained access token for "deInvSubId"
    Then The response status code should be 200

    # With the To header set by the policy, the default endpoint resolves to the backend and the invocation succeeds.
    # The body is pinned to the payload of the destination the policy set (…/customers/123): the whole subject is
    # WHERE the destination-less default endpoint resolved to, and a bare 200 asserts only that it resolved
    # somewhere.
    When I invoke the API at gateway context "{{deInvContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:rest-invocation @rule:default-endpoint @type:regression @dep:publisher @legacy:DefaultEndpointTestCase
  Scenario Outline: Updating an existing API to a default endpoint changes its gateway destination as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_default_endpoint_transition_api.json" as "deTransitionApiId" and deployed it
    And the "apis" resource "deTransitionApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "deTransitionApiId"
    Then The lifecycle status of API "deTransitionApiId" should be "Published"
    When I retrieve the "apis" resource with id "deTransitionApiId"
    And I extract response field "context" and store it as "deTransitionContext"
    When I have set up application with keys, subscribed to API "deTransitionApiId", and obtained access token for "deTransitionSubId"
    Then The response status code should be 200

    # Baseline proves this existing API still routes to its original HTTP endpoint before changing endpoint type.
    When I invoke the API at gateway context "{{deTransitionContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Hello WSO2 from File 1_Sandbox" within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello WSO2 from File 1_Sandbox"
    And The response should not contain "\"name\":\"John\""

    # Change this already-published API to endpoint_type=default, then attach the To-header policy that gives the
    # default endpoint a deterministic destination. Fetch a fresh API representation between updates to avoid
    # overwriting the endpoint change with a stale operations payload.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/set_default_endpoint_destination.j2" and "artifacts/payloads/policySpecFiles/set_default_endpoint_destination.yaml" as "deTransitionPolicyId"
    When I retrieve the "apis" resource with id "deTransitionApiId"
    And I put the response payload in context as "deTransitionApiPayload"
    When I put the following JSON payload in context as "deTransitionEndpoint"
    """
    {"endpoint_type":"default","production_endpoints":{"template_not_supported":false,"config":null,"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/customers/123"},"sandbox_endpoints":{"template_not_supported":false,"config":null,"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/customers/123"}}
    """
    When I update the "apis" resource "deTransitionApiId" and "deTransitionApiPayload" with configuration type "endpointConfig" and value:
      """
      deTransitionEndpoint
      """
    Then The response status code should be 200

    When I retrieve the "apis" resource with id "deTransitionApiId"
    And I put the response payload in context as "deTransitionApiPayload"
    When I update the "apis" resource "deTransitionApiId" and "deTransitionApiPayload" with configuration type "operations" and value:
      """
      [{"verb":"GET","target":"/","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[{"policyName":"set_default_endpoint_destination","policyVersion":"v1","parameters":{}}],"response":[],"fault":[]}}]
    """
    Then The response status code should be 200
    And The value of response field "id" should be "{{deTransitionApiId}}"

    # Deploy the updated revision and gate on gateway artifact convergence before checking the changed behavior.
    When I deploy the API with id "deTransitionApiId"
    Then The response status code should be 201
    And the "apis" resource "deTransitionApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "deTransitionApiId" revision is deployed in the gateway

    # The new response proves the default endpoint now resolves through the To-header policy; the old backend marker
    # must be gone so a stale artifact cannot satisfy this test.
    When I invoke the API at gateway context "{{deTransitionContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "\"name\":\"John\"" within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello WSO2 from File 1_Sandbox"
    # Confirm Publisher read-back after the successful gateway behavior proves both the persisted setting and its effect.
    When I retrieve the "apis" resource with id "deTransitionApiId"
    Then The response status code should be 200
    And The value of response field "endpointConfig.endpoint_type" should be "default"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
