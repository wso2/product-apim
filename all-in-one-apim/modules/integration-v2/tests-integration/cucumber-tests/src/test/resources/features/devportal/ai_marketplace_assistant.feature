Feature: AI Marketplace Assistant REST contract
  Validates the DevPortal Marketplace Assistant request validation after the pluggable-implementation refactor
  (carbon-apimgt PR #13920). The refactor runs request validation before the backend is consulted, so a
  malformed payload is rejected with 400 regardless of whether an AI service is configured. The configured
  happy path (chat 201, api-count 200) is covered separately in ai_marketplace_assistant_backend.feature, and
  the unconfigured null-return degradation is covered by the carbon-apimgt unit tests
  (DefaultMarketplaceAssistantServiceImplTest).

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
