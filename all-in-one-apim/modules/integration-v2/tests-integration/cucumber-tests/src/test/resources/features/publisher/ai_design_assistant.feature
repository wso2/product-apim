Feature: AI Design Assistant REST contract
  Validates the Publisher Design Assistant endpoints after the pluggable-implementation refactor
  (carbon-apimgt PR #13920). With no AI credentials configured (the default distribution state), the request
  validation now runs before the backend is consulted, and the default implementation degrades gracefully to an
  empty response instead of erroring.

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

  # NOTE: with the AI service unconfigured the REST resource returns a null Response; JAX-RS renders that as
  # 204 No Content. Confirm this exact code on the first live run and adjust if an interceptor maps it otherwise.
  @cap:publisher @feat:design-assistant @type:regression
  Scenario: Design Assistant generate-payload returns no content when the AI service is not configured
    When I put the following JSON payload in context as "daValidSession"
      """
      { "sessionId": "integration-v2-session" }
      """
    And I send an AI Design Assistant generate-payload request with payload "daValidSession"
    Then The response status code should be 204
