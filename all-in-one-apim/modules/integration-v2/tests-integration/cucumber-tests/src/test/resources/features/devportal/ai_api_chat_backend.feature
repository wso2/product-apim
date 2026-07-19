Feature: AI API Chat default implementation — backend invocation
  Exercises the out-of-the-box (default) API Chat implementation end-to-end against a canned AI backend
  (carbon-apimgt PR #13920). The API Chat "prepare" stage fetches the target API's OpenAPI definition and calls
  the AI service, so a published API is required (provisioned by the _setup_published_apis fixture listed first
  in the runner). The block overlay points [apim.ai] endpoint at the node stub with an auth token set, so
  DefaultAPIChatAssistantServiceImpl performs a real HTTP call and the DevPortal REST layer maps the response
  (ApiChatResponseDTO) back to the client.

  Background:
    Given The system is ready and I have valid devportal access token as "subscriberUser"

  @cap:devportal @feat:api-chat @rule:default-impl @type:smoke @dep:publisher
  Scenario: API Chat prepare returns the AI service response for a published API
    When I put the following JSON payload in context as "apiChatPrepareBody"
      """
      { "apiChatRequestId": "integration-v2-apichat-req-1" }
      """
    And I send an AI API Chat PREPARE request for API "publishedApiId" with payload "apiChatPrepareBody"
    Then The response status code should be 201
    And The response should contain "Mock API chat prepare result"
