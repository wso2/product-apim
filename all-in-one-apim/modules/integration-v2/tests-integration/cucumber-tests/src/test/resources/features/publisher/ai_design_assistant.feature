Feature: AI Design Assistant REST contract
  Validates the Publisher Design Assistant request validation after the pluggable-implementation refactor
  (carbon-apimgt PR #13920). The refactor runs request validation before the backend is consulted, so a
  malformed payload is rejected with 400 regardless of whether an AI service is configured. The configured
  happy path (generate 200, chat 200) is covered separately in ai_design_assistant_backend.feature.

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
