Feature: AI Design Assistant REST contract
  Validates the Publisher Design Assistant endpoints after the pluggable-implementation refactor
  (carbon-apimgt PR #13920). Request validation now runs before the backend is consulted (malformed payload
  rejected with 400 regardless of AI config), and with no AI service configured the default implementation
  degrades gracefully to an empty response (null Response -> 204) instead of erroring. The configured happy
  path (generate 200, chat 200) is covered separately in ai_design_assistant_backend.feature.

  Background:
    Given The system is ready and I have valid publisher access tokens as "publisherUser"

  @cap:publisher @feat:design-assistant @type:negative
  Scenario: Design Assistant generate-payload rejects an empty sessionId with 400
    When I put the following JSON payload in context as "daEmptySession"
      """
      { "sessionId": "" }
      """
    And I send an AI Design Assistant generate-payload request with payload "daEmptySession"
    Then The response status code should be 400

  @cap:publisher @feat:design-assistant @type:negative
  Scenario: Design Assistant chat rejects an empty sessionId with 400
    When I put the following JSON payload in context as "daChatEmptySession"
      """
      { "sessionId": "", "text": "Design an API for a pet store" }
      """
    And I send an AI Design Assistant chat request with payload "daChatEmptySession"
    Then The response status code should be 400

  # With the AI service unconfigured (distribution default: apim.ai key/token empty) the REST resource returns a
  # null Response, which CXF renders as 204 No Content. This exercises the end-to-end null -> 204 mapping that the
  # DefaultDesignAssistantServiceImpl unit test (impl returns null) does not cover.
  @cap:publisher @feat:design-assistant @type:regression
  Scenario: Design Assistant generate-payload returns no content when the AI service is not configured
    When I put the following JSON payload in context as "daValidSession"
      """
      { "sessionId": "integration-v2-session" }
      """
    And I send an AI Design Assistant generate-payload request with payload "daValidSession"
    Then The response status code should be 204
