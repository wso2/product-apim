@cleanup
Feature: Gateway Default Endpoint Invocation

  Ports the INVOCATION half of DefaultEndpointTestCase. The legacy default endpoint had no fixed backend URL — a
  registry-uploaded mediation sequence (default_endpoint.xml) set the message destination via a synapse "To"
  header, and the native default endpoint then resolved there. The modern equivalent is a request-flow operation
  policy carrying the same synapse "To" header (set_default_endpoint_destination.j2). With that policy attached,
  an API whose endpoint_type=default resolves the destination and the gateway invocation returns 200 — recovering
  the arc that a bare default endpoint suspends (303001, see publisher/default_endpoint.feature). Runs in the
  gateway block (backend + invocation) x2-tenant (super + tenant1) as each tenant's admin; the common policy, API
  and subscription are all tenant-scoped. Teardown via the per-scenario cleanup hook.

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
    When I invoke the API at gateway context "{{deInvContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
