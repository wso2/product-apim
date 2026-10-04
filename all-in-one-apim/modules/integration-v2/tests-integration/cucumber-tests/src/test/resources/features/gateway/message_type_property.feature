Feature: Gateway message-type property invocation

  The legacy APIInvocationWithMessageTypeProperty installed a Synapse in-sequence with the axis2 messageType
  property set to application/json, invoked it with GET, and expected the backend's 202 Accepted response. This
  API-owned sequence keeps the same GET/messageType/Accepted contract without installing global Synapse state.
  Its backend fixture returns 202 only when it observes application/json. Each case runs as an admin in both
  carbon.super and tenant1.com; the API and application are scenario-owned and cleaned up afterward.

  @cleanup @cap:gateway @feat:mediation-policies @rule:message-type @type:regression @dep:publisher @legacy:APIInvocationWithMessageTypeProperty
  Scenario Outline: An API sequence sets messageType to JSON for a GET and receives Accepted as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_sequence_backend_api.json" in context as "mtApiPayload"
    And I create an "apis" resource with payload "mtApiPayload" as "mtApiId"
    Then The response status code should be 201

    When I upload the sequence backend "artifacts/sequenceBackend/sequence_message_type_json.xml" of type "PRODUCTION" for API "mtApiId"
    Then The response status code should be 200
    When I upload the sequence backend "artifacts/sequenceBackend/sequence_message_type_json.xml" of type "SANDBOX" for API "mtApiId"
    Then The response status code should be 200

    When I put the following JSON payload in context as "mtRevisionPayload"
    """
    {"description":"messageType JSON revision"}
    """
    And I make a request to create a revision for "apis" resource "mtApiId" with payload "mtRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "mtRevisionId"
    When I deploy revision "mtRevisionId" of "apis" resource "mtApiId"
    Then The response status code should be 201
    And the "apis" resource "mtApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "mtApiId"
    Then The lifecycle status of API "mtApiId" should be "Published"

    When I retrieve the "apis" resource with id "mtApiId"
    And I extract response field "context" and store it as "mtApiContext"
    # Legacy invokes this unsecured global Synapse API anonymously. Keep that exact credential path; the
    # response/body assertions also prove the sequence set messageType to application/json before the backend.
    When I invoke the API at gateway context "{{mtApiContext}}/1.0.0/" with method "GET" without authentication until response status code becomes 202 within 60 seconds
    Then The response status code should be 202
    And The response reason phrase should be "Accepted"
    And The response should contain "Accepted"
    When I have set up application with keys, subscribed to API "mtApiId", and obtained access token for "mtSubscriptionId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mtApiContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 202 within 60 seconds
    Then The response status code should be 202
    And The response reason phrase should be "Accepted"
    And The response should contain "Accepted"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
