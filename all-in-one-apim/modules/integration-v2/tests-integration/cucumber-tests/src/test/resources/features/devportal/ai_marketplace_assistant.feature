Feature: AI Marketplace Assistant REST contract
  Validates the DevPortal Marketplace Assistant endpoints after the pluggable-implementation refactor
  (carbon-apimgt PR #13920). Request validation now runs before the backend is consulted (malformed payload
  rejected with 400 regardless of AI config), and with no AI service configured the default implementation
  degrades gracefully to an empty response (null Response -> 204) instead of erroring. The configured happy
  path (chat 201, api-count 200) is covered separately in ai_marketplace_assistant_backend.feature.

  Background:
    Given The system is ready and I have valid devportal access token as "subscriberUser"

  @cap:devportal @feat:marketplace-assistant @type:negative
  Scenario: Marketplace Assistant chat rejects an empty query with 400
    When I put the following JSON payload in context as "mpEmptyQuery"
      """
      { "query": "", "history": [] }
      """
    And I send an AI Marketplace Assistant chat request with payload "mpEmptyQuery"
    Then The response status code should be 400

  # With the AI service unconfigured (distribution default: apim.ai key/token empty) the REST resource returns a
  # null Response, which CXF renders as 204 No Content. This exercises the end-to-end null -> 204 mapping that the
  # DefaultMarketplaceAssistantServiceImpl unit test (impl returns null) does not cover.
  @cap:devportal @feat:marketplace-assistant @type:regression
  Scenario: Marketplace Assistant chat returns no content when the AI service is not configured
    When I put the following JSON payload in context as "mpValidQuery"
      """
      { "query": "List the available APIs", "history": [] }
      """
    And I send an AI Marketplace Assistant chat request with payload "mpValidQuery"
    Then The response status code should be 204

  @cap:devportal @feat:marketplace-assistant @type:regression
  Scenario: Marketplace Assistant API count returns no content when the AI service is not configured
    When I request the AI Marketplace Assistant API count
    Then The response status code should be 204
