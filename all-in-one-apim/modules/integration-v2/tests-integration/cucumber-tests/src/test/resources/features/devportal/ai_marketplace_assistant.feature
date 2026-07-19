Feature: AI Marketplace Assistant REST contract
  Validates the DevPortal Marketplace Assistant endpoints after the pluggable-implementation refactor
  (carbon-apimgt PR #13920). With no AI credentials configured (the default distribution state), the request
  validation now runs before the backend is consulted, and the default implementation degrades gracefully to an
  empty response instead of erroring.

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

  # NOTE: with the AI service unconfigured the REST resource returns a null Response; JAX-RS renders that as
  # 204 No Content. Confirm this exact code on the first live run and adjust if an interceptor maps it otherwise.
  @cap:devportal @feat:marketplace-assistant @type:regression
  Scenario: Marketplace Assistant chat returns no content when the AI service is not configured
    When I put the following JSON payload in context as "mpValidQuery"
      """
      { "query": "List the available APIs", "history": [] }
      """
    And I send an AI Marketplace Assistant chat request with payload "mpValidQuery"
    Then The response status code should be 204

  @cap:devportal @feat:marketplace-assistant @type:regression
  Scenario: Marketplace Assistant API count returns no content when the AI service is not configured
    When I request the AI Marketplace Assistant API count
    Then The response status code should be 204
