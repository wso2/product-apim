Feature: AI Marketplace Assistant default implementation — backend invocation
  Exercises the out-of-the-box (default) Marketplace Assistant implementation end-to-end against a canned AI
  backend (carbon-apimgt PR #13920). The block overlay points [apim.ai] endpoint at the node stub
  (http://nodebackend:3001/ai/...) with an auth token set, so DefaultMarketplaceAssistantServiceImpl performs a
  real HTTP call to the AI service and the REST layer maps the response back to the client. This proves the
  default code path — config resolution, factory, HTTP invocation, and response mapping — still works after the
  pluggable-implementation refactor.

  Background:
    Given The system is ready and I have valid devportal access token as "subscriberUser"

  @cap:devportal @feat:marketplace-assistant @rule:default-impl @type:smoke
  Scenario: Marketplace Assistant chat returns the AI service response
    When I put the following JSON payload in context as "mpChatQuery"
      """
      { "query": "What APIs can I use?", "history": [] }
      """
    And I send an AI Marketplace Assistant chat request with payload "mpChatQuery"
    Then The response status code should be 201
    And The response should contain "These are the available APIs matching your query."

  @cap:devportal @feat:marketplace-assistant @rule:default-impl @type:smoke
  Scenario: Marketplace Assistant API count returns the AI service count
    When I request the AI Marketplace Assistant API count
    Then The response status code should be 200
    And The response should contain "42"
