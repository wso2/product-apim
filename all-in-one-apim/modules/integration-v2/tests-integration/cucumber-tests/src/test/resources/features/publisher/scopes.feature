@cleanup
Feature: Publisher API Shared Scopes

  Publisher-plane shared-scope management: creating a shared scope and assigning it to an API resource.
  Shared-scope management requires admin privileges (the apim:shared_scope_manage scope is granted only to
  admins, not to a creator+publisher user), so the positive flows run as the tenant admin in both the super
  tenant and tenant1.com. Each scenario creates its own resources and is torn down by the per-scenario
  cleanup hook (created scopes and APIs are both registered for teardown).

  @cap:publisher @feat:scopes @type:smoke @legacy:SharedScopeTestCase
  Scenario Outline: Create and retrieve a shared scope as <admin>
    Given The system is ready and I have valid publisher access tokens as "<admin>"
    When I create a new shared scope as "scope-create-test"
    Then The response status code should be 201
    And The response should contain "scope-create-test"
    When I fetch the shared scope with name "scope-create-test" into context as "fetchedScopeId"
    Then The response status code should be 200
    And The response should contain "scope-create-test"

    Examples:
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:scopes @type:regression @legacy:SharedScopeTestCase
  Scenario Outline: Assign a shared scope to an API as <admin>
    Given The system is ready and I have valid publisher access tokens as "<admin>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "scopeApiId" and deployed it
    When I create a new shared scope as "scope-assign-test"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "scopeApiId"
    And I put the response payload in context as "scopeApiPayload"
    When I update the "apis" resource "scopeApiId" and "scopeApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"scope-assign-test","displayName":"scope-assign-test","description":"This Scope is to test the creation of new scope","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopeApiId"
    Then The response should contain "scope-assign-test"

    Examples:
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:scopes @type:negative @legacy:SharedScopeTestCase
  Scenario Outline: A subscriber-role user cannot create a shared scope as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to create a shared scope as "scope-negative-test"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
