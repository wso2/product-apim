@cleanup
Feature: Gateway AI API Invocation

  AI-API capability: the admin registers a custom (no-auth) AI service provider — a copy of MistralAI whose
  backend authentication is disabled so it can front a mock LLM — then an AIAPI-subtype API is imported from the
  Mistral OpenAPI (backend = the mock LLM chat-completions endpoint on the node backend), published, subscribed
  to, and invoked through the gateway; the LLM response flows back. Also asserts the predefined AI service
  providers are listed. Ports AIAPITestCase (core provider-list + unsecured-AI-API create/publish/invoke).
  NOTE: two invocation variants cover both AI auth schemes — the oauth2 scheme (reusing the standard
  subscribe+token+invoke harness) and the api_key scheme (mint an application API key, invoke with the ApiKey
  header) — the AI data-plane (LLM response through the gateway) is the subject in both. The AIAPI's
  securityScheme lists both oauth2 and api_key. Teardown is fully hook-managed (@cleanup): ResourceCleanup
  deletes the AI service provider AFTER its API, since a provider delete is blocked by a foreign key while any
  AIAPI-subtype API still references it.

  @cap:gateway @feat:ai-invocation @rule:providers @type:smoke @dep:admin @legacy:AIAPITestCase
  Scenario Outline: The predefined AI service providers are listed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the AI service providers
    Then The response status code should be 200
    # The whole SHIPPED set, not just one name: a substring check for "MistralAI" would pass with the other six
    # providers missing, and would say nothing about the summary fields legacy asserts per provider.
    And The AI service providers "MistralAI,OpenAI,AzureOpenAI,AWSBedrock,Anthropic,Gemini,AzureAIFoundry" should each be listed with built-in support, an id, an apiVersion and a description

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Admin provider CRUD — RETRIEVE: a plain GET of a registered provider returns the identity it was registered
  # with. Ports AIAPITestCase#retrieveCustomAiServiceProvider.
  @cap:gateway @feat:ai-invocation @rule:provider-retrieve @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Retrieve a custom AI service provider by id as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    When I retrieve the AI service provider "aiProviderId"
    Then The response status code should be 200
    And The value of response field "id" should be "{{aiProviderId}}"
    And The value of response field "name" should be "TestAIService"
    And The value of response field "apiVersion" should be "1.0.0"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:ai-invocation @rule:invocation @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Invoke a published AI API through the gateway to a mock LLM backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register a no-auth AI service provider (prerequisite for the AIAPI-subtype create); isolated per tenant org
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    # Import the AIAPI-subtype API from the Mistral OpenAPI (backend → the mock LLM)
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    # Read the imported API before deployment; the import status alone does not prove the Publisher
    # representation is available at this sequence point.
    When I retrieve the "apis" resource with id "aiApiId"
    Then The response status code should be 200
    And The value of response field "subtypeConfiguration.subtype" should be "AIAPI"
    When I deploy the API with id "aiApiId"
    And I wait until "apis" "aiApiId" revision is deployed in the gateway
    And The deployment info of revision "revisionId" of "apis" resource "aiApiId" should report acknowledged gateway counts
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"
    When I retrieve the "apis" resource with id "aiApiId"
    And I extract response field "context" and store it as "aiContext"
    When I have set up application with keys, subscribed to API "aiApiId" with plan "Unlimited", and obtained access token for "aiSubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # Invoke the AI API's chat-completions resource; the gateway proxies to the mock LLM and returns its response
    When I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response should contain "chat.completion"
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"
    # Provider + API teardown is hook-managed (@cleanup): the AI provider is swept after its API (FK order)

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:api-config @rule:endpoints @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Manage the endpoints of an AI API through the endpoints resource as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # An AI provider + AIAPI-subtype API is the subject whose endpoints are managed
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    # Legacy performs endpoint lifecycle operations on the already-published AI API, not its draft working copy.
    When I deploy the API with id "aiApiId"
    Then The response status code should be 201
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The response status code should be 200
    And The lifecycle status of API "aiApiId" should be "Published"
    # Add a production endpoint
    When I put the following JSON payload in context as "prodEndpointPayload"
    """
    {"name": "Prod Endpoint", "deploymentStage": "PRODUCTION", "endpointConfig": {"endpoint_type": "http", "production_endpoints": {"url": "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/no-auth"}, "endpoint_security": {"production": {"enabled": true, "type": "apikey", "apiKeyIdentifier": "Authorization", "apiKeyValue": "Bearer 123", "apiKeyIdentifierType": "HEADER"}}}}
    """
    And I add an endpoint to API "aiApiId" with payload "prodEndpointPayload" as "prodEndpointId"
    Then The response status code should be 201
    # Add a sandbox endpoint
    When I put the following JSON payload in context as "sandboxEndpointPayload"
    """
    {"name": "Sandbox Endpoint", "deploymentStage": "SANDBOX", "endpointConfig": {"endpoint_type": "http", "sandbox_endpoints": {"url": "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/no-auth/sandbox"}, "endpoint_security": {"sandbox": {"enabled": true, "type": "apikey", "apiKeyIdentifier": "Authorization", "apiKeyValue": "Bearer 456", "apiKeyIdentifierType": "HEADER"}}}}
    """
    And I add an endpoint to API "aiApiId" with payload "sandboxEndpointPayload" as "sandboxEndpointId"
    Then The response status code should be 201
    # List endpoints — both must be present
    When I retrieve the endpoints of API "aiApiId"
    Then The response status code should be 200
    And The response should contain "Prod Endpoint"
    And The response should contain "Sandbox Endpoint"
    And The endpoint list should include ids "prodEndpointId,sandboxEndpointId" with at least 2 entries
    # Get EACH endpoint by id and verify the retrieval addressed the right one: its id, name and stage. Asserting
    # the stage alone cannot tell the two endpoints apart, and the sandbox endpoint was never fetched by id at all.
    When I retrieve endpoint "prodEndpointId" of API "aiApiId"
    Then The response status code should be 200
    And The value of response field "id" should be "{{prodEndpointId}}"
    And The value of response field "name" should be "Prod Endpoint"
    And The value of response field "deploymentStage" should be "PRODUCTION"
    When I retrieve endpoint "sandboxEndpointId" of API "aiApiId"
    Then The response status code should be 200
    And The value of response field "id" should be "{{sandboxEndpointId}}"
    And The value of response field "name" should be "Sandbox Endpoint"
    And The value of response field "deploymentStage" should be "SANDBOX"
    # Update the production endpoint's URL and verify it persisted
    When I put the following JSON payload in context as "prodEndpointUpdatePayload"
    """
    {"name": "Prod Endpoint", "deploymentStage": "PRODUCTION", "endpointConfig": {"endpoint_type": "http", "production_endpoints": {"url": "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/no-auth-updated"}, "endpoint_security": {"production": {"enabled": true, "type": "apikey", "apiKeyIdentifier": "Authorization", "apiKeyValue": "Bearer 456", "apiKeyIdentifierType": "HEADER"}}}}
    """
    And I update endpoint "prodEndpointId" of API "aiApiId" with payload "prodEndpointUpdatePayload"
    Then The response status code should be 200
    When I retrieve endpoint "prodEndpointId" of API "aiApiId"
    Then The response status code should be 200
    And The response should contain "no-auth-updated"
    And The value of response field "endpointConfig.production_endpoints.url" should be "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/no-auth-updated"
    # Delete the sandbox endpoint — the production endpoint survives, the sandbox id is gone from the list
    When I delete endpoint "sandboxEndpointId" of API "aiApiId"
    Then The response status code should be 200
    # The legacy delete test independently GETs the production endpoint after removing sandbox; the list check
    # below alone would not prove the surviving endpoint remains retrievable by its own id.
    When I retrieve endpoint "prodEndpointId" of API "aiApiId"
    Then The response status code should be 200
    And The value of response field "id" should be "{{prodEndpointId}}"
    And The value of response field "name" should be "Prod Endpoint"
    When I retrieve the endpoints of API "aiApiId"
    Then The response status code should be 200
    And The response should contain "Prod Endpoint"
    And The response should not contain "{{sandboxEndpointId}}"
    And The endpoint list should include ids "prodEndpointId" with at least 1 entries

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:ai-invocation @rule:invocation @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Reuse one application API key across an unsecured and a newly subscribed secured AI API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Mirror the legacy dependency chain: mint the application's API key while subscribed only to the unsecured
    # API, then add the secured API subscription and reuse that same key. This catches stale subscribed-API claims
    # as well as proving that the updated provider configuration is used by the second API.
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    # Import the AIAPI-subtype API (its securityScheme includes api_key alongside oauth2)
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "aiApiId"
    Then The response status code should be 200
    And The value of response field "subtypeConfiguration.subtype" should be "AIAPI"
    When I deploy the API with id "aiApiId"
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"
    When I retrieve the "apis" resource with id "aiApiId"
    And I extract response field "context" and store it as "aiContext"
    # Set up an application subscribed to the AI API, then mint an API key for that application
    When I have set up application with keys, subscribed to API "aiApiId" with plan "Unlimited", and obtained access token for "aiSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiKeyGenerationPayload"
    """
    {"keyName": "AITestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "apiKeyGenerationPayload"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # First API-key invocation: unsecured backend.
    When I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using api key "apiKey" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response should contain "chat.completion"
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"

    # Legacy AIAPITestCase also locally signs a JWT-format APIM API key. Keep that credential mode separate from
    # the opaque DevPortal key above; mint it while only the unsecured API is subscribed, then reuse the same token
    # after the secured API subscription is added below.
    When I generate locally signed JWT API key for application id "createdAppId" as "jwtAiApiKey"
    And I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using api key "jwtAiApiKey" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"

    When I retrieve the AI service provider "aiProviderId"
    Then The response status code should be 200
    And The AI service provider configurations should match the file "artifacts/payloads/ai/ai-service-provider-config-no-auth.json"
    When I update the AI service provider "aiProviderId" named "TestAIService" version "1.0.0" to config "artifacts/payloads/ai/ai-service-provider-config-with-auth.json" with definition "artifacts/payloads/ai/mistral-def.json" and description "Updated AI service provider config"
    Then The response status code should be 200
    When I retrieve the AI service provider "aiProviderId"
    Then The response status code should be 200
    And The response should contain "Updated AI service provider config"
    And The AI service provider configurations should match the file "artifacts/payloads/ai/ai-service-provider-config-with-auth.json"

    # Create the secured API after the provider update. Its backend returns 401 unless the updated provider
    # credential reaches it. Add a second subscription to the same application and reuse apiKey unchanged.
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_api_key_add_props.json" as "securedAiApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "securedAiApiId"
    Then The response status code should be 200
    And The value of response field "subtypeConfiguration.subtype" should be "AIAPI"
    When I deploy the API with id "securedAiApiId"
    Then The response status code should be 201
    And the "apis" resource "securedAiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "securedAiApiId"
    Then The lifecycle status of API "securedAiApiId" should be "Published"
    When I retrieve the "apis" resource with id "securedAiApiId"
    And I extract response field "context" and store it as "securedAiContext"
    When I put the following JSON payload in context as "securedAiSubscriptionPayload"
    """
    {"applicationId": "{{createdAppId}}", "apiId": "{{securedAiApiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "securedAiApiId" using application "createdAppId" with payload "securedAiSubscriptionPayload" as "securedAiSubId"
    Then The response status code should be 201
    And I invoke the API at gateway context "{{securedAiContext}}/1.0.0/v1/chat/completions" with method "POST" using api key "apiKey" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response should contain "chat.completion"
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"
    # Same signed JWT key as above, minted before this second subscription; exact body + 200 proves the key remains
    # valid for the updated provider and newly subscribed secured API without regenerating the key.
    When I invoke the API at gateway context "{{securedAiContext}}/1.0.0/v1/chat/completions" with method "POST" using api key "jwtAiApiKey" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:ai-invocation @rule:invocation @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Invoke a secured AI API through the gateway using an application API key as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # This consumer-ApiKey flow is distinct from the no-auth API-key scenario above: the API's backend endpoint
    # also requires the configured Bearer credential, and the mock returns 401 unless APIM supplies it.
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-with-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "securedKeyAiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_api_key_add_props.json" as "securedKeyAiApiId"
    Then The response status code should be 201
    When I deploy the API with id "securedKeyAiApiId"
    Then The response status code should be 201
    And the "apis" resource "securedKeyAiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "securedKeyAiApiId"
    Then The lifecycle status of API "securedKeyAiApiId" should be "Published"
    When I retrieve the "apis" resource with id "securedKeyAiApiId"
    And I extract response field "context" and store it as "securedKeyAiContext"
    When I have set up application with keys, subscribed to API "securedKeyAiApiId" with plan "Unlimited", and obtained access token for "securedKeyAiSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "securedKeyApiKeyPayload"
    """
    {"keyName": "SecuredAIAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "securedKeyApiKeyPayload"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "securedKeyMistralPayload"
    When I invoke the API at gateway context "{{securedKeyAiContext}}/1.0.0/v1/chat/completions" with method "POST" using api key "apiKey" and payload "securedKeyMistralPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "chat.completion"
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:ai-invocation @rule:invocation @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Invoke an AI API whose provider authenticates to a secured backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register an AUTH-ENABLED AI service provider — its authenticationConfiguration injects the API's configured
    # backend credential into the Authorization header on the backend leg
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-with-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    # Import the secured AIAPI — its endpoint_security carries the credential "Bearer 123" and its backend points
    # at the /with-auth mock LLM route, which returns 401 unless that exact Authorization header is injected
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "aiApiId"
    Then The response status code should be 200
    And The value of response field "subtypeConfiguration.subtype" should be "AIAPI"
    When I deploy the API with id "aiApiId"
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"
    When I retrieve the "apis" resource with id "aiApiId"
    And I extract response field "context" and store it as "aiContext"
    When I have set up application with keys, subscribed to API "aiApiId" with plan "Unlimited", and obtained access token for "aiSubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # A 200 here PROVES the gateway injected the backend credential — the /with-auth route rejects with 401 otherwise
    When I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response should contain "chat.completion"
    And The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @rule:ai-token-quota @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Enforce an AI token quota — the gateway returns 429 once the token count is exceeded as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # An AI-token-quota subscription policy: 300 total tokens/min. The mock LLM reports 358 total tokens per
    # response, so the accumulated token count exceeds the quota within the first couple of calls → 429.
    When I create a subscription throttling policy "aiTokenQuota" allowing 300 total tokens per minute
    Then The response status code should be 201
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    # An app can only subscribe on a tier the API OFFERS — add the AI-token-quota tier to the API's business plans
    When I retrieve the "apis" resource with id "aiApiId"
    And I put the response payload in context as "aiApiPayload"
    When I update the "apis" resource "aiApiId" and "aiApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I deploy the API with id "aiApiId"
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"
    When I retrieve the "apis" resource with id "aiApiId"
    And I extract response field "context" and store it as "aiContext"
    # Subscribe the application with the AI-token-quota policy (resolved from context), not Unlimited
    When I have set up application with keys, subscribed to API "aiApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "aiSubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # Invoke repeatedly; the accumulated token usage trips the quota and the gateway throttles with 429
    When I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:failover @rule:model-round-robin @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Route an AI API across models with a weighted round-robin policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" definition "artifacts/payloads/ai/mistral-def.json" and models "mistral-small-latest,mistral-medium-latest,mistral-large-latest" as "aiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    # Legacy configures 80/20 across two distinct endpoint IDs and accepts either configured response for one
    # invocation. Keep that probability/configuration contract (do not turn it into a deterministic 100/0 test).
    When I put the following JSON payload in context as "roundRobinEndpointPayload"
    """
    {"name":"RoundRobin Endpoint","deploymentStage":"PRODUCTION","endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/no-auth"}}}
    """
    And I add an endpoint to API "aiApiId" with payload "roundRobinEndpointPayload" as "roundRobinEndpointId"
    Then The response status code should be 201
    When I deploy the API with id "aiApiId"
    Then The response status code should be 201
    And The deployment info of revision "revisionId" of "apis" resource "aiApiId" should report acknowledged gateway counts
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"

    # Match the legacy update-after-publish flow: mutate the live API's operation policy, create a new revision,
    # and deploy that revision before invocation. Configuring the policy only before first deployment misses it.
    When I put the following JSON payload in context as "roundRobinConfig"
    """
    {"production":[{"vendor":"","model":"mistral-medium-latest","endpointId":"{{roundRobinEndpointId}}","endpointName":"RoundRobin Endpoint","weight":80},{"vendor":"","model":"mistral-large-latest","endpointId":"default_production_endpoint","endpointName":"Default Production Endpoint","weight":20}],"sandbox":[],"suspendDuration":"5"}
    """
    And I apply the AI mediation policy "modelWeightedRoundRobin" with parameter "weightedRoundRobinConfigs" value "roundRobinConfig" to API "aiApiId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "roundRobinRevisionPayload"
    """
    {"description":"weighted round-robin policy revision"}
    """
    And I make a request to create a revision for "apis" resource "aiApiId" with payload "roundRobinRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "roundRobinRevisionId"
    When I deploy revision "roundRobinRevisionId" of "apis" resource "aiApiId"
    Then The response status code should be 201
    And I wait until "apis" "aiApiId" revision is deployed in the gateway
    And The deployment info of revision "roundRobinRevisionId" of "apis" resource "aiApiId" should report acknowledged gateway counts
    Then The lifecycle status of API "aiApiId" should be "Published"
    When I retrieve the "apis" resource with id "aiApiId"
    And I extract response field "context" and store it as "aiContext"
    When I have set up application with keys, subscribed to API "aiApiId" with plan "Unlimited", and obtained access token for "aiSubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # The complete response is pinned; only the selected model varies between the two configured weighted choices.
    When I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json" with model set to one of "mistral-medium-latest,mistral-large-latest"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Failover: the target model's endpoint fails (429) and the gateway falls back to the fallback model's endpoint.
  # This row runs on a FRESH API so failover is the sole variable; the copied-version arc legacy used is a separate
  # scenario below (a copy that could not carry failover config would pass this row).
  @cap:gateway @feat:failover @rule:model-failover @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Fail an AI API over to a fallback model when the target model endpoint fails as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    # Add a FAILOVER-TARGET endpoint that returns 429 (the /failover-target mock route)
    When I put the following JSON payload in context as "failoverEndpointPayload"
    """
    {"name": "Failover Endpoint", "deploymentStage": "PRODUCTION", "endpointConfig": {"endpoint_type": "http", "production_endpoints": {"url": "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/failover-target"}}}
    """
    And I add an endpoint to API "aiApiId" with payload "failoverEndpointPayload" as "failoverEndpointId"
    Then The response status code should be 201
    # Make the failover endpoint the PRIMARY production endpoint so the gateway hits it first (429) → failover
    When I set the primary production endpoint of API "aiApiId" to "failoverEndpointId"
    Then The response status code should be 200
    # modelFailover: try mistral-small-latest on the failover endpoint (429) → fall back to mistral-large-latest on
    # the default (echoing) endpoint. A large-model response therefore PROVES failover triggered.
    When I put the following JSON payload in context as "failoverConfig"
    """
    {"production":{"targetModel":{"model":"mistral-small-latest","endpointId":"{{failoverEndpointId}}","endpointName":"Failover Endpoint"},"fallbackModels":[{"model":"mistral-large-latest","endpointId":"default_production_endpoint","endpointName":"Default Production Endpoint"}]},"sandbox":{"targetModel":{},"fallbackModels":[]},"requestTimeout":"120","suspendDuration":"0"}
    """
    And I apply the AI mediation policy "modelFailover" with parameter "failoverConfigs" value "failoverConfig" to API "aiApiId"
    Then The response status code should be 200
    When I deploy the API with id "aiApiId"
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"
    When I retrieve the "apis" resource with id "aiApiId"
    And I extract response field "context" and store it as "aiContext"
    When I have set up application with keys, subscribed to API "aiApiId" with plan "Unlimited", and obtained access token for "aiSubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # Pin the complete response shape/content as well as the fallback model; a stray diagnostic body containing
    # the model name must not satisfy this failover assertion.
    When I invoke the API at gateway context "{{aiContext}}/1.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json" with model set to one of "mistral-large-latest"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Copy + failover: the same failover arc, but on a NEW VERSION created by copying the AI API (the combination
  # legacy exercised). What this adds over the fresh-API row above is that the copy is still a real AIAPI bound to
  # the provider, and that an endpoint added to the copy plus a modelFailover policy applied to the copy are honoured
  # at the gateway on the new version's route. Ports AIAPITestCase#testCreateApiVersionWithFailover.
  @cap:gateway @feat:failover @rule:model-failover @type:regression @dep:admin @dep:publisher @legacy:AIAPITestCase
  Scenario Outline: Fail a COPIED new version of an AI API over to a fallback model as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" and definition "artifacts/payloads/ai/mistral-def.json" as "aiProviderId"
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/ai/mistral-def.json" with additional properties "artifacts/payloads/ai/mistral_no_auth_add_props.json" as "aiApiId"
    Then The response status code should be 201
    # The legacy copy-failover arc starts from a published API. Explicitly establish that source state before
    # copying so a draft-only source cannot satisfy the scenario.
    When I deploy the API with id "aiApiId"
    Then The response status code should be 201
    And the "apis" resource "aiApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiApiId"
    Then The lifecycle status of API "aiApiId" should be "Published"
    # Copy the AI API to a new version — the failover config is then built on the COPY, not the original
    When I create a new version "2.0.0" of "apis" resource "aiApiId" with default version "false" as "aiCopyApiId"
    When I retrieve the "apis" resource with id "aiCopyApiId"
    Then The response status code should be 200
    And The value of response field "version" should be "2.0.0"
    # The copy must still be an AIAPI bound to the same provider — otherwise the AI mediation policies below would
    # be applied to a plain HTTP API and the failover assertion would be meaningless.
    And The value of response field "subtypeConfiguration.subtype" should be "AIAPI"
    # The provider binding is carried too. It is asserted on the BODY rather than by field path because the product
    # returns subtypeConfiguration.configuration as a JSON *string*, not the object the create/import request sent —
    # on the copy AND on the original alike (verified live: PathNotFoundException, "found 'java.lang.String'" at
    # $['subtypeConfiguration']['configuration']). "TestAIService" is the LLM provider name and appears nowhere else
    # in an API DTO, so this cannot match incidentally.
    And The response should contain "TestAIService"
    And I extract response field "context" and store it as "aiCopyContext"
    # Add a FAILOVER-TARGET endpoint (the /failover-target mock route answers 429) to the NEW VERSION
    When I put the following JSON payload in context as "copyFailoverEndpointPayload"
    """
    {"name": "Failover Endpoint", "deploymentStage": "PRODUCTION", "endpointConfig": {"endpoint_type": "http", "production_endpoints": {"url": "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/failover-target"}}}
    """
    And I add an endpoint to API "aiCopyApiId" with payload "copyFailoverEndpointPayload" as "copyFailoverEndpointId"
    Then The response status code should be 201
    When I set the primary production endpoint of API "aiCopyApiId" to "copyFailoverEndpointId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "copyFailoverConfig"
    """
    {"production":{"targetModel":{"model":"mistral-small-latest","endpointId":"{{copyFailoverEndpointId}}","endpointName":"Failover Endpoint"},"fallbackModels":[{"model":"mistral-large-latest","endpointId":"default_production_endpoint","endpointName":"Default Production Endpoint"}]},"sandbox":{"targetModel":{},"fallbackModels":[]},"requestTimeout":"120","suspendDuration":"0"}
    """
    And I apply the AI mediation policy "modelFailover" with parameter "failoverConfigs" value "copyFailoverConfig" to API "aiCopyApiId"
    Then The response status code should be 200
    When I deploy the API with id "aiCopyApiId"
    Then The response status code should be 201
    And the "apis" resource "aiCopyApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aiCopyApiId"
    Then The lifecycle status of API "aiCopyApiId" should be "Published"
    When I have set up application with keys, subscribed to API "aiCopyApiId" with plan "Unlimited", and obtained access token for "aiCopySubId"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/mistral-payload.json" in context as "mistralPayload"
    # Invoked on the NEW VERSION's route (/2.0.0/), pin the complete fallback response, not only a matching
    # model substring.
    When I invoke the API at gateway context "{{aiCopyContext}}/2.0.0/v1/chat/completions" with method "POST" using access token "generatedAccessToken" and payload "mistralPayload" until response status code becomes 200 within 60 seconds
    Then The response body should equal the JSON file "artifacts/payloads/ai/mistral-small-response.json" with model set to one of "mistral-large-latest"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Provider models: register a provider with an explicit model list, then retrieve it via the publisher
  # models endpoint and assert all three models are present. Ports AIAPITestCase#testGetServiceProviderModels.
  @cap:gateway @feat:ai-invocation @rule:provider-models @type:regression @dep:admin @legacy:AIAPITestCase
  Scenario Outline: Retrieve an AI service provider's registered model list as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an AI service provider "TestAIService" version "1.0.0" with config "artifacts/payloads/ai/ai-service-provider-config-no-auth.json" definition "artifacts/payloads/ai/mistral-def.json" and models "mistral-small-latest,mistral-medium-latest,mistral-large-latest" as "aiProviderId"
    Then The response status code should be 201
    When I retrieve the models of AI service provider "aiProviderId"
    Then The response status code should be 200
    # The three models the create step REGISTERED via modelProviders come back, exactly -- a round-trip on
    # scenario-owned state, so the flattened count is pinned too (a second vendor group would exceed it).
    And The response array field "[*].models[*]" should have exactly 3 entries
    And The response field "[*].models[*]" should be exactly the list "mistral-small-latest,mistral-medium-latest,mistral-large-latest"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
