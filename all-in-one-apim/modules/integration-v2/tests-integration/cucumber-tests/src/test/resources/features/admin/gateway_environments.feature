@cleanup
Feature: Admin Gateway Environments

  Ports EnvironmentTestCase: admin-plane CRUD of gateway environments via the admin REST API — create (with a
  vhost, with several vhosts, with a role visibility permission, with a specific gateway type), list, retrieve,
  update, delete, the not-found (404) edge, the read-only built-in Default edges, and the create-validation
  negatives (no vhost, invalid name, empty display name, already-existing). Everything runs ×2 tenant (tenant
  admins manage their own environments). Also covers deploying an API and an API-PRODUCT revision to a custom
  environment's vhost and the environment's deletion only becoming possible once BOTH are undeployed, and the
  gateway instances the built-in Default environment reports. Each scenario uses uniquely-named environments
  (parallel-safe) and cleans them up.

  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: Gateway environment CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment with name "${UNIQUE:envCrud}" display name "Env CRUD" and vhost host "crud.gw.example.com"
    Then The response status code should be 201
    When I retrieve all gateway environments
    Then The response status code should be 200
    And The response should contain "crud.gw.example.com"
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    When I update the gateway environment "environmentId" setting its description to "updated environment description"
    Then The response status code should be 200
    And The response should contain "updated environment description"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EnvironmentTestCase#testUpdateEnvironment: an update round-trips displayName, description AND the vhost
  # list, while the config-provided (read-only) Default environment rejects the update with 400
  # (APIAdminImpl#updateEnvironment -> READONLY_GATEWAY_ENVIRONMENT / 900508).
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment's display name, description and vhosts are updated; the built-in Default rejects an update as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment with name "${UNIQUE:envUpd}" display name "Env To Update" and vhost host "before.gw.example.com"
    Then The response status code should be 201
    When I update the gateway environment "environmentId" setting display name "US Gateway Environment" description "This is a updated test gateway environment" and vhost host "updated.gw.example.com"
    Then The response status code should be 200
    And The value of response field "displayName" should be "US Gateway Environment"
    And The value of response field "description" should be "This is a updated test gateway environment"
    And The value of response field "vhosts[0].host" should be "updated.gw.example.com"
    # The update is persisted, not merely echoed.
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    And The value of response field "displayName" should be "US Gateway Environment"
    And The value of response field "description" should be "This is a updated test gateway environment"
    And The value of response field "vhosts[0].host" should be "updated.gw.example.com"
    And The response should not contain "before.gw.example.com"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200
    # The built-in Default environment is read-only.
    When I attempt to update the built-in Default gateway environment setting its description to "trying to update a read-only environment"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: Create a gateway environment with a specific gateway type as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envApk}" with vhost host "apk.gw.example.com" and gateway type "APK"
    Then The response status code should be 201
    And The value of response field "gatewayType" should be "APK"
    # The create response carries a non-null id (extraction fails the step if the field is absent).
    And I extract response field "id" and store it as "envApkId"
    When I delete the gateway environment with id "envApkId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:negative @legacy:EnvironmentTestCase
  Scenario Outline: Invalid gateway environment creations are rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # No vhost → 400.
    When I create a gateway environment with name "${UNIQUE:envNoVhost}" display name "No Vhost" and vhost host ""
    Then The response status code should be 400
    # Invalid (special-character) name → 400. EnvironmentDTO.name carries @Pattern(^[a-zA-Z0-9_-]+$), so this is
    # real bean-validation enforcement.
    When I create a gateway environment with name "inv@lid env name!" display name "Bad Name" and vhost host "bad.gw.example.com"
    Then The response status code should be 400
    # EMPTY display name → 400 (EnvironmentDTO.displayName is @Size(min=1,max=255)). An ABSENT display name is a
    # different validation path and is accepted — covered by its own scenario below.
    When I create a gateway environment with name "${UNIQUE:envNoDisplay}" display name "" and vhost host "nodisplay.gw.example.com"
    Then The response status code should be 400
    # Already-existing environment (the built-in Default) → 400.
    When I create a gateway environment with name "Default" display name "Default" and vhost host "default.gw.example.com"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EnvironmentTestCase#testAddingGatewayEnvironmentWithoutDisplayName, which asserted 201 for a NULL
  # displayName. That is not the same case as the empty-string negative above: EnvironmentDTO.displayName is
  # annotated @Size(min=1,max=255) with NO @NotNull, and bean validation's @Size does not apply to a null value —
  # so an omitted display name is accepted while an empty one is rejected. Both paths are pinned.
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment can be created without a display name as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envNullDisplay}" with vhost host "nulldisplay.gw.example.com" and no display name
    Then The response status code should be 201
    # A non-null id is returned (extraction fails the step if the field is absent).
    And I extract response field "id" and store it as "envNullDisplayId"
    And The value of response field "vhosts[0].host" should be "nulldisplay.gw.example.com"
    When I delete the gateway environment with id "envNullDisplayId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EnvironmentTestCase#testGatewayPermissions (the create + DTO round-trip half): an environment can be
  # created with a role visibility permission of either ALLOW or DENY — two separately stored configurations —
  # and the permission persists. The legacy devportal ENFORCEMENT half is deliberately NOT ported: its assertions
  # were vacuous (List<APIEndpointURLsDTO>.contains(String) is always false, so both of its checks passed
  # regardless of the product's behaviour) and the whole test was commented out of the legacy suite.
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment can be created with a <permissionType> role permission as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envPerm}" with vhost host "perm.gw.example.com" with permission type "<permissionType>" for role "Internal/creator"
    Then The response status code should be 201
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    And The value of response field "permissions.permissionType" should be "<permissionType>"
    And The response field "permissions.roles" should be exactly the list "Internal/creator"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             | permissionType |
      | admin             | ALLOW          |
      | admin             | DENY           |
      | admin@tenant1.com | ALLOW          |
      | admin@tenant1.com | DENY           |

  # Ports EnvironmentTestCase#testGetGatewayInstancesInDefaultEnvironment: the built-in Default environment reports
  # its registered gateway instances — count > 0, count equal to the returned list's size, and every instance with
  # a gatewayId and a status of exactly ACTIVE or EXPIRED (the only two values
  # GatewayManagementUtils#validateGatewayStatus can produce).
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: The Default gateway environment reports its gateway instances as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the gateway instances of environment "Default"
    Then The response status code should be 200
    And The gateway instances response should report every instance with a gateway id and an ACTIVE or EXPIRED status

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E2: duplicate-hostname vhost negative — NEW verified coverage (legacy-disabled). Two vhosts with the same
  # hostname within one environment are rejected (400).
  @cap:admin @feat:environments @type:negative @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment with duplicate vhost hostnames is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envDupVhost}" with vhost hosts "dup.gw.example.com,dup.gw.example.com"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EnvironmentTestCase#testAddingGatewayEnvironmentWithVhostsHavingSpecialCharacters, pinning the ACTUAL
  # behaviour: a vhost hostname containing special characters is ACCEPTED (201) and stored verbatim. Legacy
  # expected 400, but that expectation was both wrong for 4.7.0 and vacuous (its try/catch had no Assert.fail, so
  # it passed whatever happened) — and the class entry was commented out of the legacy suite, so it never ran.
  # VHostDTO.host carries only @NotNull/@Size(1,255) and NO @Pattern, i.e. there is no hostname-format validation
  # at all, unlike EnvironmentDTO.name. So 400 must never be asserted here; the round-trip is what is pinned.
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A vhost hostname with special characters is accepted and stored verbatim as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envSpecialVhost}" with vhost hosts "foods.com#$%?"
    Then The response status code should be 201
    And The value of response field "vhosts[0].host" should be "foods.com#$%?"
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    And The value of response field "vhosts[0].host" should be "foods.com#$%?"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E1: an environment can be created with MULTIPLE vhosts, and updated to remove one — NEW verified coverage
  # (legacy-disabled testAddGatewayEnvironmentMultipleVHosts / testUpdateEnvironmentByRemovingVHost). ×2 tenant.
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment with multiple vhosts can be created and have a vhost removed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envMultiVhost}" with vhost hosts "one.gw.example.com,two.gw.example.com"
    Then The response status code should be 201
    And The response should contain "one.gw.example.com"
    And The response should contain "two.gw.example.com"
    When I update the gateway environment "environmentId" to only vhost host "one.gw.example.com"
    Then The response status code should be 200
    And The response should not contain "two.gw.example.com"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EnvironmentTestCase#testDeployApiRevisionWithVhost + #testDeleteEnvironmentAfterUndeployingRevisions:
  # BOTH an API revision and an API-PRODUCT revision deploy to a custom environment's vhost, the environment
  # cannot be deleted while EITHER is still deployed on it (409), and it deletes (200) only once both have been
  # undeployed. The environment is addressed by NAME in the deploy payload but by ID (a UUID) for delete.
  @cap:admin @feat:environments @type:regression @dep:publisher @legacy:EnvironmentTestCase
  Scenario Outline: API and API-product revisions deploy to a custom environment's vhost; the environment deletes only after both are undeployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "e3ApiPayload"
    And I create an "apis" resource with payload "e3ApiPayload" as "e3ApiId"
    Then The response status code should be 201
    When I create a gateway environment "${UNIQUE:e3Env}" with vhost hosts "e3.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "e3EnvName"
    When I put the following JSON payload in context as "e3RevPayload"
    """
    {"description":"revision for vhost deploy"}
    """
    And I make a request to create a revision for "apis" resource "e3ApiId" with payload "e3RevPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "e3ApiRevId"
    # Deploy the API revision to the custom environment's vhost.
    When I put the following JSON payload in context as "e3DeployPayload"
    """
    [{"name":"{{e3EnvName}}","vhost":"e3.gw.example.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "e3ApiRevId" of "apis" resource "e3ApiId" with payload "e3DeployPayload"
    Then The response status code should be 201
    # Deploy an API-PRODUCT revision to the same custom environment vhost.
    When I create an API product "${UNIQUE:e3Product}" with context "${UNIQUE:e3ProductCtx}" from API "e3ApiId" as "e3ProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "e3ProdRevPayload"
    """
    {"description":"product revision for vhost deploy"}
    """
    And I make a request to create a revision for "api-products" resource "e3ProductId" with payload "e3ProdRevPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "e3ProdRevId"
    When I make a request to deploy revision "e3ProdRevId" of "api-products" resource "e3ProductId" with payload "e3DeployPayload"
    Then The response status code should be 201
    # The environment cannot be deleted while the API revision is deployed on it.
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 409
    When I undeploy revision "e3ApiRevId" of "apis" resource "e3ApiId" with payload "e3DeployPayload"
    Then The response status code should be 201
    # Nor while only the API-product revision remains deployed on it.
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 409
    When I undeploy revision "e3ProdRevId" of "api-products" resource "e3ProductId" with payload "e3DeployPayload"
    Then The response status code should be 201
    # With both undeployed, the environment can be deleted.
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E4 (edges): deleting the built-in Default environment, or a non-existent one, is rejected.
  @cap:admin @feat:environments @type:negative @legacy:EnvironmentTestCase
  Scenario Outline: Deleting the built-in Default environment or a non-existent environment is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I delete the gateway environment with id "Default"
    Then The response status code should be 400
    When I delete the gateway environment with id "nonexistent-environment-00000"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
