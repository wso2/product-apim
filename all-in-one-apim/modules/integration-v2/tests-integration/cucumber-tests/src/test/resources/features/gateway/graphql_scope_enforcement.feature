@cleanup
Feature: Gateway GraphQL Operation-Level Security

  Ports the operation-level security checks from the legacy GraphqlServerRestartTestCase (functional concerns;
  the legacy "restart" was incidental): (1) a shared scope attached to the `languages` QUERY operation is
  enforced — a token WITH it queries (200), one WITHOUT it is refused (403); (2) an operation with
  `authType=None` is invocable WITHOUT any token (200). Run in BOTH the super tenant and tenant1.com to prove
  operation-level security is tenant-agnostic (the tenant API is addressed by its full /t/<tenant> context, and
  the shared scope is bound to the per-tenant admin role). Runs in the concurrent IntegrationV2-Gateway block
  (backend started). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:graphql-invocation @type:regression @dep:publisher @legacy:GraphqlServerRestartTestCase
  Scenario Outline: A scope-gated GraphQL operation is enforced (200 with the scope, 403 without) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a new shared scope as "gqlScopeEnf"
    Then The response status code should be 201

    # Create the GraphQL API, register the scope on it, and gate the `languages` operation with it.
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "graphQLAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "graphQLAPIPayload" as "gqlApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gqlApiId"
    And I put the response payload in context as "gqlPayload"
    And I extract response field "context" and store it as "gqlApiContext"
    When I update the "apis" resource "gqlApiId" and "gqlPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"gqlScopeEnf","displayName":"gqlScopeEnf","description":"graphql scope enforcement","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "gqlApiId"
    And I put the response payload in context as "gqlPayload"
    When I update the "apis" resource "gqlApiId" and "gqlPayload" with configuration type "operations" and value:
      """
      [{"target":"languages","verb":"QUERY","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["gqlScopeEnf"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"language","verb":"QUERY","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200

    When I put the following JSON payload in context as "gqlRevPayload"
    """
    {"description":"scope revision"}
    """
    And I make a request to create a revision for "apis" resource "gqlApiId" with payload "gqlRevPayload"
    When I put the following JSON payload in context as "gqlDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "gqlApiId" with payload "gqlDeployPayload"
    When I publish the "apis" resource with id "gqlApiId"
    Then The lifecycle status of API "gqlApiId" should be "Published"

    # Subscribe an application and key it.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "gqlApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "gqlSubId"
    Then The response status code should be 201

    # A token WITH the scope can query the gated operation (200); one WITHOUT it is refused (403).
    When I request an OAuth access token for the current user using password grant with scope "gqlScopeEnf"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{gqlApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{gqlApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlQuery" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:graphql-invocation @type:regression @dep:publisher @legacy:GraphqlServerRestartTestCase
  Scenario Outline: A GraphQL operation with authType None is invocable without a token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlNoneAPIPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlNoneAPIPayload" as "gqlNoneApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gqlNoneApiId"
    And I put the response payload in context as "gqlNonePayload"
    And I extract response field "context" and store it as "gqlNoneApiContext"

    # Set the `languages` operation to authType None (no security).
    When I update the "apis" resource "gqlNoneApiId" and "gqlNonePayload" with configuration type "operations" and value:
      """
      [{"target":"languages","verb":"QUERY","authType":"None","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"language","verb":"QUERY","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlNoneRevPayload"
    """
    {"description":"none-auth revision"}
    """
    And I make a request to create a revision for "apis" resource "gqlNoneApiId" with payload "gqlNoneRevPayload"
    When I put the following JSON payload in context as "gqlNoneDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "gqlNoneApiId" with payload "gqlNoneDeployPayload"
    When I publish the "apis" resource with id "gqlNoneApiId"
    Then The lifecycle status of API "gqlNoneApiId" should be "Published"

    # No token — the None-auth operation must still be invocable (200).
    When I put the following JSON payload in context as "gqlNoneEmptyToken"
    """
    """
    And I put the following JSON payload in context as "gqlNoneQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{gqlNoneApiContext}}/1.0.0" with method "POST" using access token "gqlNoneEmptyToken" and payload "gqlNoneQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
