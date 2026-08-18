@cleanup
Feature: Gateway AI API With Unlimited Tier Disabled

  Ports GeminiAPIUnlimitedTierDisabledTestCase. Runs in the IntegrationV2-UnlimitedTierDisabled block, whose
  overlay sets the single server-GLOBAL key that drives the whole feature —
  "[apim.throttling] enable_unlimited_tier = false", read back through ThrottleProperties.isEnableUnlimitedTier().
  With Unlimited disabled the product stops OFFERING that tier (naming it as a subscription, resource or
  application tier is refused with 400 / 900305) and stops DEFAULTING to it (a tier-less resource falls through to
  the first key of a TreeMap of the tenant's API-level policies, i.e. 10KPerMin for the shipped
  10KPerMin / 20KPerMin / 50KPerMin set).

  The API under test is an AIAPI bound to the SHIPPED "Gemini" 1.1.0 AI service provider — not a custom provider —
  and is imported from THAT provider's own OpenAPI definition, fetched at runtime. That is load-bearing: the
  shipped Gemini 1.0.0 definition stamps "x-throttling-tier: Unlimited" onto every operation while the 1.1.0
  definition carries no WSO2 extensions at all, so only on 1.1.0 do the operation tiers come from the server's
  default — which is the behaviour these scenarios pin. Copying the definition into a fixture would make every
  assertion about it a property of the copy instead.

  Both scenarios build the same Gemini fixture and are co-located because they share this block's overlay and that
  arc; the throttling-default scenario is tagged for its true subject (the tier substitution) rather than for the
  folder. Neither may be moved into a default-lane block, and this block hosts nothing that names Unlimited: the
  shared create_apim_test_app.json fixture subscribes on the Unlimited application tier and would be refused with
  400 here, which is exactly why the flag cannot be co-hosted (see the banner in testng-v2.xml).
  x2 tenant, over an admin actor: the arc spans the admin plane (provider listing + definition), the publisher
  plane (import/deploy/publish) and the consumer plane (application, subscription, api key), which legacy also
  drove as SUPER_TENANT_ADMIN / TENANT_ADMIN.
  Teardown is hook-managed (@cleanup): the imported API and the application are registered on create. The AI
  service provider is built-in, so nothing provider-side is created or swept.

  @cap:gateway @feat:ai-invocation @rule:unlimited-tier-disabled @type:regression @dep:admin @dep:publisher @legacy:GeminiAPIUnlimitedTierDisabledTestCase
  Scenario Outline: A Gemini AI API is created, published and invoked while the Unlimited tier is disabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Resolve the SHIPPED Gemini 1.1.0 provider out of the listing by name+version and pin that it is NOT the
    # deprecated one (Gemini 1.0.0 is — it is the only shipped provider overriding isDeprecated()).
    When I retrieve the AI service providers
    Then The response status code should be 200
    And The AI service provider "Gemini" version "1.1.0" should be listed with deprecated "false" and stored as "geminiProviderId"
    # Fetch the provider's own OpenAPI definition and import the AIAPI from it verbatim.
    When I retrieve the api definition of AI service provider "geminiProviderId"
    Then The response status code should be 200
    And I put the response payload in context as "geminiApiDefinition"
    When I import openapi definition captured as "geminiApiDefinition" with additional properties "artifacts/payloads/ai/gemini_add_props.json" as "geminiApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "geminiApiId"
    Then The response status code should be 200
    And The value of response field "version" should be "1.1.0"
    And The value of response field "subtypeConfiguration.subtype" should be "AIAPI"
    And I extract response field "context" and store it as "geminiContext"
    When I deploy the API with id "geminiApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "geminiApiId"
    Then The response status code should be 200
    And The lifecycle status of API "geminiApiId" should be "Published"
    # An application on a NON-Unlimited application tier — the shared fixture's "Unlimited" is refused (400/900305)
    # while the flag is off, so the tier legacy used (10PerMin) is named explicitly here.
    When I put the following JSON payload in context as "geminiAppPayload"
    """
    {"name":"${UNIQUE:GeminiApiApplication}","throttlingPolicy":"10PerMin","description":"Gemini AI API application","tokenType":"JWT"}
    """
    And I create an application with payload "geminiAppPayload"
    Then The response status code should be 201
    # Subscribe on AIBronze — the AI-quota business plan the API OFFERS (its policies list). Unlimited is not a
    # defined tier here, so it could be neither offered by the API nor named by the subscription.
    When I put the following JSON payload in context as "geminiSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AIBronze"}
    """
    And I subscribe to API "geminiApiId" using application "createdAppId" with payload "geminiSubPayload" as "geminiSubId"
    Then The response status code should be 201
    And The value of response field "throttlingPolicy" should be "AIBronze"
    # The API's only securityScheme is api_key, so the credential is an application API key (validity -1 = never
    # expires, as legacy minted it) — not an OAuth2 access token.
    When I put the following JSON payload in context as "geminiApiKeyPayload"
    """
    {"keyName": "GeminiAIApiKey", "validityPeriod": -1, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "geminiApiKeyPayload"
    Then The response status code should be 200
    And I put JSON payload from file "artifacts/payloads/ai/gemini-payload.json" in context as "geminiPayload"
    # Invoke the provider definition's generateContent resource (/v1beta/models/{model}:generateContent). The
    # WHOLE response body is pinned against the mock's document — legacy compared the body to gemini-response.json
    # verbatim, and a substring check would pass on a truncated or rewritten LLM response.
    When I invoke the API at gateway context "{{geminiContext}}/1.1.0/v1beta/models/gemini-1.5-flash:generateContent" with method "POST" using api key "apiKey" and payload "geminiPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response body should equal the JSON file "artifacts/payloads/ai/gemini-response.json"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The substitution rule itself, on the same fixture: no operation of an API imported from the Gemini 1.1.0
  # definition (which declares no x-throttling-tier) may carry Unlimited while the flag is off. Legacy asserted
  # only "not Unlimited", which any value at all satisfies; the exact fall-through tier is pinned here so a change
  # of substitution rule cannot pass unnoticed. No deploy/publish: the tier is stamped at persist time, so a
  # published API would add nothing to the assertion.
  @cap:admin @feat:throttling-policies @rule:unlimited-tier-disabled @type:regression @dep:publisher @legacy:GeminiAPIUnlimitedTierDisabledTestCase
  Scenario Outline: Every operation of a Gemini AI API falls back to the next available tier when Unlimited is disabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the AI service providers
    Then The response status code should be 200
    And The AI service provider "Gemini" version "1.1.0" should be listed with deprecated "false" and stored as "tierGeminiProviderId"
    When I retrieve the api definition of AI service provider "tierGeminiProviderId"
    Then The response status code should be 200
    And I put the response payload in context as "tierGeminiApiDefinition"
    When I import openapi definition captured as "tierGeminiApiDefinition" with additional properties "artifacts/payloads/ai/gemini_add_props.json" as "tierGeminiApiId"
    Then The response status code should be 201
    Then Every operation of API "tierGeminiApiId" should declare throttling policy "10KPerMin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
