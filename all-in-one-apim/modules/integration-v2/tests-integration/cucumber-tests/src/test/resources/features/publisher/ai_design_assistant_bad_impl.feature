Feature: AI Design Assistant pluggable implementation — misconfiguration
  Proves the custom-implementation config path end-to-end (carbon-apimgt PR #13920): the api-manager.xml
  <DesignAssistantImplementation> element (rendered from [apim.ai] design_assistant_impl) is read by
  APIManagerConfiguration, resolved reflectively by DesignAssistantServiceFactory, and a configured class that
  cannot be instantiated surfaces as a 500 at the REST boundary rather than a silent failure. The block overlay
  configures a non-existent implementation class. This is coverage the unit tests cannot give — it proves the
  TOML key wires through a running server to the endpoint.

  Background:
    Given The system is ready and I have valid publisher access tokens as "publisherUser"

  @cap:publisher @feat:design-assistant @rule:custom-impl @type:regression
  Scenario: A non-instantiable Design Assistant implementation class yields a 500
    When I put the following JSON payload in context as "daSession"
      """
      { "sessionId": "integration-v2-session" }
      """
    And I send an AI Design Assistant generate-payload request with payload "daSession"
    Then The response status code should be 500
