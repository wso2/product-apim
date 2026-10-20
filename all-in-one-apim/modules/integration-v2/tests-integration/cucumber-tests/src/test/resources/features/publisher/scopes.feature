@cleanup
Feature: Publisher API Shared Scopes

  Publisher-plane shared-scope management: creating a shared scope, assigning it to an API resource,
  attaching/detaching it on a specific operation (the APIScopeTestCase arc), and deleting the shared scope.
  Shared-scope management requires admin privileges (the apim:shared_scope_manage scope is granted only to
  admins, not to a creator+publisher user), so the positive flows run as the tenant admin in both the super
  tenant and tenant1.com. Each scenario creates its own resources and is torn down by the per-scenario
  cleanup hook (created scopes and APIs are both registered for teardown).

  @cap:publisher @feat:scopes @type:smoke @legacy:SharedScopeTestCase @legacy:SharedScopeTestWithRestart
  Scenario Outline: Create and retrieve a shared scope as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    # The name is a BASE — the step uniquifies it (a shared scope is tenant-wide, so a literal would 409 a
    # re-run whose teardown could not complete). Capture the created name and assert against that.
    When I create a new shared scope as "scope-create-test"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createScopeName"
    And The response should contain "{{createScopeName}}"
    When I fetch the shared scope with name "{{createScopeName}}" into context as "fetchedScopeId"
    Then The response status code should be 200
    And The response should contain "{{createScopeName}}"
    # Retrieve THAT scope by id and assert its stored fields, including the role bindings the create payload
    # requested. Ports SharedScopeTestWithRestart.testGetAndUpdateSharedScope's name/displayName/bindings
    # assertions; the by-name list read above could otherwise be satisfied by another scope's bindings.
    When I retrieve the shared scope with id "fetchedScopeId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{createScopeName}}"
    And The value of response field "displayName" should be "{{createScopeName}}"
    And The response field "bindings" should be exactly the list "admin"

  Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Keep the existing shared-scope create/retrieve scenario unchanged; this focused case verifies that the server
  # preserves a display name distinct from the scope name on both the create response and a fresh GET.
  @cap:publisher @feat:scopes @type:regression @legacy:SharedScopeTestCase
  Scenario Outline: A shared scope preserves a distinct display name as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new shared scope as "scope-distinct-display" with display name "scope-display-label"
    Then The response status code should be 201
    And The value of response field "name" should be "{{distinctScopeName}}"
    And The value of response field "displayName" should be "{{distinctScopeDisplayName}}"
    When I retrieve the shared scope with id "scopeID"
    Then The response status code should be 200
    And The value of response field "name" should be "{{distinctScopeName}}"
    And The value of response field "displayName" should be "{{distinctScopeDisplayName}}"
    And The response field "bindings" should be exactly the list "admin"
    When I delete shared scope with "scopeID"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:scopes @type:regression @legacy:SharedScopeTestCase @legacy:APIScopeTestCase
  Scenario Outline: Assign a shared scope to an API and to an operation as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "scopeApiId" and deployed it
    When I create a new shared scope as "scope-assign-test"
    Then The response status code should be 201
    And I extract response field "name" and store it as "assignScopeName"
    When I retrieve the "apis" resource with id "scopeApiId"
    And I put the response payload in context as "scopeApiPayload"

    # Register the shared scope on the API (adds it to the API's scope list).
    When I update the "apis" resource "scopeApiId" and "scopeApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"{{assignScopeName}}","displayName":"{{assignScopeName}}","description":"This Scope is to test the creation of new scope","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopeApiId"
    Then The response should contain "{{assignScopeName}}"
    And I put the response payload in context as "scopeApiPayload"

    # Attach the scope to a specific operation (the APIScopeTestCase behaviour): the operation now requires
    # the scope. Asserted by re-fetch containing the scope on the operation.
    When I update the "apis" resource "scopeApiId" and "scopeApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{assignScopeName}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopeApiId"
    # Asserted on the OPERATION, not on the payload as a whole: the scope is already in the API-level scopes array
    # from the step above, so a bare "response contains" would pass even if the operation carried no scope at all.
    Then The response field "operations[?(@.target=='/customers/{id}' && @.verb=='GET')].scopes[*]" should be exactly the list "{{assignScopeName}}"
    And The response array field "operations[?(@.target=='/customers/{id}' && @.verb=='GET')].scopes[*]" should have exactly 1 entries
    And I put the response payload in context as "scopeApiPayload"

    # Detach the scope from the operation again (scopes array cleared).
    When I update the "apis" resource "scopeApiId" and "scopeApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    # Re-read: a 200 on the PUT only says the request was accepted, not that the scope is gone. The verb pin comes
    # first so the empty-scope assertion cannot pass vacuously by the operation having disappeared entirely.
    When I retrieve the "apis" resource with id "scopeApiId"
    Then The response status code should be 200
    And The response field "operations[?(@.target=='/customers/{id}' && @.verb=='GET')].verb" should be exactly the list "GET"
    And The response field "operations[?(@.target=='/customers/{id}' && @.verb=='GET')].scopes[*]" should be exactly the list ""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # APIScopeTestCase creates an API with API-local scopes, copies the published API, then updates the scoped source
  # and deploys a revision. Shared-scope assignment above is not equivalent: this pins preservation of the API-local
  # role bindings and operation references across copy, metadata update, and revision deployment.
  @cap:publisher @feat:scopes @rule:api-local-scope-copy-preservation @type:regression @dep:gateway @legacy:APIScopeTestCase
  Scenario Outline: API-local role scopes survive API copy and source update as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_local_scope_role_matrix_api.json" as "scopeCopySourceId" and deployed it
    And the "apis" resource "scopeCopySourceId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "scopeCopySourceId"
    Then The lifecycle status of API "scopeCopySourceId" should be "Published"

    When I create a new version "2.0.0" of "apis" resource "scopeCopySourceId" with default version "false" as "scopeCopyId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "scopeCopyId"
    Then The response status code should be 200
    And The response array field "scopes" should have exactly 2 entries
    And The response field "scopes[?(@.scope.name=='local_get_scope')].scope.bindings[*]" should be exactly the list "Internal/subscriber"
    And The response field "scopes[?(@.scope.name=='local_post_scope')].scope.bindings[*]" should be exactly the list "admin"
    And The response field "operations[?(@.verb=='GET')].scopes[*]" should be exactly the list "local_get_scope"
    And The response field "operations[?(@.verb=='POST')].scopes[*]" should be exactly the list "local_post_scope"

    When I retrieve the "apis" resource with id "scopeCopySourceId"
    And I put the response payload in context as "scopeCopySourcePayload"
    When I update the "apis" resource "scopeCopySourceId" and "scopeCopySourcePayload" with configuration type "description" and value:
      """
      Updated source API with local scopes
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopeCopySourceId"
    And I put the response payload in context as "scopeCopyPolicyPayload"
    When I update the "apis" resource "scopeCopySourceId" and "scopeCopyPolicyPayload" with configuration type "policies" and value:
      """
      ["Gold"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopeCopySourceId"
    And I put the response payload in context as "scopeCopyEndpointPayload"
    When I put the following JSON payload in context as "scopeCopyEndpointConfig"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/updated/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/updated/"}}
    """
    And I update the "apis" resource "scopeCopySourceId" and "scopeCopyEndpointPayload" with configuration type "endpointConfig" and value:
      """
      scopeCopyEndpointConfig
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "scopeCopyRevisionPayload"
    """
    {"description":"API-local scope update revision"}
    """
    And I make a request to create a revision for "apis" resource "scopeCopySourceId" with payload "scopeCopyRevisionPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "scopeCopyDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "scopeCopySourceId" with payload "scopeCopyDeployPayload"
    Then The response status code should be 201
    And the "apis" resource "scopeCopySourceId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "scopeCopySourceId"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated source API with local scopes"
    And The response field "policies[*]" should be exactly the list "Gold"
    And The response should contain "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/updated/"
    And The response array field "scopes" should have exactly 2 entries
    And The response field "scopes[?(@.scope.name=='local_get_scope')].scope.bindings[*]" should be exactly the list "Internal/subscriber"
    And The response field "scopes[?(@.scope.name=='local_post_scope')].scope.bindings[*]" should be exactly the list "admin"
    And The response field "operations[?(@.verb=='GET')].scopes[*]" should be exactly the list "local_get_scope"
    And The response field "operations[?(@.verb=='POST')].scopes[*]" should be exactly the list "local_post_scope"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:scopes @type:regression @legacy:SharedScopeTestCase
  Scenario Outline: Create and delete a shared scope as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new shared scope as "scope-delete-test"
    Then The response status code should be 201
    When I delete shared scope with "scopeID"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # THREE consecutive description updates, each asserted — legacy SharedScopeTestWithRestart deliberately
  # exercised repeated updates of the same scope (a second or third PUT on an already-updated scope is a distinct
  # path from the first, e.g. if an update were implemented as insert-if-absent). A single update cannot show that.
  # The final re-read confirms the last write is what is stored, not merely what the PUT echoed back.
  @cap:publisher @feat:scopes @type:regression @legacy:SharedScopeTestWithRestart
  Scenario Outline: A shared scope survives three consecutive description updates as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create a new shared scope as "scope-update-test"
    Then The response status code should be 201
    And I extract response field "name" and store it as "updateScopeName"
    When I update the shared scope "scopeID" setting its description to "Updated shared scope description"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated shared scope description"
    When I update the shared scope "scopeID" setting its description to "Updated shared scope description 1"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated shared scope description 1"
    When I update the shared scope "scopeID" setting its description to "Updated shared scope description 2"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated shared scope description 2"
    When I retrieve the shared scope with id "scopeID"
    Then The response status code should be 200
    And The value of response field "description" should be "Updated shared scope description 2"
    And The value of response field "name" should be "{{updateScopeName}}"
    And The response field "bindings" should be exactly the list "admin"

    Examples:
      | actor             |
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

  # I3: validate a system role via HEAD /roles/{base64url(role)} — an existing role → 200, a non-existing one →
  # 404. Ports APIM638ValidateTheRoleOfAnExistingUser / ...NonExistingUser. (The earlier 401 was a v2 glue bug:
  # a literal '=' base64 pad in the path segment; fixed by URL-safe base64 WITHOUT padding — see
  # Utils.getValidateRoleURL. Legacy runs this green with a publisher token, so we do too.)
  @cap:publisher @feat:scopes @rule:role-validation @type:regression @legacy:APIM638ValidateTheRoleOfAnExistingUserThroughThePublisherRestAPITestCase
  Scenario Outline: Validating a system role returns 200 for an existing role and 404 for a non-existing one as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the role "admin"
    Then The response status code should be 200
    When I validate the role "Internal/publisher"
    Then The response status code should be 200
    When I validate the role "no-such-role-xyz-000"
    Then The response status code should be 404

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
