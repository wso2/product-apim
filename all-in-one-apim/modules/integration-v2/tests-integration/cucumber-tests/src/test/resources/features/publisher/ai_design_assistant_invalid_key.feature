Feature: AI Design Assistant — invalid AI credential handling
  Verifies that the Publisher Design Assistant endpoints reject an invalid/rejected AI credential with 401
  (carbon-apimgt PR #13920). The block overlay configures the key branch (apim.ai.key set) pointing at the node
  stub, whose AI resources reject the call with HTTP 401; invokeAIService maps that to an "Invalid credentials"
  error which the REST layer surfaces as 401.

  Background:
    Given The system is ready and I have valid publisher access tokens as "publisherUser"

  @cap:publisher @feat:design-assistant @rule:invalid-key @type:negative
  Scenario: Design Assistant generate-payload with an invalid AI key is rejected with 401
    When I put the following JSON payload in context as "daGenSession"
      """
      { "sessionId": "integration-v2-session" }
      """
    And I send an AI Design Assistant generate-payload request with payload "daGenSession"
    Then The response status code should be 401

  @cap:publisher @feat:design-assistant @rule:invalid-key @type:negative
  Scenario: Design Assistant chat with an invalid AI key is rejected with 401
    When I put the following JSON payload in context as "daChatSession"
      """
      { "sessionId": "integration-v2-session", "text": "Design an API for a pet store" }
      """
    And I send an AI Design Assistant chat request with payload "daChatSession"
    Then The response status code should be 401
