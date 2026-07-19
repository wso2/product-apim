Feature: AI Marketplace Assistant — invalid AI credential handling
  Verifies how the DevPortal Marketplace Assistant endpoints surface an invalid/rejected AI credential
  (carbon-apimgt PR #13920). The block overlay configures the key branch (apim.ai.key set) pointing at the node
  stub, whose AI resources reject the call with HTTP 401. The two endpoints map that rejection differently and
  each is asserted exactly: the chat path returns 401 (invokeAIService "Invalid credentials"), while the
  api-count path surfaces a generic 500.

  Background:
    Given The system is ready and I have valid devportal access token as "subscriberUser"

  @cap:devportal @feat:marketplace-assistant @rule:invalid-key @type:negative
  Scenario: Marketplace Assistant chat with an invalid AI key is rejected with 401
    When I put the following JSON payload in context as "mpChatQuery"
      """
      { "query": "What APIs can I use?", "history": [] }
      """
    And I send an AI Marketplace Assistant chat request with payload "mpChatQuery"
    Then The response status code should be 401

  @cap:devportal @feat:marketplace-assistant @rule:invalid-key @type:negative
  Scenario: Marketplace Assistant API count with an invalid AI key surfaces a 500
    When I request the AI Marketplace Assistant API count
    Then The response status code should be 500
