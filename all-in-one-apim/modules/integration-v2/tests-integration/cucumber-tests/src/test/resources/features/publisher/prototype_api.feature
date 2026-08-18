@cleanup
Feature: Prototype API runtime, mock and visibility

  Ports PrototypedAPITestcase (+ APIM23/APIM24 devportal visibility): an API deployed as a prototype
  (lifecycle action "Deploy as a Prototype", endpoint config implementation_status=prototyped) is invocable
  at the gateway with a subscription token, is demoted back to CREATED (after which invocation without auth is
  rejected), supports inline OAS mock-implementation generation — whose generated script is then deployed and
  ANSWERS at the gateway with no real backend — and appears under the devportal's prototyped
  API listing. Runs ×2 tenant (super + tenant1.com) though the legacy Factory was super-only. The block starts
  the node backend (the prototyped endpoint routes to it). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @rule:prototype @type:regression @dep:publisher @legacy:PrototypedAPITestcase
  Scenario Outline: A deployed prototyped API is invocable with a subscription token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_prototype_api.json" in context as "protoPayload"
    And I create an "apis" resource with payload "protoPayload" as "protoApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "protoApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    And The lifecycle status of API "protoApiId" should be "Prototyped"
    When I deploy the API with id "protoApiId"
    When I retrieve the "apis" resource with id "protoApiId"
    And I extract response field "context" and store it as "apiContext"
    When I have set up application with keys, subscribed to API "protoApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    # The prototyped API's endpoint config points at node-customer-service, and the backend payload is what shows
    # the prototype route resolved to it — a bare 200 asserts only that something answered.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:security-enforcement @rule:prototype @type:negative @dep:publisher @legacy:PrototypedAPITestcase
  Scenario Outline: A prototyped API demoted back to CREATED rejects unauthenticated invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_prototype_api.json" in context as "protoPayload"
    And I create an "apis" resource with payload "protoPayload" as "protoApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "protoApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    When I deploy the API with id "protoApiId"
    When I retrieve the "apis" resource with id "protoApiId"
    And I extract response field "context" and store it as "apiContext"
    When I change the lifecycle of API "protoApiId" with action "Demote to Created"
    Then The response status code should be 200
    And The lifecycle status of API "protoApiId" should be "Created"
    # Demote-to-Created undeploys the API from the gateway, so the route is removed — an invocation returns
    # 404 (not routable), not 401. Poll until the undeploy propagates.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 404 within 60 seconds
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The generated script must cover EVERY resource of the imported definition, not just one: the petstore-shaped
  # prototype definitions carry three (/pets, /pets/{petId}, /oldpets), and a generator that emitted a stub for only
  # the first would satisfy a single-path assertion. Ports the mock-script half of PrototypedAPITestcase's
  # testOAS2InlinePrototypeWithMock / testOAS3InlinePrototypeWithMock.
  @cap:publisher @feat:api-lifecycle @rule:prototype @type:regression @legacy:PrototypedAPITestcase
  Scenario Outline: An inline mock implementation script is generated for every resource of a prototyped <oasVersion> API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "<apiDefinition>" , additional properties from "<additionalProperty>" and create api as "mockApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "mockApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    And The lifecycle status of API "mockApiId" should be "Prototyped"
    When I generate the mock implementation script for API "mockApiId"
    Then The response status code should be 200
    When I retrieve the mock implementation script for API "mockApiId"
    Then The response status code should be 200
    And The response should contain "/pets"
    And The response should contain "/pets/{petId}"
    And The response should contain "/oldpets"

    Examples:
      | oasVersion | actor                     | apiDefinition                                              | additionalProperty                                                |
      | OAS2       | publisherUser             | artifacts/payloads/OAS/OAS2PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS2PrototypeMockAdditionalProperties.json |
      | OAS3       | publisherUser             | artifacts/payloads/OAS/OAS3PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS3PrototypeMockAdditionalProperties.json |
      | OAS2       | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS2PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS2PrototypeMockAdditionalProperties.json |
      | OAS3       | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS3PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS3PrototypeMockAdditionalProperties.json |

  # The whole point of an inline mock is that it RESPONDS without a real backend, so the generated script is
  # deployed and invoked here: a 200 from /pets/1 can only come from the inline mock (the API's endpointConfig is
  # never consulted for an INLINE implementation). Ports the deploy+invoke half of PrototypedAPITestcase's
  # testOAS2InlinePrototypeWithMock / testOAS3InlinePrototypeWithMock, which the publisher-plane scenario above
  # deliberately leaves out.
  @cap:gateway @feat:rest-invocation @rule:prototype-mock @type:regression @dep:publisher @legacy:PrototypedAPITestcase
  Scenario Outline: A deployed inline mock of a prototyped <oasVersion> API answers at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import open api definition from "<apiDefinition>" , additional properties from "<additionalProperty>" and create api as "mockApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "mockApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    And The lifecycle status of API "mockApiId" should be "Prototyped"
    When I generate the mock implementation script for API "mockApiId"
    Then The response status code should be 200
    When I deploy the API with id "mockApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "mockApiId"
    And I extract response field "context" and store it as "mockApiContext"
    When I have set up application with keys, subscribed to API "mockApiId" with plan "Unlimited", and obtained access token for "mockSubscriptionId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mockApiContext}}/1.0.0/pets/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | oasVersion | actor             | apiDefinition                                              | additionalProperty                                                |
      | OAS2       | admin             | artifacts/payloads/OAS/OAS2PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS2PrototypeMockAdditionalProperties.json |
      | OAS3       | admin             | artifacts/payloads/OAS/OAS3PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS3PrototypeMockAdditionalProperties.json |
      | OAS2       | admin@tenant1.com | artifacts/payloads/OAS/OAS2PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS2PrototypeMockAdditionalProperties.json |
      | OAS3       | admin@tenant1.com | artifacts/payloads/OAS/OAS3PrototypeMockDefinition.json    | artifacts/payloads/OAS/OAS3PrototypeMockAdditionalProperties.json |

  @cap:devportal @feat:discovery @rule:prototype @type:regression @dep:publisher @legacy:APIM23VisibilityOfPrototypedAPIInStoreTestCase
  Scenario Outline: A prototyped API is visible in the devportal as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_prototype_api.json" in context as "protoPayload"
    And I create an "apis" resource with payload "protoPayload" as "protoApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "protoApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    When I deploy the API with id "protoApiId"
    When I retrieve the devportal API "protoApiId" until it contains "PROTOTYPED" within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
