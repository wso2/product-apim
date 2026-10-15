@cleanup
Feature: Custom subscription workflow HTTP redirect

  Exercise the legacy custom SubscriptionCreation executor contract in an isolated runtime. The executor JAR is
  copied into the APIM component library before boot, its workflow-extensions.xml selection is restored after the
  runner, and the scenario checks the redirect URL serialized in the subscription workflow response.

  @cap:admin @feat:workflows @type:regression @dep:devportal @dep:publisher @legacy:SubscriptionWFHTTPRedirectTest
  Scenario: Custom subscription creation workflow returns its configured HTTP redirect URL
    Given The system is ready
    And I have valid access tokens as "admin"
    When I enable custom HTTP-redirect subscription workflow executor from "artifacts/configFiles/workflowHTTPRedirect/workflow-extensions.xml"

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "redirectApiId" and deployed it
    When I publish the "apis" resource with id "redirectApiId"
    Then The lifecycle status of API "redirectApiId" should be "Published"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "redirectAppPayload"
    And I create an application with payload "redirectAppPayload"
    Then The response status code should be 201

    When I put the following JSON payload in context as "redirectSubscriptionPayload"
    """
    {"applicationId":"{{createdAppId}}","apiId":"{{redirectApiId}}","throttlingPolicy":"Unlimited"}
    """
    And I submit a subscription for API "redirectApiId" and application "createdAppId" with payload "redirectSubscriptionPayload"
    Then The response status code should be 201
    Then The subscription workflow response should contain redirect URL "http://google.lk"
