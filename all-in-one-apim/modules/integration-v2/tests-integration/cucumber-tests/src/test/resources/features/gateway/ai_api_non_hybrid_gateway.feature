@cleanup
Feature: AI API deployment to a non-hybrid gateway

  An AI API keeps its endpoints as named endpoints, outside the endpoint configuration. Clearing that configuration
  therefore leaves it carrying no production endpoint while a named production endpoint remains. The check that
  refuses a deployment when the API carries no endpoint of the stage the gateway serves read the endpoint
  configuration alone, so it refused such an API and it never reached the gateway.

  That check is only evaluated for a gateway serving ONE stage, which is why this block redefines Default as
  production-only (see the tomlExtraOverlayPath on its test block). The AI coverage in
  gateway/ai_api_invocation.feature deploys to a hybrid Default and so stays green either way.

  @cap:gateway @feat:ai-invocation @rule:non-hybrid-deployment @type:regression @dep:admin @dep:publisher
  Scenario: An AI API deploys to a production-only gateway with only a named production endpoint
    Given The system is ready
    And I have valid access tokens as "admin"
    # An AI API carrying its production endpoint inline, in its endpoint configuration
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "epRemovalApiId"
    Then The response status code should be 201
    # A second production endpoint, held as a named endpoint rather than in the endpoint configuration
    When I put the following JSON payload in context as "survivingEndpointPayload"
    """
    {"name": "Surviving Production Endpoint", "deploymentStage": "PRODUCTION", "endpointConfig": {"endpoint_type": "http", "production_endpoints": {"url": "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/no-auth"}}}
    """
    And I add an endpoint to API "epRemovalApiId" with payload "survivingEndpointPayload" as "survivingEndpointId"
    Then The response status code should be 201
    # Make the named endpoint primary, so the gateway has one to route to once the inline one is gone
    When I retrieve the "apis" resource with id "epRemovalApiId"
    And I put the response payload in context as "epRemovalApiPayload"
    And I update the "apis" resource "epRemovalApiId" and "epRemovalApiPayload" with configuration type "primaryProductionEndpointId" and value:
    """
    {{survivingEndpointId}}
    """
    Then The response status code should be 200
    # Drop the endpoint the API was created with. It is inline and has no REST delete of its own, so the endpoint
    # CONFIGURATION is cleared instead — the state the deployment used to be refused for.
    When I put the following JSON payload in context as "emptiedEndpointConfig"
    """
    {"endpoint_type":"http"}
    """
    And I retrieve the "apis" resource with id "epRemovalApiId"
    And I put the response payload in context as "epRemovalApiPayloadWithPrimary"
    And I update the "apis" resource "epRemovalApiId" and "epRemovalApiPayloadWithPrimary" with configuration type "endpointConfig" and value:
    """
    emptiedEndpointConfig
    """
    Then The response status code should be 200
    # The premise, asserted before deploying so that a premise that did not hold is reported as such rather than as a
    # routing failure. BOTH halves matter: nothing of the production stage left in the CONFIGURATION (what the check
    # read — without this the check never refused anything and the scenario would pass unfixed too), and a named
    # production endpoint still primary (what it has to read instead).
    When I retrieve the "apis" resource with id "epRemovalApiId"
    Then The response should not contain "production_endpoints"
    And The value of response field "primaryProductionEndpointId" should be "{{survivingEndpointId}}"
    # It reaches the production-only gateway: deploy, publish, subscribe and invoke. A refused deployment never routes.
    When I deploy the API with id "epRemovalApiId"
    When I publish the "apis" resource with id "epRemovalApiId"
    Then The lifecycle status of API "epRemovalApiId" should be "Published"
    When I retrieve the "apis" resource with id "epRemovalApiId"
    And I extract response field "context" and store it as "epRemovalContext"
    When I have set up application with keys, subscribed to API "epRemovalApiId" with plan "Unlimited", and obtained access token for "epRemovalSubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    When I invoke the API at gateway context "{{epRemovalContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response should contain "chat.completion"
