Feature: AI API Chat — invalid AI credential handling
  Verifies that the DevPortal API Chat prepare endpoint rejects an invalid/rejected AI credential with 401
  (carbon-apimgt PR #13920). A published API is required (the _setup_published_apis fixture, listed first in the
  runner). The block overlay configures the key branch (apim.ai.key set) pointing at the node stub, whose AI
  resource rejects the call with HTTP 401; invokeAIService maps that to an "Invalid credentials" error which the
  REST layer surfaces as 401.

  Background:
    Given The system is ready and I have valid devportal access token as "subscriberUser"

  @cap:devportal @feat:api-chat @rule:invalid-key @type:negative @dep:publisher
  Scenario: API Chat prepare with an invalid AI key is rejected with 401
    When I put the following JSON payload in context as "apiChatPrepareBody"
      """
      { "apiChatRequestId": "integration-v2-apichat-req-1" }
      """
    And I send an AI API Chat PREPARE request for API "publishedApiId" with payload "apiChatPrepareBody"
    Then The response status code should be 401
